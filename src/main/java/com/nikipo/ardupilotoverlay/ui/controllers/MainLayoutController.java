package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import com.nikipo.ardupilotoverlay.ui.navigation.NavigationBarController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class MainLayoutController {
    @FXML
    private BorderPane root;

    @FXML
    private NavigationBarController navigationBarController;

    public void setContent(Node node) {
        root.setCenter(node);
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setOrchestrator(UIOrchestrator orchestrator) {
        navigationBarController.setOrchestrator(orchestrator);
    }

    public void refreshNavigation(WizardStep currentStep) {

        navigationBarController.refresh(currentStep);
    }
}
