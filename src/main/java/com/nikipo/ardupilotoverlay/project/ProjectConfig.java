package com.nikipo.ardupilotoverlay.project;

public class ProjectConfig {

    private String projectName;

    public ProjectConfig(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}