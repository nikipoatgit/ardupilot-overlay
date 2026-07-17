package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.ui.controllers.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainWindow {

    private final Stage stage;
    private final Scene scene;
    private final Navigator navigator;

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
                bounds.getWidth() * 0.8,
                bounds.getHeight() * 0.8
        );

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/theme.css")
                ).toExternalForm()
        );

        navigator = new Navigator(controller);

        navigator.showHome();

        stage.setTitle("Ardupilot Overlay");
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    public Navigator getNavigator() {
        return navigator;
    }
}