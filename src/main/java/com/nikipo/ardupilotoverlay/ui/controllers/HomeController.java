package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.ui.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HomeController implements Navigable{
    private Navigator navigator;

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }
    @FXML
    private void onNewProject(ActionEvent event) {
        navigator.showVideo();
    }

    @FXML
    private void onOpenProject(ActionEvent event) {
        System.out.println("Open Project");
    }
}
