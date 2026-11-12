package com.nikipo.ardupilotoverlay.config;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDirectories {

    public static final String APP_NAME = "ArdupilotOverlay";
    public static final String APP_AUTHOR = "Nikipo";
    public static final String APP_VERSION = "1.0.0";


    private final Path rootDirectory;
    private final Path configDirectory;
    private final Path logsDirectory;
    private final Path cacheDirectory;
    private final Path presetsDirectory;
    private final Path autosaveDirectory;

    public AppDirectories() throws IOException {

        AppDirs appDirs = AppDirsFactory.getInstance();

        rootDirectory = Path.of(
                appDirs.getUserConfigDir(APP_NAME, APP_VERSION, APP_AUTHOR)
        );

        configDirectory = rootDirectory.resolve("config");
        logsDirectory = rootDirectory.resolve("logs");
        cacheDirectory = rootDirectory.resolve("cache");
        presetsDirectory = rootDirectory.resolve("presets");
        autosaveDirectory = rootDirectory.resolve("autosave");

        createDirectories();
    }

    private void createDirectories() throws IOException {

        // If the directory doesn't exist , it is created.
        //If it already exists ,nothing happens.
        //If parent directories don't exist , they are created automatically.
        Files.createDirectories(configDirectory);
        Files.createDirectories(logsDirectory);
        Files.createDirectories(cacheDirectory);
        Files.createDirectories(presetsDirectory);
        Files.createDirectories(autosaveDirectory);
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getConfigDirectory() {
        return configDirectory;
    }

    public Path getLogsDirectory() {
        return logsDirectory;
    }

    public Path getCacheDirectory() {
        return cacheDirectory;
    }

    public Path getPresetsDirectory() {
        return presetsDirectory;
    }

    public Path getAutosaveDirectory() {
        return autosaveDirectory;
    }
}