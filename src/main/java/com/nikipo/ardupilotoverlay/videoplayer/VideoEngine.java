package com.nikipo.ardupilotoverlay.videoplayer;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Graphics2D;
import java.nio.file.Path;
import java.util.Objects;

import static org.bytedeco.ffmpeg.global.swscale.SWS_AREA;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BICUBIC;

public class VideoEngine {
    public static final double DEFAULT_FRAME_RATE = 30.0;
    public static final int PREVIEW_MAX_HEIGHT = 480;

    private static final Logger logger = AppLogger.getLogger(VideoEngine.class);

    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private FFmpegFrameGrabber grabber;
    private Path source;
    private int sourceWidth;
    private int sourceHeight;

    public synchronized void open(Path file) throws FFmpegFrameGrabber.Exception {
        open(file, PREVIEW_MAX_HEIGHT);
    }

    public synchronized void open(Path file, int previewMaxHeight) throws FFmpegFrameGrabber.Exception {
        Objects.requireNonNull(file, "file");
        if (previewMaxHeight <= 0) {
            throw new IllegalArgumentException("previewMaxHeight must be positive");
        }

        if (isOpen() && file.equals(source)) {
            return;
        }

        close();

        FFmpegFrameGrabber probe = new FFmpegFrameGrabber(file.toFile());
        int width;
        int height;
        try {
            probe.start();
            width = probe.getImageWidth();
            height = probe.getImageHeight();
        } finally {
            try {
                probe.stop();
            } catch (FFmpegFrameGrabber.Exception e) {
                logger.warn("Failed to stop video probe for {}", file, e);
            }
            probe.release();
        }

        FFmpegFrameGrabber nextGrabber = new FFmpegFrameGrabber(file.toFile());
        nextGrabber.setVideoOption("threads", "auto");
        if (height > previewMaxHeight && width > 0) {
            int scaledHeight = previewMaxHeight;
            int scaledWidth = (int) Math.floor((double) width * scaledHeight / height);
            scaledWidth -= scaledWidth % 2;
            nextGrabber.setImageWidth(Math.max(2, scaledWidth));
            nextGrabber.setImageHeight(scaledHeight);
            try {
                nextGrabber.setImageScalingFlags(SWS_AREA);
            } catch (IllegalArgumentException e) {
                nextGrabber.setImageScalingFlags(SWS_BICUBIC);
            }
        }
        try {
            nextGrabber.start();
            grabber = nextGrabber;
            source = file;
            sourceWidth = width;
            sourceHeight = height;
        } catch (FFmpegFrameGrabber.Exception e) {
            nextGrabber.release();
            throw e;
        }
    }

    public synchronized BufferedImage getFrame() throws Exception {
        VideoFrame frame = grabFrame(0);
        return frame == null ? null : frame.image();
    }

    public synchronized VideoFrame grabFrame(long generation) throws Exception {
        ensureOpen();

        Frame frame = grabber.grabImage();
        if (frame == null) {
            return null;
        }

        BufferedImage converted = converter.convert(frame);
        if (converted == null) {
            return null;
        }

        BufferedImage copy = copyImage(converted);
        long timestamp = frame.timestamp >= 0 ? frame.timestamp : grabber.getTimestamp();
        return new VideoFrame(copy, toBgraPre(copy), timestamp / 1_000_000.0, generation);
    }

    public synchronized double getFPS() {
        if (!isOpen()) {
            return DEFAULT_FRAME_RATE;
        }

        double frameRate = grabber.getFrameRate();
        return frameRate > 0 ? frameRate : DEFAULT_FRAME_RATE;
    }

    public synchronized void close() {
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
            sourceWidth = 0;
            sourceHeight = 0;
        }
    }

    public synchronized void seek(double timeSeconds) throws FFmpegFrameGrabber.Exception {
        ensureOpen();

        double duration = getDuration();
        double clampedTime = duration > 0
                ? Math.min(Math.max(0.0, timeSeconds), duration)
                : Math.max(0.0, timeSeconds);
        grabber.setTimestamp((long) (clampedTime * 1_000_000));
    }

    public synchronized double getDuration() {
        if (!isOpen()) {
            return 0.0;
        }

        long timeMicros = grabber.getLengthInTime();
        if (timeMicros > 0) {
            return timeMicros / 1_000_000.0;
        }

        long totalFrames = grabber.getLengthInFrames();
        double fps = getFPS();
        return totalFrames > 0 && fps > 0 ? (double) totalFrames / fps : 0.0;
    }

    public synchronized double getCurrentTime() {
        return isOpen() ? grabber.getTimestamp() / 1_000_000.0 : 0.0;
    }

    public synchronized double getFrameDurationSeconds() {
        return 1.0 / getFPS();
    }

    public synchronized boolean isOpen() {
        return grabber != null;
    }

    public synchronized Path getSource() {
        return source;
    }

    public synchronized double getSourceAspect() {
        return sourceWidth > 0 && sourceHeight > 0
                ? (double) sourceWidth / sourceHeight
                : 16.0 / 9.0;
    }

    public synchronized int getSourceWidth() {
        return sourceWidth;
    }

    public synchronized int getSourceHeight() {
        return sourceHeight;
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("No video file is open.");
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );
        if (source.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            byte[] sourcePixels = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
            byte[] targetPixels = ((DataBufferByte) copy.getRaster().getDataBuffer()).getData();
            System.arraycopy(sourcePixels, 0, targetPixels, 0,
                    Math.min(sourcePixels.length, targetPixels.length));
        } else {
            Graphics2D graphics = copy.createGraphics();
            try {
                graphics.drawImage(source, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }
        return copy;
    }

    private static byte[] toBgraPre(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixels = new byte[width * height * 4];
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        int offset = 0;
        for (int color : argb) {
            int alpha = color >>> 24;
            pixels[offset++] = (byte) (((color & 0xff) * alpha + 127) / 255);
            pixels[offset++] = (byte) ((((color >>> 8) & 0xff) * alpha + 127) / 255);
            pixels[offset++] = (byte) ((((color >>> 16) & 0xff) * alpha + 127) / 255);
            pixels[offset++] = (byte) alpha;
        }
        return pixels;
    }

    public record VideoFrame(
            BufferedImage image,
            byte[] bgraPrePixels,
            double ptsSeconds,
            long generation
    ) {
    }
}
