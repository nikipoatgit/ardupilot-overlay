package com.nikipo.ardupilotoverlay.ui;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.project.Project;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.ui.controllers.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UIOrchestrator {

    private final MainLayoutController layout;
    private static final Logger logger = AppLogger.getLogger(UIOrchestrator.class);
    private final Map<String, Page> cache = new HashMap<>();

    private static class Page {
        final Parent root;

        Page(Parent root) {
            this.root = root;
        }
    }

    public UIOrchestrator(MainLayoutController layout) {
        this.layout = layout;
    }

    public void showHome() {
        loadSafely("/fxml/Home.fxml");
    }

    public void showMediaSelector() {
        loadSafely("/fxml/MediaSelector.fxml");
    }

    public void showProjectConfig() {
        if (canNavigateTo(WizardStep.PROJECT)) {
            loadSafely("/fxml/ProjectConfig.fxml");
        }
    }

    public void showTelemetrySelector() {
        if (canNavigateTo(WizardStep.TELEMETRY)) {
            loadSafely("/fxml/TelemetrySelector.fxml");
        }
    }

    public void showOverlay() {
        Runnable show = () -> {
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project == null || project.getTelemetry() == null
                    || project.getVideoFile() == null
                    || ProjectManager.getInstance().getVideoEngine() == null
                    || !ProjectManager.getInstance().getVideoEngine().isOpen()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Cannot show overlay");
                alert.setHeaderText("Project, telemetry, and video are required");
                alert.setContentText("Load a video and parse telemetry before opening the overlay.");
                alert.show();
                return;
            }
            if (canNavigateTo(WizardStep.OVERLAY)) {
                loadSafely("/fxml/Overlay.fxml");
            }
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    public void showExport() {
        if (canNavigateTo(WizardStep.EXPORT)) {
            loadSafely("/fxml/Export.fxml");
        }
    }

    public boolean canNavigateTo(WizardStep step) {
        if (step == WizardStep.HOME) {
            return true;
        }

        Project project = ProjectManager.getInstance().getCurrentProject();
        if (step == WizardStep.VIDEO) {
            return project != null;
        }

        return project != null
                && project.getCurrentStep().ordinal() >= step.ordinal() - 1;
    }

    public void clearPage(String fxml) {
        cache.remove(fxml);
    }

    public void clearAllPages() {
        cache.clear();
    }

    private void loadSafely(String fxml) {
        try {
            load(fxml);
        } catch (IOException e) {
            logger.error("Failed to load UI page {}", fxml, e);
        }
    }

    private void load(String fxml) throws IOException {
        Page page = cache.get(fxml);
        if (page == null) {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxml))
            );

            Parent root = loader.load();
            Object controller = loader.getController();

            if (controller instanceof Navigable navigable) {
                navigable.setOrchestrator(this);
            }

            page = new Page(root);
            cache.put(fxml, page);
        }

        layout.setContent(page.root);
        layout.refreshNavigation(stepFor(fxml));
    }

    private static WizardStep stepFor(String fxml) {
        return switch (fxml) {
            case "/fxml/Home.fxml" -> WizardStep.HOME;
            case "/fxml/MediaSelector.fxml" -> WizardStep.VIDEO;
            case "/fxml/ProjectConfig.fxml" -> WizardStep.PROJECT;
            case "/fxml/TelemetrySelector.fxml" -> WizardStep.TELEMETRY;
            case "/fxml/Overlay.fxml" -> WizardStep.OVERLAY;
            case "/fxml/Export.fxml" -> WizardStep.EXPORT;
            default -> WizardStep.HOME;
        };
    }
}
