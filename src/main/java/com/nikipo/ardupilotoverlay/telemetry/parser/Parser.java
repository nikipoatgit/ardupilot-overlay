package com.nikipo.ardupilotoverlay.telemetry.parser;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;

import java.io.IOException;
import java.nio.file.Path;

public class Parser {

    private final Path file;

    public Parser(Path file) {
        this.file = file;
    }

    public Telemetry getTelemetry() throws IOException, ParserException {

        String extension = getExtension(file);

        TelemetryParser parser = switch (extension.toLowerCase()) {
            case "bin" -> new BinParser(file);
            case "tlog" -> new TLogParser(file);
            case "csv" -> new CsvParser(file);
            default -> throw new ParserException(
                    "Unsupported telemetry file: " + extension);
        };

        return parser.parse(file);
    }

    private static String getExtension(Path path) {

        String name = path.getFileName().toString();

        int dot = name.lastIndexOf('.');

        if (dot == -1)
            return "";

        return name.substring(dot + 1);
    }
}