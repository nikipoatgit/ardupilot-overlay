package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HomeController implements Navigable {
    private UIOrchestrator orchestrator;

    @Override
    public void setOrchestrator(UIOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    @FXML
    private void onNewProject(ActionEvent event) {
        orchestrator.showMediaSelector();
    }

    @FXML
    private void onOpenProject(ActionEvent event) {
        System.out.println("Open Project");
    }
}
