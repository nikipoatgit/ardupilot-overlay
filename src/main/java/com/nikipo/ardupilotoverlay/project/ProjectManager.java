package com.nikipo.ardupilotoverlay.project;

import java.nio.file.Path;

public class ProjectManager {
    private static final ProjectManager INSTANCE = new ProjectManager();

    private Project currentProject;

    private ProjectManager() {}

    public static ProjectManager getInstance() {
        return INSTANCE;
    }

    public void newProject(Path videoPath) {
        closeProject();
        currentProject = new Project(videoPath);
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public boolean hasProject() {
        return currentProject != null;
    }

    public void closeProject() {
        if (currentProject != null) {
            currentProject.close();
        }

        currentProject = null;
    }
}
