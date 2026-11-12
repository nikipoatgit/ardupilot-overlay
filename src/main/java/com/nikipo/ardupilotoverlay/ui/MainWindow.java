package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.ui.controllers.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainWindow {

    private final Stage stage;
    private final Scene scene;
        private final UIOrchestrator orchestrator;

    public MainWindow(Stage stage) throws IOException {

        this.stage = stage;

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource("/fxml/MainLayout.fxml")
                )
        );

        Parent root = loader.load();

        MainLayoutController controller = loader.getController();

        scene = new Scene(
                root,
                Math.max(1100, bounds.getWidth() * 0.8),
                Math.max(700, bounds.getHeight() * 0.8)
        );

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/theme.css")
                ).toExternalForm()
        );
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/overlay.css")
                ).toExternalForm()
        );

        orchestrator = new UIOrchestrator(controller);
        controller.setOrchestrator(orchestrator);

        orchestrator.showHome();

        stage.setTitle("Ardupilot Overlay");
        // since scene has max dim of 1100 and 700  set stage min dim to it
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    public UIOrchestrator getOrchestrator() {
        return orchestrator;
    }
}