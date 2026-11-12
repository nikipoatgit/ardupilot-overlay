package com.nikipo.ardupilotoverlay.videoplayer;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class VideoPlayer {
    private static final Logger logger = AppLogger.getLogger(VideoPlayer.class);
    private static final int DECODE_QUEUE_CAPACITY = 8;

    private final VideoEngine videoEngine;
    private final double durationSeconds;
    private final double fps;
    private final double frameDurationSeconds;
    private final ArrayBlockingQueue<VideoEngine.VideoFrame> frameQueue =
            new ArrayBlockingQueue<>(DECODE_QUEUE_CAPACITY);
    private final Object decoderLock = new Object();
    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<SeekRequest> pendingSeek = new AtomicReference<>();
    private final AtomicLong primingGeneration = new AtomicLong(Long.MIN_VALUE);
    private final Consumer<BufferedImage> frameCallback;
    private final VideoFrameCallback pixelFrameCallback;
    private final Consumer<Throwable> errorCallback;
    private final Consumer<PlaybackState> stateCallback;
    private final AnimationTimer timer;

    private volatile boolean decoderDisposed;
    private volatile boolean decoderPlaying;
    private volatile double decoderPositionSeconds;
    private volatile long staleFramesDiscarded;
    private Thread decoderThread;
    private AudioEngine audioEngine;
    private Path audioSource;
    private boolean muted;
    private volatile boolean clockStarted;
    private volatile double fallbackClockStart;
    private volatile long fallbackClockNanos;
    private volatile double lastDisplayedPtsSeconds;
    private volatile long lastDisplayedGeneration = Long.MIN_VALUE;
    private PlaybackState state = PlaybackState.STOPPED;

    public VideoPlayer(VideoEngine videoEngine, Consumer<BufferedImage> frameCallback) {
        this(videoEngine, frameCallback, error -> logger.error("Playback failed", error));
    }

    public VideoPlayer(VideoEngine videoEngine, VideoFrameCallback frameCallback) {
        this(videoEngine, frameCallback, error -> logger.error("Playback failed", error));
    }

    public VideoPlayer(
            VideoEngine videoEngine,
            VideoFrameCallback frameCallback,
            Consumer<Throwable> errorCallback
    ) {
        this(videoEngine, frameCallback, errorCallback, state -> {});
    }

    public VideoPlayer(
            VideoEngine videoEngine,
            Consumer<BufferedImage> frameCallback,
            Consumer<Throwable> errorCallback
    ) {
        this(videoEngine, frameCallback, errorCallback, state -> {});
    }

    public VideoPlayer(
            VideoEngine videoEngine,
            Consumer<BufferedImage> frameCallback,
            Consumer<Throwable> errorCallback,
            Consumer<PlaybackState> stateCallback
    ) {
        this.videoEngine = Objects.requireNonNull(videoEngine, "videoEngine");
        this.durationSeconds = videoEngine.getDuration();
        this.fps = videoEngine.getFPS();
        this.frameDurationSeconds = 1.0 / fps;
        this.frameCallback = Objects.requireNonNull(frameCallback, "frameCallback");
        this.pixelFrameCallback = null;
        this.errorCallback = Objects.requireNonNull(errorCallback, "errorCallback");
        this.stateCallback = Objects.requireNonNull(stateCallback, "stateCallback");
        timer = createTimer();
    }

    public VideoPlayer(
            VideoEngine videoEngine,
            VideoFrameCallback frameCallback,
            Consumer<Throwable> errorCallback,
            Consumer<PlaybackState> stateCallback
    ) {
        this.videoEngine = Objects.requireNonNull(videoEngine, "videoEngine");
        this.durationSeconds = videoEngine.getDuration();
        this.fps = videoEngine.getFPS();
        this.frameDurationSeconds = 1.0 / fps;
        this.frameCallback = null;
        this.pixelFrameCallback = Objects.requireNonNull(frameCallback, "frameCallback");
        this.errorCallback = Objects.requireNonNull(errorCallback, "errorCallback");
        this.stateCallback = Objects.requireNonNull(stateCallback, "stateCallback");
        timer = createTimer();
    }

    private AnimationTimer createTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                displayFrames();
            }
        };
    }

    public void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        if (!videoEngine.isOpen()) {
            return;
        }

        ensureAudio();
        clockStarted = false;
        decoderPlaying = true;
        if (durationSeconds > 0 && lastDisplayedPtsSeconds >= durationSeconds - frameDurationSeconds) {
            seek(0.0);
            decoderPlaying = true;
        }
        boolean decoderCanResume = decoderPositionSeconds >= lastDisplayedPtsSeconds
                && !frameQueue.isEmpty()
                && frameQueue.peek().generation() == generation.get();
        if (!decoderCanResume && Math.abs(decoderPositionSeconds - lastDisplayedPtsSeconds)
                > frameDurationSeconds) {
            seek(lastDisplayedPtsSeconds);
        }
        ensureDecoderThread();
        synchronized (decoderLock) {
            decoderLock.notifyAll();
        }
        setState(PlaybackState.PLAYING);
        VideoEngine.VideoFrame queuedFrame = frameQueue.peek();
        if (queuedFrame != null && queuedFrame.generation() == generation.get()) {
            scheduleClockStart(queuedFrame);
        }
    }

    public void pause() {
        if (!isPlaying()) {
            return;
        }
        lastDisplayedPtsSeconds = displayedPosition();
        decoderPlaying = false;
        clockStarted = false;
        primingGeneration.set(Long.MIN_VALUE);
        if (audioEngine != null) {
            audioEngine.pause();
        }
        timer.stop();
        setState(PlaybackState.PAUSED);
    }

    public void stop() {
        decoderPlaying = false;
        clockStarted = false;
        frameQueue.clear();
        if (audioEngine != null) {
            audioEngine.stop();
        }
        timer.stop();
        setState(PlaybackState.STOPPED);
    }

    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }

    public PlaybackState getState() {
        return state;
    }

    public double getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        if (playbackSpeed <= 0) {
            throw new IllegalArgumentException("Playback speed must be greater than zero.");
        }
        this.playbackSpeed = playbackSpeed;
        if (audioEngine != null) {
            audioEngine.setPlaybackSpeed(playbackSpeed);
            audioEngine.setMuted(muted);
        }
        if (isPlaying() && clockStarted) {
            fallbackClockStart = getCurrentTime();
            fallbackClockNanos = System.nanoTime();
        }
    }

    public void seek(double time) {
        double target = clamp(time, 0, durationSeconds);
        boolean wasPlaying = isPlaying();
        long nextGeneration = generation.incrementAndGet();
        int clearedFrames = frameQueue.size();
        frameQueue.clear();
        pendingSeek.set(new SeekRequest(target, nextGeneration, clearedFrames));
        lastDisplayedGeneration = Long.MIN_VALUE;
        fallbackClockStart = target;
        fallbackClockNanos = System.nanoTime();
        clockStarted = false;
        primingGeneration.set(Long.MIN_VALUE);
        if (!wasPlaying && audioEngine != null) {
            audioEngine.seek(target);
        }
        decoderPlaying = wasPlaying;
        ensureDecoderThread();
        synchronized (decoderLock) {
            decoderLock.notifyAll();
        }
    }

    public void stepFrames(int frames) {
        if (frames == 0) {
            return;
        }
        long frameIndex = Math.round(lastDisplayedPtsSeconds * fps);
        double target = (frameIndex + frames + 0.5) / fps;
        seek(clamp(target, 0, durationSeconds));
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (audioEngine != null) {
            audioEngine.setMuted(muted);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean hasAudio() {
        return audioEngine != null && audioEngine.isAvailable();
    }

    public double getCurrentTime() {
        if (!isPlaying()) {
            return lastDisplayedPtsSeconds;
        }
        if (!clockStarted) {
            return fallbackClockStart;
        }
        if (audioEngine != null && audioEngine.isAvailable() && playbackSpeed == 1.0) {
            return clamp(audioEngine.getClockSeconds(), 0, durationSeconds);
        }
        return clamp(fallbackClockStart
                + (System.nanoTime() - fallbackClockNanos) / 1_000_000_000.0 * playbackSpeed,
                0, durationSeconds);
    }

    public void dispose() {
        stop();
        decoderDisposed = true;
        synchronized (decoderLock) {
            decoderLock.notifyAll();
        }
        Thread thread = decoderThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (audioEngine != null) {
            audioEngine.dispose();
        }
        videoEngine.close();
    }

    private void displayFrames() {
        if (!isPlaying() || !clockStarted) {
            return;
        }

        long currentGeneration = generation.get();
        double clock = getCurrentTime();
        VideoEngine.VideoFrame latest = null;
        VideoEngine.VideoFrame frame;
        while ((frame = frameQueue.peek()) != null) {
            if (frame.generation() != currentGeneration) {
                staleFramesDiscarded++;
                frameQueue.poll();
                continue;
            }
            if (frame.ptsSeconds() > clock + frameDurationSeconds / 2.0) {
                break;
            }
            latest = frameQueue.poll();
        }
        if (latest != null) {
            displayFrame(latest);
        }
    }

    private void displayFrame(VideoEngine.VideoFrame frame) {
        if (frame.generation() != generation.get()) {
            staleFramesDiscarded++;
            return;
        }
        lastDisplayedPtsSeconds = frame.ptsSeconds();
        lastDisplayedGeneration = frame.generation();
        if (pixelFrameCallback != null) {
            pixelFrameCallback.accept(frame);
        } else {
            frameCallback.accept(frame.image());
        }
    }

    private void ensureAudio() {
        if (audioEngine == null || !Objects.equals(audioSource, videoEngine.getSource())) {
            if (audioEngine != null) {
                audioEngine.dispose();
            }
            audioEngine = AudioEngine.tryOpen(videoEngine.getSource());
            audioSource = videoEngine.getSource();
            audioEngine.setMuted(muted);
        }
    }

    private void handleAudioFailure(Throwable error) {
        logger.warn("Audio playback is unavailable; using the video clock", error);
        if (audioEngine != null) {
            audioEngine.dispose();
        }
        audioEngine = null;
        fallbackClockStart = lastDisplayedPtsSeconds;
        fallbackClockNanos = System.nanoTime();
    }

    private void ensureDecoderThread() {
        synchronized (decoderLock) {
            if (decoderThread == null) {
                decoderThread = new Thread(this::decodeLoop, "video-decoder");
                decoderThread.setDaemon(true);
                decoderThread.start();
            }
        }
    }

    private void decodeLoop() {
        try {
            while (!decoderDisposed) {
                SeekRequest request = pendingSeek.getAndSet(null);
                if (request != null) {
                    decodeSeek(request);
                    continue;
                }

                if (!decoderPlaying) {
                    synchronized (decoderLock) {
                        if (!decoderDisposed && !decoderPlaying && pendingSeek.get() == null) {
                            decoderLock.wait();
                        }
                    }
                    continue;
                }

                long frameGeneration = generation.get();
                VideoEngine.VideoFrame frame = videoEngine.grabFrame(frameGeneration);
                if (frame == null) {
                    decoderPlaying = false;
                    Platform.runLater(this::stop);
                    break;
                }
                decoderPositionSeconds = frame.ptsSeconds();
                if (frame.generation() != generation.get()) {
                    staleFramesDiscarded++;
                    continue;
                }
                if (!enqueueFrame(frame, frameGeneration, false)) {
                    continue;
                }
                scheduleClockStart(frame);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!decoderDisposed) {
                errorCallback.accept(e);
            }
        } finally {
            decoderThread = null;
        }
    }

    private void decodeSeek(SeekRequest request) throws Exception {
        long staleBefore = staleFramesDiscarded;
        staleFramesDiscarded += request.clearedFrames();
        frameQueue.clear();
        videoEngine.seek(request.target());
        double minimumPts = request.target() - frameDurationSeconds / 2.0;
        VideoEngine.VideoFrame frame;
        while (generation.get() == request.generation()
                && (frame = videoEngine.grabFrame(request.generation())) != null) {
            decoderPositionSeconds = frame.ptsSeconds();
            if (frame.generation() != generation.get()) {
                staleFramesDiscarded++;
                continue;
            }
            if (frame.ptsSeconds() < minimumPts) {
                staleFramesDiscarded++;
                continue;
            }
            if (!enqueueFrame(frame, request.generation(), !decoderPlaying)) {
                return;
            }
            logger.info("Video seek: target={} generation={} firstPts={} staleDiscarded={}",
                    request.target(), request.generation(), frame.ptsSeconds(),
                    staleFramesDiscarded - staleBefore);
            if (decoderPlaying) {
                scheduleClockStart(frame);
            } else {
                Platform.runLater(() -> presentSeekFrame(request.generation()));
            }
            return;
        }
    }

    private void presentSeekFrame(long frameGeneration) {
        if (frameGeneration != generation.get() || isPlaying()) {
            return;
        }
        VideoEngine.VideoFrame frame = frameQueue.poll();
        if (frame != null && frame.generation() == generation.get()) {
            displayFrame(frame);
        }
    }

    private void scheduleClockStart(VideoEngine.VideoFrame frame) {
        long currentGeneration = generation.get();
        if (frame.generation() != currentGeneration
                || !primingGeneration.compareAndSet(Long.MIN_VALUE, currentGeneration)) {
            return;
        }
        Platform.runLater(() -> startClockAfterPriming(frame));
    }

    private void startClockAfterPriming(VideoEngine.VideoFrame frame) {
        if (!isPlaying() || frame.generation() != generation.get() || clockStarted) {
            if (frame.generation() == generation.get()) {
                primingGeneration.set(Long.MIN_VALUE);
            }
            return;
        }
        fallbackClockStart = frame.ptsSeconds();
        fallbackClockNanos = System.nanoTime();
        clockStarted = true;
        if (audioEngine != null) {
            audioEngine.play(frame.ptsSeconds(), playbackSpeed, null, this::handleAudioFailure);
        }
        logger.info("Video clock started: generation={} framePts={} clock={}",
                frame.generation(), frame.ptsSeconds(), getCurrentTime());
        timer.start();
    }

    private boolean enqueueFrame(VideoEngine.VideoFrame frame, long frameGeneration, boolean allowPaused)
            throws InterruptedException {
        while (!decoderDisposed
                && (decoderPlaying || allowPaused)
                && frameGeneration == generation.get()) {
            if (frameQueue.offer(frame, 50, TimeUnit.MILLISECONDS)) {
                return true;
            }
        }
        return false;
    }

    private double displayedPosition() {
        return lastDisplayedGeneration == generation.get()
                ? lastDisplayedPtsSeconds
                : lastDisplayedPtsSeconds;
    }

    private void setState(PlaybackState nextState) {
        if (state != nextState) {
            state = nextState;
            stateCallback.accept(nextState);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SeekRequest(double target, long generation, int clearedFrames) {
    }

    private double playbackSpeed = 1.0;
}
