package com.nikipo.ardupilotoverlay.video;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;

public class VideoEngine {
    public static final double DEFAULT_FRAME_RATE = 30.0;

    private static final Logger logger = AppLogger.getLogger(VideoEngine.class);

    private FFmpegFrameGrabber grabber;
    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private Path source;


    public void open(Path file) throws FFmpegFrameGrabber.Exception {
        Objects.requireNonNull(file, "file");

        if (isOpen() && file.equals(source)) {
            return;
        }

        close();

        FFmpegFrameGrabber nextGrabber = new FFmpegFrameGrabber(file.toFile());
        try {
            nextGrabber.start();
            grabber = nextGrabber;
            source = file;
        } catch (FFmpegFrameGrabber.Exception e) {
            nextGrabber.release();
            throw e;
        }
    }

    public BufferedImage getFrame() throws Exception {
        ensureOpen();

        Frame frame = grabber.grabImage();

        if (frame != null) {
            return converter.convert(frame);
        }

        return null;
    }


    public double getFPS() {
        if (!isOpen()) {
            return DEFAULT_FRAME_RATE;
        }

        double frameRate = grabber.getFrameRate();
        return frameRate > 0 ? frameRate : DEFAULT_FRAME_RATE;
    }

    public void close() {
        if (grabber == null) {
            return;
        }

        try {
            grabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.warn("Failed to stop video grabber for {}", source, e);
        }

        try {
            grabber.release();
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.warn("Failed to release video grabber for {}", source, e);
        } finally {
            grabber = null;
            source = null;
        }
    }

    public void seek(double timeSeconds) throws FFmpegFrameGrabber.Exception {
        ensureOpen();

        double duration = getDuration();
        double clampedTime = duration > 0
                ? Math.min(Math.max(0.0, timeSeconds), duration)
                : Math.max(0.0, timeSeconds);
        long timestamp = (long) (clampedTime * 1_000_000);
        grabber.setTimestamp(timestamp);
    }

    public double getDuration() {
        if (!isOpen()) {
            return 0.0;
        }

        long timeMicros = grabber.getLengthInTime();
        if (timeMicros > 0) {
            return timeMicros / 1_000_000.0;
        }

        long totalFrames = grabber.getLengthInFrames();
        double fps = getFPS();

        if (totalFrames > 0 && fps > 0) {
            return (double) totalFrames / fps;
        }

        return 0.0;
    }

    public double getCurrentTime() {
        if (!isOpen()) {
            return 0.0;
        }

        return grabber.getTimestamp() / 1_000_000.0;
    }

    public double getFrameDurationSeconds() {
        return 1.0 / getFPS();
    }

    public boolean isOpen() {
        return grabber != null;
    }

    public Path getSource() {
        return source;
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("No video file is open.");
        }
    }
}
