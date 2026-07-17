package com.nikipo.ardupilotoverlay.util;

import javafx.stage.FileChooser;

public class FileFilters {
    public static final FileChooser.ExtensionFilter VIDEO =
            new FileChooser.ExtensionFilter(
                    "Video Files",
                    "*.mp4",
                    "*.mov",
                    "*.avi",
                    "*.mkv",
                    "*.webm",
                    "*.m4v",
                    "*.wmv",
                    "*.flv",
                    "*.mpg",
                    "*.mpeg",
                    "*.ts",
                    "*.mts",
                    "*.m2ts",
                    "*.3gp"
            );

    public static final FileChooser.ExtensionFilter TELEMETRY =
            new FileChooser.ExtensionFilter(
                    "Telemetry Files",
                    "*.bin", "*.tlog", "*.csv"
            );

    public static final FileChooser.ExtensionFilter PROJECT =
            new FileChooser.ExtensionFilter(
                    "ArduOverlay Project",
                    "*.json"
            );

    public static final FileChooser.ExtensionFilter ALL =
            new FileChooser.ExtensionFilter(
                    "All Files",
                    "*.*"
            );
}
