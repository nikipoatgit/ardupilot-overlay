package com.nikipo.ardupilotoverlay.ui.navigation;

import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class NavigationBarController {

    private UIOrchestrator orchestrator;
    private WizardStep currentStep;

    @FXML private Button homeButton;
    @FXML private Button videoButton;
    @FXML private Button projectButton;
    @FXML private Button telemetryButton;
    @FXML private Button overlayButton;
    @FXML private Button exportButton;

    public void setOrchestrator(UIOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        refresh();
    }

    public void refresh(WizardStep currentStep) {
        this.currentStep = currentStep;
        refresh();
    }

    public void refresh() {
        if (orchestrator == null) {
            return;
        }

        videoButton.setDisable(!orchestrator.canNavigateTo(WizardStep.VIDEO));
        projectButton.setDisable(!orchestrator.canNavigateTo(WizardStep.PROJECT));
        telemetryButton.setDisable(!orchestrator.canNavigateTo(WizardStep.TELEMETRY));
        overlayButton.setDisable(!orchestrator.canNavigateTo(WizardStep.OVERLAY));
        exportButton.setDisable(!orchestrator.canNavigateTo(WizardStep.EXPORT));

        setActive(homeButton, currentStep == WizardStep.HOME);
        setActive(videoButton, currentStep == WizardStep.VIDEO);
        setActive(projectButton, currentStep == WizardStep.PROJECT);
        setActive(telemetryButton, currentStep == WizardStep.TELEMETRY);
        setActive(overlayButton, currentStep == WizardStep.OVERLAY);
        setActive(exportButton, currentStep == WizardStep.EXPORT);
    }

    private static void setActive(Button button, boolean active) {
        button.getStyleClass().remove("breadcrumb-active");
        if (active) {
            button.getStyleClass().add("breadcrumb-active");
        }
    }


    // for now we dont need btns to do any thing
    @FXML
    private void onHome(ActionEvent event) {
//        orchestrator.showHome();
    }

    @FXML
    private void onVideo(ActionEvent event) {
//        orchestrator.showMediaSelector();
    }

    @FXML
    private void onProject(ActionEvent event) {
//        orchestrator.showProjectConfig();
    }

    @FXML
    private void onTelemetry(ActionEvent event) {
//        orchestrator.showTelemetrySelector();
    }

    @FXML
    private void onOverlay(ActionEvent event) {
//        orchestrator.showOverlay();
    }

    @FXML
    private void onExport(ActionEvent event) {
//        orchestrator.showExport();
    }
}
