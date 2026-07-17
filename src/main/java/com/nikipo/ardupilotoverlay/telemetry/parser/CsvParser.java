package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;

import java.io.IOException;
import java.nio.file.Path;

public class CsvParser implements TelemetryParser{
    public CsvParser(Path file) {
    }

    @Override
    public Telemetry parse(Path file) throws IOException, ParserException {
        return null;
    }
}
