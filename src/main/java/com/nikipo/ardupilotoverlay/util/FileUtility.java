package com.nikipo.ardupilotoverlay.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

public final class FileUtility {
    private static final String LAST_VIDEO_DIRECTORY = "lastVideoDirectory";
    private static final String LAST_TELEMETRY_DIRECTORY = "lastTelemetryDirectory";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(FileUtility.class);

    public static Path chooseFile(
            Window owner,
            String title,
            List<FileChooser.ExtensionFilter> filters
    ) {
        return chooseFile(owner, title, filters, null);
    }

    public static Path chooseFile(
            Window owner,
            String title,
            List<FileChooser.ExtensionFilter> filters,
            Path initialDirectory
    ) {
        FileChooser chooser = new FileChooser();

        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(filters);
        if (initialDirectory != null && Files.isDirectory(initialDirectory)) {
            chooser.setInitialDirectory(initialDirectory.toFile());
        }

        File file = chooser.showOpenDialog(owner);

        return file == null ? null : file.toPath();
    }

    public static Path getLastVideoDirectory() {
        String directory = PREFERENCES.get(LAST_VIDEO_DIRECTORY, "");
        if (directory.isBlank()) {
            return null;
        }

        Path path = Path.of(directory);
        return Files.isDirectory(path) ? path : null;
    }

    public static void rememberVideoDirectory(Path videoFile) {
        if (videoFile == null || videoFile.getParent() == null) {
            return;
        }

        Path directory = videoFile.toAbsolutePath().normalize().getParent();
        if (Files.isDirectory(directory)) {
            PREFERENCES.put(LAST_VIDEO_DIRECTORY, directory.toString());
        }
    }

    public static Path getLastTelemetryDirectory() {
        String directory = PREFERENCES.get(LAST_TELEMETRY_DIRECTORY, "");
        if (directory.isBlank()) {
            return null;
        }

        Path path = Path.of(directory);
        return Files.isDirectory(path) ? path : null;
    }

    public static void rememberTelemetryDirectory(Path telemetryFile) {
        if (telemetryFile == null || telemetryFile.getParent() == null) {
            return;
        }

        Path directory = telemetryFile.toAbsolutePath().normalize().getParent();
        if (Files.isDirectory(directory)) {
            PREFERENCES.put(LAST_TELEMETRY_DIRECTORY, directory.toString());
        }
    }

    public static boolean FileValidator(Path path) throws IOException {

        if (path == null)
            throw new IllegalArgumentException("Path is null.");

        if (!Files.exists(path))
            throw new IOException("File does not exist.");

        if (!Files.isRegularFile(path))
            throw new IOException("Not a regular file.");

        if (!Files.isReadable(path))
            throw new IOException("File is not readable.");

        if (Files.size(path) == 0)
            throw new IOException("File is empty.");

        String name = path.getFileName().toString().trim();

        if (name.isBlank())
            throw new IOException("Invalid file name.");

        // Windows reserved names
        String base = name.contains(".")
                ? name.substring(0, name.lastIndexOf('.'))
                : name;

        switch (base.toUpperCase()) {
            case "CON":
            case "PRN":
            case "AUX":
            case "NUL":
            case "COM1":
            case "COM2":
            case "COM3":
            case "COM4":
            case "COM5":
            case "COM6":
            case "COM7":
            case "COM8":
            case "COM9":
            case "LPT1":
            case "LPT2":
            case "LPT3":
            case "LPT4":
            case "LPT5":
            case "LPT6":
            case "LPT7":
            case "LPT8":
            case "LPT9":
                throw new IOException("Reserved filename.");
        }

        return true;
    }

    public static String getFileNameWithoutExtension(Path path) {

        String name = path.getFileName().toString();

        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }
}
