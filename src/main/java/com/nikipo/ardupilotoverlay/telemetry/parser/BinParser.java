package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.telemetry.dataType.Home;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TelemetryCapability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses ArduPilot dataflash (.bin) logs into a {@link Telemetry} object.
 *
 * Ported from the web ArduPilotOverlay tool's JS DataflashParser, but
 * restructured as a single sequential pass: as each record is decoded it is
 * fed straight into the matching Telemetry time series, rather than being
 * indexed for later random-access retrieval.
 *
 * FIELD LOOKUP: fields are read out of each record BY NAME, using the
 * FMT/Columns definition actually present in the log, not by assumed fixed
 * column position. This makes the parser resilient to ArduPilot reordering
 * or adding columns to a message across firmware versions. It is NOT
 * resilient to a field being renamed outright - if a channel comes back
 * empty, compare the field names referenced below (e.g. "Roll", "RemPct")
 * against the FMT record for that message type in your actual log.
 *
 * A few mappings below are the ones most likely to need adjusting for a
 * given firmware version; each is flagged where it's used:
 *   - which message carries RSSI (RAD vs RSSI vs none at all)
 *   - the EV (event) ids used to detect armed/disarmed
 *   - what ORGN.Type means (we just take the first ORGN record as home)
 *
 * Two TelemetryCapability values currently have no corresponding Telemetry
 * field (BATTERY_MAH_USED, RC_CHANNELS) and are intentionally left unused
 * here rather than guessing where they should live on Telemetry.
 */
public class BinParser implements TelemetryParser {

    private static final int HEAD1 = 0xA3;
    private static final int HEAD2 = 0x95;
    private static final int FMT_MSG_TYPE = 128;

    // ArduPilot LogEvent ids (see Log.h -> enum LogEvent) used to derive the
    // armed state from EV records. Stable across recent firmware, but worth
    // double-checking against the version that produced your logs.

    private static final int EVENT_ARMED = 10;
    private static final int EVENT_DISARMED = 11;

    private static final Map<Integer, String> MODE_MAP_PLANE = Map.ofEntries(
            Map.entry(0, "MANUAL"), Map.entry(1, "CIRCLE"), Map.entry(2, "STABILIZE"),
            Map.entry(3, "TRAINING"), Map.entry(4, "ACRO"), Map.entry(5, "FBWA"),
            Map.entry(6, "FBWB"), Map.entry(7, "CRUISE"), Map.entry(8, "AUTOTUNE"),
            Map.entry(10, "AUTO"), Map.entry(11, "RTL"), Map.entry(12, "LOITER"),
            Map.entry(13, "TAKEOFF"), Map.entry(14, "AVOID_ADSB"), Map.entry(15, "GUIDED"),
            Map.entry(16, "INITIALISING"), Map.entry(17, "QSTABILIZE"), Map.entry(18, "QHOVER"),
            Map.entry(19, "QLOITER"), Map.entry(20, "QLAND"), Map.entry(21, "QRTL"),
            Map.entry(22, "QAUTOTUNE"), Map.entry(23, "QACRO"), Map.entry(24, "THERMAL")
    );

    private static final Map<Integer, String> MODE_MAP_COPTER = Map.ofEntries(
            Map.entry(0, "STABILIZE"), Map.entry(1, "ACRO"), Map.entry(2, "ALT_HOLD"),
            Map.entry(3, "AUTO"), Map.entry(4, "GUIDED"), Map.entry(5, "LOITER"),
            Map.entry(6, "RTL"), Map.entry(7, "CIRCLE"), Map.entry(9, "LAND"),
            Map.entry(11, "DRIFT"), Map.entry(13, "SPORT"), Map.entry(14, "FLIP"),
            Map.entry(15, "AUTOTUNE"), Map.entry(16, "POSHOLD"), Map.entry(17, "BRAKE"),
            Map.entry(18, "THROW"), Map.entry(19, "AVOID_ADSB"), Map.entry(20, "GUIDED_NOGPS"),
            Map.entry(21, "SMART_RTL"), Map.entry(22, "FLOWHOLD"), Map.entry(23, "FOLLOW"),
            Map.entry(24, "ZIGZAG"), Map.entry(25, "SYSTEMID"), Map.entry(26, "AUTOROTATE")
    );

    private static final Map<Integer, String> MODE_MAP_ROVER = Map.ofEntries(
            Map.entry(0, "MANUAL"), Map.entry(1, "ACRO"), Map.entry(3, "STEERING"),
            Map.entry(4, "HOLD"), Map.entry(5, "LOITER"), Map.entry(6, "FOLLOW"),
            Map.entry(7, "SIMPLE"), Map.entry(8, "DOCK"), Map.entry(9, "CIRCLE"),
            Map.entry(10, "AUTO"), Map.entry(11, "RTL"), Map.entry(12, "SMART_RTL"),
            Map.entry(15, "GUIDED"), Map.entry(16, "INITIALISING")
    );

    private static final Map<Integer, String> MODE_MAP_TRACKER = Map.ofEntries(
            Map.entry(0, "MANUAL"), Map.entry(1, "STOP"), Map.entry(2, "SCAN"),
            Map.entry(3, "SERVO_TEST"), Map.entry(10, "AUTO"), Map.entry(16, "INITIALISING")
    );

    private static final Map<Integer, String> MODE_MAP_SUB = Map.ofEntries(
            Map.entry(0, "STABILIZE"), Map.entry(1, "ACRO"), Map.entry(2, "ALT_HOLD"),
            Map.entry(3, "AUTO"), Map.entry(4, "GUIDED"), Map.entry(7, "CIRCLE"),
            Map.entry(9, "SURFACE"), Map.entry(16, "POSHOLD"), Map.entry(19, "MANUAL"),
            Map.entry(20, "MOTOR_DETECT")
    );

    private final Map<Integer, FormatDef> formats = new HashMap<>();
    private String vehicleType; // "plane" | "copter" | "rover" | "tracker" | "sub" | null
    private boolean homeSet = false;

    public BinParser(Path file) {
        // no-op: all state that depends on the file is built up in parse()
    }

    @Override
    public Telemetry parse(Path file) throws IOException, ParserException {

        byte[] bytes = Files.readAllBytes(file);
        BinaryReader r = new BinaryReader(bytes);

        Telemetry telemetry = new Telemetry();

        // The FMT message (type 128) describes itself; seed it so the first
        // FMT record in the file can be decoded.
        formats.put(FMT_MSG_TYPE, new FormatDef(FMT_MSG_TYPE, "FMT", "BBnNZ",
                new String[]{"Type", "Length", "Name", "Format", "Columns"}));

        int recordCount = 0;

        while (r.remaining() >= 3) {

            int h1 = r.uint8();
            if (h1 != HEAD1) continue;

            if (r.remaining() < 2) break;
            int afterH1 = r.position();
            int h2 = r.uint8();
            if (h2 != HEAD2) {
                // False match on the first header byte - rewind to just
                // after it and keep scanning byte by byte.
                r.position(afterH1);
                continue;
            }

            if (r.remaining() < 1) break;
            int type = r.uint8();

            FormatDef def = formats.get(type);
            if (def == null) {
                // A message type whose FMT record we haven't seen yet (or a
                // corrupted file). We don't know its length, so we can't
                // safely skip over it - stop rather than mis-parsing the
                // rest of the file as garbage.
                break;
            }

            int recordStart = r.position();
            if (r.remaining() < def.size()) break; // truncated trailing record

            Map<String, Object> fields;
            try {
                fields = readRecord(r, def);
            } catch (RuntimeException e) {
                break; // corrupt/truncated record
            }

            if (type == FMT_MSG_TYPE) {
                try {
                    FormatDef parsed = FormatDef.fromFmtRecord(fields);
                    formats.put(parsed.type(), parsed);
                } catch (RuntimeException e) {
                    // Unrecognized format character or malformed FMT record:
                    // skip registering this message type, keep going.
                }
            } else {
                handleRecord(def.name(), fields, telemetry);
            }

            // Always land exactly at the end of the record, regardless of
            // how many bytes were actually consumed while decoding it.
            r.position(recordStart + def.size());
            recordCount++;
        }

        if (recordCount == 0) {
            throw new ParserException("No recognizable ArduPilot dataflash records found in " + file);
        }

        return telemetry;
    }

    // -------------------------------------------------------------------
    // Record -> Telemetry dispatch
    // -------------------------------------------------------------------

    private void handleRecord(String name, Map<String, Object> fields, Telemetry t) {

        Long timeUs = asLong(fields.get("TimeUS"));

        switch (name) {

            case "MSG" -> detectVehicleType(String.valueOf(fields.get("Message")));

            case "ATT" -> {
                if (timeUs == null) break;
                Float roll = asFloat(fields.get("Roll"));
                Float pitch = asFloat(fields.get("Pitch"));
                Float yaw = asFloat(fields.get("Yaw"));
                if (roll != null) {
                    t.roll.add(timeUs, roll);
                    t.addCapability(TelemetryCapability.ROLL_PITCH);
                }
                if (pitch != null) {
                    t.pitch.add(timeUs, pitch);
                    t.addCapability(TelemetryCapability.ROLL_PITCH);
                }
                if (yaw != null) {
                    t.heading.add(timeUs, yaw);
                    t.addCapability(TelemetryCapability.HEADING);
                }
            }

            case "GPS", "GPS2" -> {
                if (timeUs == null) break;
                // ASSUMPTION: Lat/Lng stored as raw int32 degrees*1e7 ('L').
                Object latRaw = fields.get("Lat");
                Object lngRaw = firstField(fields, "Lng", "Lon");
                if (latRaw != null) {
                    float lat = (float) (((Number) latRaw).longValue() / 1.0e7);
                    t.latitude.add(timeUs, lat);
                    t.addCapability(TelemetryCapability.LATITUDE);
                }
                if (lngRaw != null) {
                    float lng = (float) (((Number) lngRaw).longValue() / 1.0e7);
                    t.longitude.add(timeUs, lng);
                    t.addCapability(TelemetryCapability.LONGITUDE);
                }
                Float alt = asFloat(fields.get("Alt"));
                if (alt != null) {
                    t.altitude.add(timeUs, alt);
                    t.addCapability(TelemetryCapability.ALTITUDE);
                }
                Float spd = asFloat(fields.get("Spd"));
                if (spd != null) {
                    t.groundSpeed.add(timeUs, spd);
                    t.addCapability(TelemetryCapability.GROUND_SPEED);
                }
                Integer status = asInt(fields.get("Status"));
                if (status != null) {
                    t.gpsFix.add(timeUs, status);
                    t.addCapability(TelemetryCapability.GPS);
                }
                Integer nsats = asInt(firstField(fields, "NSats"));
                if (nsats != null) {
                    t.satellites.add(timeUs, nsats);
                    t.addCapability(TelemetryCapability.SATELLITES);
                }
            }

            case "POS" -> {
                if (timeUs == null) break;
                Float relAlt = asFloat(firstField(fields, "RelHomeAlt", "RelOriginAlt"));
                if (relAlt != null) {
                    t.relativeAltitude.add(timeUs, relAlt);
                    t.addCapability(TelemetryCapability.RELATIVE_ALTITUDE);
                }
            }

            case "CTUN" -> {
                if (timeUs == null) break;
                Float climb = asFloat(firstField(fields, "ClimbRate", "CRt"));
                if (climb != null) {
                    t.climbRate.add(timeUs, climb);
                    t.addCapability(TelemetryCapability.CLIMB_RATE);
                }
                Float vspd = asFloat(fields.get("VSpd"));
                if (vspd != null) {
                    t.verticalSpeed.add(timeUs, vspd);
                    t.addCapability(TelemetryCapability.VERTICAL_SPEED);
                }
            }

            case "ARSP" -> {
                if (timeUs == null) break;
                Float airspeed = asFloat(fields.get("Airspeed"));
                if (airspeed != null) {
                    t.airSpeed.add(timeUs, airspeed);
                    t.addCapability(TelemetryCapability.AIR_SPEED);
                }
            }

            case "BAT" -> {
                if (timeUs == null) break;
                Integer instance = asInt(fields.get("Instance"));
                if (instance == null || instance == 0) {
                    addBatteryFields(t, timeUs, fields);
                }
            }

            case "CURR" -> {
                // Pre-"Instance"-field firmware; single implicit battery.
                if (timeUs != null) addBatteryFields(t, timeUs, fields);
            }

            case "WIND" -> {
                if (timeUs == null) break;
                Float speed = asFloat(fields.get("Speed"));
                Float dir = asFloat(fields.get("Direction"));
                if (speed != null) {
                    t.windSpeed.add(timeUs, speed);
                    t.addCapability(TelemetryCapability.WIND_SPEED);
                }
                if (dir != null) {
                    t.windDirection.add(timeUs, dir);
                    t.addCapability(TelemetryCapability.WIND_DIRECTION);
                }
            }

            // ASSUMPTION: RSSI comes from either a "RAD" (telemetry radio)
            // or "RSSI" record depending on firmware/setup. Value is left
            // as whatever raw scale the log uses (not normalized to 0-100).
            case "RAD" -> {
                if (timeUs == null) break;
                Integer rssi = asInt(fields.get("RSSI"));
                if (rssi != null) {
                    t.rssi.add(timeUs, rssi);
                    t.addCapability(TelemetryCapability.RSSI);
                }
            }
            case "RSSI" -> {
                if (timeUs == null) break;
                Integer rssi = asInt(firstField(fields, "RXRSSI", "RSSI"));
                if (rssi != null) {
                    t.rssi.add(timeUs, rssi);
                    t.addCapability(TelemetryCapability.RSSI);
                }
            }

            case "MODE" -> {
                if (timeUs == null) break;
                Integer mode = asInt(fields.get("Mode"));
                if (mode != null) {
                    String modeName = modeToString(mode);
                    if (modeName != null) {
                        t.flightMode.add(timeUs, modeName);
                        t.addCapability(TelemetryCapability.FLIGHT_MODE);
                    }
                }
            }

            // ASSUMPTION: armed state derived from EV ids 10/11 - verify
            // against your firmware's LogEvent enum if this looks wrong.
            case "EV" -> {
                if (timeUs == null) break;
                Integer id = asInt(fields.get("Id"));
                if (id != null && (id == EVENT_ARMED || id == EVENT_DISARMED)) {
                    t.armed.add(timeUs, id == EVENT_ARMED);
                    t.addCapability(TelemetryCapability.ARMED);
                }
            }

            // ASSUMPTION: first ORGN record in the log is treated as home,
            // regardless of its Type field (EKF origin vs AHRS home etc.).
            case "ORGN" -> {
                if (!homeSet) {
                    Object latRaw = fields.get("Lat");
                    Object lngRaw = fields.get("Lng");
                    if (latRaw != null && lngRaw != null) {
                        double lat = ((Number) latRaw).longValue() / 1.0e7;
                        double lng = ((Number) lngRaw).longValue() / 1.0e7;
                        Object altRaw = fields.get("Alt");
                        double alt = altRaw != null ? ((Number) altRaw).doubleValue() : 0.0;
                        t.setHome(new Home(lat, lng, alt));
                        t.addCapability(TelemetryCapability.HOME);
                        homeSet = true;
                    }
                }
            }

            default -> {
                // Message type not mapped to a Telemetry channel - ignored.
            }
        }
    }

    private void addBatteryFields(Telemetry t, long timeUs, Map<String, Object> fields) {
        Float volt = asFloat(fields.get("Volt"));
        Float curr = asFloat(fields.get("Curr"));
        Integer remPct = asInt(fields.get("RemPct"));
        if (volt != null) {
            t.batteryVoltage.add(timeUs, volt);
            t.addCapability(TelemetryCapability.BATTERY_VOLTAGE);
        }
        if (curr != null) {
            t.batteryCurrent.add(timeUs, curr);
            t.addCapability(TelemetryCapability.BATTERY_CURRENT);
        }
        if (remPct != null) {
            t.batteryRemaining.add(timeUs, remPct);
            t.addCapability(TelemetryCapability.BATTERY_REMAINING);
        }
    }

    private String modeToString(int mode) {
        Map<Integer, String> map = switch (vehicleType == null ? "copter" : vehicleType) {
            case "plane" -> MODE_MAP_PLANE;
            case "rover" -> MODE_MAP_ROVER;
            case "tracker" -> MODE_MAP_TRACKER;
            case "sub" -> MODE_MAP_SUB;
            default -> MODE_MAP_COPTER;
        };
        return map.get(mode);
    }

    private void detectVehicleType(String message) {
        if (message == null || vehicleType != null) return; // keep first match
        String m = message.toLowerCase();
        if (m.contains("arduplane")) vehicleType = "plane";
        else if (m.contains("arducopter")) vehicleType = "copter";
        else if (m.contains("ardusub")) vehicleType = "sub";
        else if (m.contains("rover")) vehicleType = "rover";
        else if (m.contains("tracker")) vehicleType = "tracker";
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private static Object firstField(Map<String, Object> fields, String... names) {
        for (String n : names) {
            Object v = fields.get(n);
            if (v != null) return v;
        }
        return null;
    }

    private static Float asFloat(Object o) {
        return o == null ? null : ((Number) o).floatValue();
    }

    private static Integer asInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    private static Long asLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private Map<String, Object> readRecord(BinaryReader r, FormatDef def) {
        Map<String, Object> fields = new HashMap<>();
        String format = def.format();
        String[] columns = def.columns();
        for (int i = 0; i < format.length(); i++) {
            Object value = r.readByFormat(format.charAt(i));
            String colName = i < columns.length ? columns[i] : ("field" + i);
            fields.put(colName, value);
        }
        return fields;
    }

    // -------------------------------------------------------------------
    // Format table entry (from FMT records)
    // -------------------------------------------------------------------

    private static final class FormatDef {
        private final int type;
        private final String name;
        private final String format;
        private final String[] columns;
        private final int size;

        FormatDef(int type, String name, String format, String[] columns) {
            this.type = type;
            this.name = name;
            this.format = format;
            this.columns = columns;
            this.size = computeSize(format);
        }

        static FormatDef fromFmtRecord(Map<String, Object> fields) {
            int type = ((Number) fields.get("Type")).intValue();
            String name = String.valueOf(fields.get("Name"));
            String format = String.valueOf(fields.get("Format"));
            String columnsStr = String.valueOf(fields.get("Columns"));
            String[] columns = columnsStr.isEmpty() ? new String[0] : columnsStr.split(",");
            return new FormatDef(type, name, format, columns);
        }

        int type() { return type; }
        String name() { return name; }
        String format() { return format; }
        String[] columns() { return columns; }
        int size() { return size; }

        private static int computeSize(String format) {
            int total = 0;
            for (int i = 0; i < format.length(); i++) {
                total += fieldSize(format.charAt(i));
            }
            return total;
        }

        private static int fieldSize(char type) {
            return switch (type) {
                case 'b', 'B', 'M' -> 1;
                case 'h', 'H', 'c', 'C' -> 2;
                case 'i', 'I', 'f', 'n', 'E', 'e', 'L' -> 4;
                case 'd', 'Q', 'q' -> 8;
                case 'N' -> 16;
                case 'a', 'Z' -> 64;
                default -> throw new IllegalArgumentException("Unknown format char: " + type);
            };
        }
    }

    // -------------------------------------------------------------------
    // Little-endian byte[] reader (mirrors JS DataView usage in the
    // original DataflashParser, plus exact 64-bit ints - no float hack
    // needed since Java longs are natively 64-bit).
    // -------------------------------------------------------------------

    private static final class BinaryReader {
        private final byte[] data;
        private int pos = 0;

        BinaryReader(byte[] data) { this.data = data; }

        int position() { return pos; }
        void position(int p) { pos = p; }
        int remaining() { return data.length - pos; }

        int uint8() { return data[pos++] & 0xFF; }
        int int8() { return data[pos++]; }

        int uint16() {
            int v = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
            pos += 2;
            return v;
        }

        short int16() { return (short) uint16(); }

        long uint32() {
            long v = (data[pos] & 0xFFL)
                    | ((data[pos + 1] & 0xFFL) << 8)
                    | ((data[pos + 2] & 0xFFL) << 16)
                    | ((data[pos + 3] & 0xFFL) << 24);
            pos += 4;
            return v;
        }

        int int32() { return (int) uint32(); }

        long int64() {
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v |= (data[pos + i] & 0xFFL) << (8 * i);
            }
            pos += 8;
            return v;
        }

        float float32() { return Float.intBitsToFloat(int32()); }
        double float64() { return Double.longBitsToDouble(int64()); }

        String fixedString(int len) {
            int end = pos;
            int limit = pos + len;
            while (end < limit && data[end] != 0) end++;
            String s = new String(data, pos, end - pos, StandardCharsets.US_ASCII);
            pos = limit;
            return s;
        }

        Object readByFormat(char type) {
            return switch (type) {
                case 'a' -> { pos += 64; yield null; } // int16_t[32], unused
                case 'b' -> (int) int8();
                case 'B' -> uint8();
                case 'h' -> (int) int16();
                case 'H' -> uint16();
                case 'i' -> int32();
                case 'I' -> uint32();
                case 'f' -> float32();
                case 'd' -> float64();
                case 'Q' -> int64();  // treated as signed long; ArduPilot values fit comfortably
                case 'q' -> int64();
                case 'n' -> fixedString(4);
                case 'N' -> fixedString(16);
                case 'Z' -> fixedString(64);
                case 'c' -> int16() / 100.0;
                case 'C' -> uint16() / 100.0;
                case 'E' -> uint32() / 100.0;
                case 'e' -> int32() / 100.0;
                case 'L' -> (long) int32();
                case 'M' -> uint8();
                default -> throw new IllegalArgumentException("Unknown format char: " + type);
            };
        }
    }
}