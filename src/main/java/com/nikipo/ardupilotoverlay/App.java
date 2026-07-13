package com.nikipo.ardupilotoverlay;

import com.nikipo.ardupilotoverlay.config.AppDirectories;
import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.ui.MainApplication;
import javafx.application.Application;
import org.slf4j.Logger;

import java.io.IOException;

public class App {

    private static final Logger logger = AppLogger.getLogger(App.class);

    public static void main(String[] args) {

        logger.info("Ardupilot Overlay  starting");

        try {

            AppDirectories appDirectories = new AppDirectories();

            logger.info("Root Directory      : {}", appDirectories.getRootDirectory());
            logger.info("Config Directory    : {}", appDirectories.getConfigDirectory());
            logger.info("Logs Directory      : {}", appDirectories.getLogsDirectory());
            logger.info("Cache Directory     : {}", appDirectories.getCacheDirectory());
            logger.info("Presets Directory   : {}", appDirectories.getPresetsDirectory());
            logger.info("Autosave Directory  : {}", appDirectories.getAutosaveDirectory());

        } catch (IOException e) {

            logger.error("Failed to initialize application directories.", e);
            System.exit(1);

        }

        logger.info("Application initialized successfully.");

        Application.launch(MainApplication.class, args);
    }

}