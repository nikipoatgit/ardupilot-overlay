package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TelemetryCapability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesCommonTelemetryColumnsAndSeconds() throws IOException, ParserException {
        Path file = tempDir.resolve("flight.csv");
        Files.writeString(file, "time,Roll,Lat,Voltage,Armed,Mode\n"
                + "0,1.5,51.2,12.4,false,MANUAL\n"
                + "1,3.5,51.4,12.1,true,AUTO\n");

        Telemetry telemetry = new CsvParser(file).parse(file);

        assertEquals(2, telemetry.roll.size());
        assertEquals(2.5, telemetry.roll.sample(500_000), 0.0001);
        assertEquals(51.4, telemetry.latitude.latest(), 0.0001);
        assertEquals(12.1, telemetry.batteryVoltage.latest(), 0.0001);
        assertEquals(Boolean.TRUE, telemetry.armed.latest());
        assertEquals("AUTO", telemetry.flightMode.latest());
        assertTrue(telemetry.hasCapability(TelemetryCapability.ROLL_PITCH));
        assertTrue(telemetry.hasCapability(TelemetryCapability.LATITUDE));
        assertTrue(telemetry.hasCapability(TelemetryCapability.BATTERY_VOLTAGE));
        assertTrue(telemetry.hasCapability(TelemetryCapability.ARMED));
        assertTrue(telemetry.hasCapability(TelemetryCapability.FLIGHT_MODE));
    }
}
