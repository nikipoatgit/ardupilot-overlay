package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.nikipo.ardupilotoverlay.util.FileFilters.VIDEO;
import static com.nikipo.ardupilotoverlay.util.FileUtility.FileValidator;
import static com.nikipo.ardupilotoverlay.util.FileUtility.chooseFile;
import static com.nikipo.ardupilotoverlay.util.FileUtility.getLastVideoDirectory;
import static com.nikipo.ardupilotoverlay.util.FileUtility.rememberVideoDirectory;

public class MediaSelectorController implements Navigable {
    private UIOrchestrator orchestrator;
    private static final Logger logger = AppLogger.getLogger(MediaSelectorController.class);

    @Override
    public void setOrchestrator(UIOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void onSelectVideo(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        Path video = chooseFile(
                stage,
                "Select Video",
                List.of(VIDEO),
                getLastVideoDirectory()
        );
        if (video == null) {
            return;
        }

        rememberVideoDirectory(video);

        try {
            if (FileValidator(video)){
                ProjectManager  projectManager = ProjectManager.getInstance();
                projectManager.newProject(video);
                orchestrator.showProjectConfig();
            }
        } catch (IOException e) {
            logger.error("Failed at FileValidator with ", e);
        }


    }

    public void onSkip(ActionEvent actionEvent) {
        ProjectManager  projectManager = ProjectManager.getInstance();
        projectManager.newProject(null);
        orchestrator.showProjectConfig();
    }
}
