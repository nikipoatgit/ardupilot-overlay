package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.Navigator;
import javafx.event.ActionEvent;

public class OverlayController implements Navigable {

    private Navigator navigator;

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void onContinue(ActionEvent event) {
        System.out.println("Overlay page reached.");
        // navigator.showExport();
    }
}