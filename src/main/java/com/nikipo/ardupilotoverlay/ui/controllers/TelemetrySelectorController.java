package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.project.Project;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.telemetry.parser.Parser;
import com.nikipo.ardupilotoverlay.telemetry.parser.ParserException;
import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.Navigator;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.nikipo.ardupilotoverlay.util.FileFilters.TELEMETRY;
import static com.nikipo.ardupilotoverlay.util.FileUtility.FileValidator;
import static com.nikipo.ardupilotoverlay.util.FileUtility.chooseFile;

public class TelemetrySelectorController implements Navigable {
    private static final Logger logger = AppLogger.getLogger(TelemetrySelectorController.class);

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private AnchorPane contentPane;

    @FXML
    private VBox loadingPane;

    @FXML
    private Button selectTelemetryButton;

    @FXML
    private Label statusLabel;

    private Navigator navigator;

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    @FXML
    public void onSelectTelemetry(ActionEvent actionEvent) {
        clearLabel();

        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) {
            return;
        }

        Stage stage = (Stage) ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        Path telemetryPath = chooseFile(
                stage,
                "Select Telemetry File",
                List.of(TELEMETRY)
        );

        try {
            showLoading();
            if (FileValidator(telemetryPath)) {
                project.setTelemetryFile(telemetryPath);
                Parser parser = new Parser(telemetryPath);
                Telemetry telemetry = parser.getTelemetry();
                project.setTelemetry(telemetry);

                project.setCurrentStep(WizardStep.TELEMETRY);

                navigator.showOverlay();
            }

        } catch (IOException e) {
            logger.error("Failed at FileValidator with ", e);
            showError("Failed at FileValidator with "+e.getMessage());

        } catch (ParserException e) {
            logger.error("Failed at Parser with ", e);
            showError("Failed at Parser with "+e.getMessage());
        }
        finally {
            hideLoading();
        }
    }

    private void showLoading() {
        contentPane.setDisable(true);
        contentPane.setOpacity(0.5);

        loadingPane.setManaged(true);
        loadingPane.setVisible(true);
        loadingPane.toFront();
    }

    private void hideLoading() {
        loadingPane.setVisible(false);
        loadingPane.setManaged(false);

        contentPane.setDisable(false);
        contentPane.setOpacity(1.0);
    }
    private void showError(String text) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void clearLabel(){
        statusLabel.setText("");
        statusLabel.setVisible(false);
    }


}
