package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.config.AppDirectories;
import com.nikipo.ardupilotoverlay.logging.AppLogger;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;

public class MainApplication extends Application {

    private static final Logger logger = AppLogger.getLogger(MainApplication.class);

    @Override
    public void init() {
        try {
            new AppDirectories();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize application directories", e);
        }
    }

    @Override
    public void start(Stage stage) {

        FontLoader.loadFonts();

        try{
            MainWindow window = new MainWindow(stage);
            window.show();

        } catch (IOException e) {
            logger.error("Failed to initialize main window.", e);
            System.exit(1);
        }

    }
}
