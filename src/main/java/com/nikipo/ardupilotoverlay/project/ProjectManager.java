package com.nikipo.ardupilotoverlay.project;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.telemetry.ContinuousTimeSeries;
import com.nikipo.ardupilotoverlay.telemetry.DiscreteTimeSeries;
import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.videoplayer.VideoEngine;
import com.nikipo.ardupilotoverlay.videoplayer.VideoInfo;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;

import java.nio.file.Path;

public class ProjectManager {
    private static final ProjectManager INSTANCE = new ProjectManager();
    private static final Logger logger = AppLogger.getLogger(ProjectManager.class);

    private Project currentProject;
    private VideoEngine videoEngine;

    private ProjectManager() {}

    public static ProjectManager getInstance() {
        return INSTANCE;
    }

    public void newProject(Path videoPath) {
        closeProject();
        currentProject = new Project(videoPath);
        openVideo(videoPath);
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public boolean hasProject() {
        return currentProject != null;
    }

    public VideoEngine getVideoEngine() {
        return videoEngine;
    }

    public void debugDumpTelemetry() {
        Telemetry telemetry = currentProject == null ? null : currentProject.getTelemetry();
        if (telemetry == null) {
            logger.info("Telemetry: none loaded");
            return;
        }

        logger.info("""
                Telemetry:
                  capabilities={}
                  home={}
                  roll={}
                  pitch={}
                  heading={}
                  latitude={}
                  longitude={}
                  altitude={}
                  relativeAltitude={}
                  groundSpeed={}
                  airSpeed={}
                  verticalSpeed={}
                  climbRate={}
                  batteryVoltage={}
                  batteryCurrent={}
                  batteryRemaining={}
                  satellites={}
                  gpsFix={}
                  rssi={}
                  windSpeed={}
                  windDirection={}
                  armed={}
                  flightMode={}
                """,
                telemetry.getCapabilities(),
                telemetry.getHome(),
                describe(telemetry.roll),
                describe(telemetry.pitch),
                describe(telemetry.heading),
                describe(telemetry.latitude),
                describe(telemetry.longitude),
                describe(telemetry.altitude),
                describe(telemetry.relativeAltitude),
                describe(telemetry.groundSpeed),
                describe(telemetry.airSpeed),
                describe(telemetry.verticalSpeed),
                describe(telemetry.climbRate),
                describe(telemetry.batteryVoltage),
                describe(telemetry.batteryCurrent),
                describe(telemetry.batteryRemaining),
                describe(telemetry.satellites),
                describe(telemetry.gpsFix),
                describe(telemetry.rssi),
                describe(telemetry.windSpeed),
                describe(telemetry.windDirection),
                describe(telemetry.armed),
                describe(telemetry.flightMode));
    }

    public void closeProject() {
        if (videoEngine != null) {
            videoEngine.close();
            videoEngine = null;
        }
        currentProject = null;
    }

    private void openVideo(Path videoPath) {
        if (videoPath == null) {
            currentProject.setVideoInfo(new VideoInfo(0.0));
            return;
        }

        VideoEngine engine = new VideoEngine();
        try {
            engine.open(videoPath);
            videoEngine = engine;
            currentProject.setVideoInfo(new VideoInfo(engine.getDuration()));
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.error("Failed to open video file {}", videoPath, e);
            engine.close();
            currentProject.setVideoInfo(new VideoInfo(0.0));
        }
    }

    private static <T extends Number> String describe(ContinuousTimeSeries<T> series) {
        return series.size() == 0
                ? "samples=0"
                : "samples=" + series.size() + ", latest=" + series.latest();
    }

    private static <T> String describe(DiscreteTimeSeries<T> series) {
        return series.size() == 0
                ? "samples=0"
                : "samples=" + series.size() + ", latest=" + series.latest();
    }
}
