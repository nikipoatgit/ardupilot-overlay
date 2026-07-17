package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.Navigator;
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

public class MediaSelectorController implements Navigable {
    private Navigator navigator;
    private static final Logger logger = AppLogger.getLogger(MediaSelectorController.class);

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void onSelectVideo(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        Path video = chooseFile(
                stage,
                "Select Video",
                List.of(VIDEO)
        );
        try {
            if (FileValidator(video)){
                ProjectManager  projectManager = ProjectManager.getInstance();
                projectManager.newProject(video);
                navigator.showProjectConfig();
            }
        } catch (IOException e) {
            logger.error("Failed at FileValidator with ", e);
        }


    }

    public void onSkip(ActionEvent actionEvent) {
        ProjectManager  projectManager = ProjectManager.getInstance();
        projectManager.newProject(null);
        navigator.showProjectConfig();
    }
}
