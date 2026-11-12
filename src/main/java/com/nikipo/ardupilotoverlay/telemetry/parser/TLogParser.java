package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;

import java.io.IOException;
import java.nio.file.Path;

public class TLogParser implements TelemetryParser{
    public TLogParser(Path file) {
    }

    @Override
    public Telemetry parse(Path file) throws IOException, ParserException {
        throw new ParserException("TLog parsing is not implemented yet: " + file);
    }
}
