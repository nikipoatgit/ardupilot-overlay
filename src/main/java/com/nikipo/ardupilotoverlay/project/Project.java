package com.nikipo.ardupilotoverlay.project;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import com.nikipo.ardupilotoverlay.videoplayer.VideoInfo;

import java.nio.file.Path;

import static com.nikipo.ardupilotoverlay.util.FileUtility.getFileNameWithoutExtension;
import static com.nikipo.ardupilotoverlay.util.Utility.currentTimeMillis;

public class Project {
    private WizardStep currentStep;

    private final Path videoFile;
    private Path telemetryFile;
    private final ProjectConfig config;
    private Telemetry telemetry;
    private VideoInfo videoInfo;

    public Project(Path videoFile) {

        String projectName;
        this.videoFile = videoFile;

        if (videoFile != null) {
            projectName = getFileNameWithoutExtension(videoFile);
        }
        else {
            projectName = "UNTITLED_" + currentTimeMillis() + (currentTimeMillis()%56616);
        }

        this.config = new ProjectConfig(projectName);
        this.videoInfo = new VideoInfo(0.0);
        currentStep = WizardStep.VIDEO;
    }

    public ProjectConfig getConfig() {
        return config;
    }

    public Path getVideoFile() {
        return videoFile;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public Path getTelemetryFile() {
        return telemetryFile;
    }

    public void setTelemetryFile(Path telemetryFile) {
        this.telemetryFile = telemetryFile;
    }

    public WizardStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(WizardStep currentStep) {
        this.currentStep = currentStep;
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }
    public void setTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
    }
}
