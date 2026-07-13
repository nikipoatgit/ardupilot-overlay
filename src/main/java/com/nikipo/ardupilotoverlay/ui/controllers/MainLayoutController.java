package com.nikipo.ardupilotoverlay.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class MainLayoutController {
    @FXML
    private BorderPane root;

    public void setContent(Node node) {
        root.setCenter(node);
    }

    public BorderPane getRoot() {
        return root;
    }
}
