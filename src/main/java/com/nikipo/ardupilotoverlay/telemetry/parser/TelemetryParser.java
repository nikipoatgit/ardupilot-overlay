package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;

import java.io.IOException;
import java.nio.file.Path;

public interface TelemetryParser {

    Telemetry parse(Path file)
            throws IOException, ParserException;

}