package com.nikipo.ardupilotoverlay.project;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import com.nikipo.ardupilotoverlay.video.VideoEngine;
import com.nikipo.ardupilotoverlay.video.VideoInfo;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;

import java.nio.file.Path;

import static com.nikipo.ardupilotoverlay.util.FileUtility.getFileNameWithoutExtension;
import static com.nikipo.ardupilotoverlay.util.Utility.currentTimeMillis;

public class Project {
    private static final Logger logger = AppLogger.getLogger(Project.class);

    private WizardStep currentStep;

    private final Path videoFile;
    private Path telemetryFile;


    private final ProjectConfig config;

    private VideoInfo videoInfo;
    private VideoEngine videoEngine;

    public Project(Path videoFile) {

        String projectName;
        this.videoFile = videoFile;

        if (videoFile != null) {
            projectName = getFileNameWithoutExtension(videoFile);
        }
        else {
            projectName = "UNTITLED_" + currentTimeMillis();
        }

        this.config = new ProjectConfig(projectName);
        openVideoEngine();
        videoInfo = new VideoInfo(videoEngine == null ? 0.0 : videoEngine.getDuration());
        currentStep = WizardStep.PROJECT;
    }

    public ProjectConfig getConfig() {
        return config;
    }

    public VideoEngine getVideoEngine() {
        return videoEngine;
    }

    public Path getVideoFile() {
        return videoFile;
    }

    public Path getTelemetryFile() {
        return telemetryFile;
    }

    public void setTelemetryFile(Path telemetryFile) {
        this.telemetryFile = telemetryFile;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public WizardStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(WizardStep currentStep) {
        this.currentStep = currentStep;
    }

    public boolean hasVideo() {
        return videoEngine != null && videoEngine.isOpen();
    }

    public void close() {
        if (videoEngine != null) {
            videoEngine.close();
        }
    }

    private void openVideoEngine() {
        if (videoFile == null) {
            return;
        }

        VideoEngine engine = new VideoEngine();
        try {
            engine.open(videoFile);
            videoEngine = engine;
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.error("Failed to open video file {}", videoFile, e);
            engine.close();
        }
    }

    private Telemetry telemetry;

    public Telemetry getTelemetry() {
        return telemetry;
    }
    public void setTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
    }
}
