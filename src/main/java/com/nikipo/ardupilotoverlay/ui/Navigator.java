package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.ui.controllers.Navigable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class Navigator {

    private final Scene scene;
    private static final Logger logger = AppLogger.getLogger(Navigator.class);


    public Navigator(Scene scene) {
        this.scene = scene;
    }

    public void showHome(){
        try{
            load("/fxml/Home.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }

    }

    public void showVideo()  {

        try{
            load("/fxml/MediaSelector.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }
    }

    private void load(String fxml) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource(fxml)
                )
        );

        Object controller = loader.getController();

        if (controller instanceof Navigable navigable) {
            navigable.setNavigator(this);
        }

        Parent root = loader.load();

        scene.setRoot(root);
    }

}