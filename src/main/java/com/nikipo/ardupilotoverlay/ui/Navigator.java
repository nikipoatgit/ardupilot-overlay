package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.ui.controllers.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class Navigator {

    private final MainLayoutController layout;
    private static final Logger logger = AppLogger.getLogger(Navigator.class);


    public Navigator(MainLayoutController layout) {
        this.layout = layout;
    }

    public void showHome(){
        try{
            load("/fxml/Home.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }

    }

    public void showMediaSelector()  {
        try{
            load("/fxml/MediaSelector.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }
    }


    public void showProjectConfig()  {
        try{
            load("/fxml/ProjectConfig.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }
    }

    public void showTelemetrySelector()  {
        try{
            load("/fxml/TelemetrySelector.fxml");

        } catch (IOException e) {
            logger.error("Failed at", e);
            System.exit(1);
        }
    }

    public void showOverlay()  {
        try{
            load("/fxml/Overlay.fxml");

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

        Parent root = loader.load();

        Object controller = loader.getController();

        if (controller instanceof Navigable navigable) {
            navigable.setNavigator(this);
        }

        layout.setContent(root);

    }

}
