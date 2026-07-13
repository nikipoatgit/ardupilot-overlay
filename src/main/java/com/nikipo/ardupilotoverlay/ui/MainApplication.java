package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.App;
import com.nikipo.ardupilotoverlay.logging.AppLogger;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;

public class MainApplication extends Application {

    private static final Logger logger = AppLogger.getLogger(MainApplication.class);

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
