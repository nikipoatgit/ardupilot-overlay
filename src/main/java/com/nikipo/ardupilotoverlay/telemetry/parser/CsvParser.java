package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.telemetry.ContinuousTimeSeries;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TelemetryCapability;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CsvParser implements TelemetryParser{
    public CsvParser(Path file) {
    }

    @Override
    public Telemetry parse(Path file) throws IOException, ParserException {
        if (file == null) {
            throw new ParserException("CSV file must not be null");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (CSVParser parser = CSVParser.parse(file, StandardCharsets.UTF_8, format)) {
            Map<String, String> headers = new HashMap<>();
            for (String header : parser.getHeaderNames()) {
                headers.put(normalize(header), header);
            }

            String timeHeader = findHeader(headers, "timeus", "timestampus", "time", "timestamp");
            if (timeHeader == null) {
                throw new ParserException("CSV must contain a time or timestamp column");
            }

            Telemetry telemetry = new Telemetry();
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                if (record.size() == 0) {
                    continue;
                }

                try {
                    long timeUs = parseTime(record.get(timeHeader), normalize(timeHeader));
                    addContinuous(record, headers, timeUs, telemetry);
                    addDiscrete(record, headers, timeUs, telemetry);
                } catch (RuntimeException e) {
                    throw new ParserException("Invalid CSV data at row " + rowNumber, e);
                }
            }

            if (hasNoSamples(telemetry)) {
                throw new ParserException("CSV contains no telemetry samples");
            }
            return telemetry;
        }
    }

    private static void addContinuous(CSVRecord record, Map<String, String> headers,
                                      long timeUs, Telemetry telemetry) {
        addFloat(record, headers, timeUs, telemetry, telemetry.roll, TelemetryCapability.ROLL_PITCH, "roll");
        addFloat(record, headers, timeUs, telemetry, telemetry.pitch, TelemetryCapability.ROLL_PITCH, "pitch");
        addFloat(record, headers, timeUs, telemetry, telemetry.heading, TelemetryCapability.HEADING, "heading", "yaw");
        addFloat(record, headers, timeUs, telemetry, telemetry.latitude, TelemetryCapability.LATITUDE, "latitude", "lat");
        addFloat(record, headers, timeUs, telemetry, telemetry.longitude, TelemetryCapability.LONGITUDE, "longitude", "lng", "lon");
        addFloat(record, headers, timeUs, telemetry, telemetry.altitude, TelemetryCapability.ALTITUDE, "altitude", "alt");
        addFloat(record, headers, timeUs, telemetry, telemetry.relativeAltitude, TelemetryCapability.RELATIVE_ALTITUDE, "relativealtitude", "relalt");
        addFloat(record, headers, timeUs, telemetry, telemetry.groundSpeed, TelemetryCapability.GROUND_SPEED, "groundspeed", "speed");
        addFloat(record, headers, timeUs, telemetry, telemetry.airSpeed, TelemetryCapability.AIR_SPEED, "airspeed");
        addFloat(record, headers, timeUs, telemetry, telemetry.verticalSpeed, TelemetryCapability.VERTICAL_SPEED, "verticalspeed");
        addFloat(record, headers, timeUs, telemetry, telemetry.climbRate, TelemetryCapability.CLIMB_RATE, "climbrate");
        addFloat(record, headers, timeUs, telemetry, telemetry.batteryVoltage, TelemetryCapability.BATTERY_VOLTAGE, "batteryvoltage", "voltage", "volt");
        addFloat(record, headers, timeUs, telemetry, telemetry.batteryCurrent, TelemetryCapability.BATTERY_CURRENT, "batterycurrent", "current", "curr");
        addFloat(record, headers, timeUs, telemetry, telemetry.windSpeed, TelemetryCapability.WIND_SPEED, "windspeed");
        addFloat(record, headers, timeUs, telemetry, telemetry.windDirection, TelemetryCapability.WIND_DIRECTION, "winddirection", "winddir");
    }

    private static void addDiscrete(CSVRecord record, Map<String, String> headers,
                                    long timeUs, Telemetry telemetry) {
        addInteger(record, headers, timeUs, telemetry, telemetry.batteryRemaining,
                TelemetryCapability.BATTERY_REMAINING, "batteryremaining", "remaining", "rempct");
        addInteger(record, headers, timeUs, telemetry, telemetry.satellites,
                TelemetryCapability.SATELLITES, "satellites", "numsatellites", "nsats");
        addInteger(record, headers, timeUs, telemetry, telemetry.gpsFix,
                TelemetryCapability.GPS, "gpsfix", "gpsstatus", "fix");
        addInteger(record, headers, timeUs, telemetry, telemetry.rssi,
                TelemetryCapability.RSSI, "rssi", "signalstrength");

        String armedHeader = findHeader(headers, "armed");
        if (armedHeader != null && hasValue(record, armedHeader)) {
            telemetry.armed.add(timeUs, parseBoolean(record.get(armedHeader)));
            telemetry.addCapability(TelemetryCapability.ARMED);
        }

        String modeHeader = findHeader(headers, "flightmode", "mode");
        if (modeHeader != null && hasValue(record, modeHeader)) {
            telemetry.flightMode.add(timeUs, record.get(modeHeader));
            telemetry.addCapability(TelemetryCapability.FLIGHT_MODE);
        }
    }

    private static void addFloat(CSVRecord record, Map<String, String> headers, long timeUs,
                                 Telemetry telemetry, ContinuousTimeSeries<Float> series,
                                 TelemetryCapability capability, String... names) {
        String header = findHeader(headers, names);
        if (header != null && hasValue(record, header)) {
            series.add(timeUs, Float.parseFloat(record.get(header)));
            telemetry.addCapability(capability);
        }
    }

    private static void addInteger(CSVRecord record, Map<String, String> headers, long timeUs,
                                   Telemetry telemetry, ContinuousTimeSeries<Integer> series,
                                   TelemetryCapability capability, String... names) {
        String header = findHeader(headers, names);
        if (header != null && hasValue(record, header)) {
            series.add(timeUs, Integer.parseInt(record.get(header)));
            telemetry.addCapability(capability);
        }
    }

    private static String findHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            String header = headers.get(normalize(name));
            if (header != null) {
                return header;
            }
        }
        return null;
    }

    private static boolean hasValue(CSVRecord record, String header) {
        return record.isMapped(header) && !record.get(header).isBlank();
    }

    private static long parseTime(String value, String normalizedHeader) {
        double numeric = Double.parseDouble(value);
        if (normalizedHeader.endsWith("us")) {
            return Math.round(numeric);
        }
        return Math.round(numeric * 1_000_000.0);
    }

    private static boolean parseBoolean(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "armed", "on" -> true;
            case "0", "false", "no", "disarmed", "off" -> false;
            default -> throw new IllegalArgumentException("Unsupported boolean value: " + value);
        };
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static boolean hasNoSamples(Telemetry telemetry) {
        return telemetry.roll.size() == 0
                && telemetry.pitch.size() == 0
                && telemetry.heading.size() == 0
                && telemetry.latitude.size() == 0
                && telemetry.longitude.size() == 0
                && telemetry.altitude.size() == 0
                && telemetry.batteryVoltage.size() == 0
                && telemetry.flightMode.size() == 0;
    }
}
