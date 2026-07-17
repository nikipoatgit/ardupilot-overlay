package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import org.bytedeco.javacpp.tools.ParserException;

import java.io.IOException;
import java.nio.file.Path;

public class TLogParser implements TelemetryParser{
    public TLogParser(Path file) {
    }

    @Override
    public Telemetry parse(Path file) throws IOException {
        return null;
    }
}
