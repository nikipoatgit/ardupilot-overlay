package com.nikipo.ardupilotoverlay.playback;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.video.VideoEngine;
import javafx.animation.AnimationTimer;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

public class PlaybackController {
    private static final Logger logger = AppLogger.getLogger(PlaybackController.class);

    private final VideoEngine videoEngine;
    private final AudioEngine audioEngine;
    private final Consumer<BufferedImage> frameCallback;
    private final Consumer<Throwable> errorCallback;
    private final Consumer<PlaybackState> stateCallback;
    private final AnimationTimer timer;

    private long lastFrameTime = 0;
    private double playbackSpeed = 1.0;
    private long playbackSessionId = 0;
    private PlaybackState state = PlaybackState.STOPPED;

    public PlaybackController(
            VideoEngine videoEngine,
            Consumer<BufferedImage> frameCallback
    ) {
        this(videoEngine, frameCallback, error -> logger.error("Playback failed", error));
    }

    public PlaybackController(
            VideoEngine videoEngine,
            Consumer<BufferedImage> frameCallback,
            Consumer<Throwable> errorCallback
    ) {
        this(videoEngine, frameCallback, errorCallback, state -> {});
    }

    public PlaybackController(
            VideoEngine videoEngine,
            Consumer<BufferedImage> frameCallback,
            Consumer<Throwable> errorCallback,
            Consumer<PlaybackState> stateCallback
    ) {
        this.videoEngine = Objects.requireNonNull(videoEngine, "videoEngine");
        this.audioEngine = AudioEngine.tryOpen(videoEngine.getSource());
        this.frameCallback = Objects.requireNonNull(frameCallback, "frameCallback");
        this.errorCallback = Objects.requireNonNull(errorCallback, "errorCallback");
        this.stateCallback = Objects.requireNonNull(stateCallback, "stateCallback");

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTime == 0) {
                    lastFrameTime = now;
                    return;
                }

                if (now - lastFrameTime >= getFrameIntervalNanos()) {
                    if (!updateFrame()) {
                        stop();
                        return;
                    }

                    syncAudio();

                    lastFrameTime = now;
                }
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

        setState(PlaybackState.PLAYING);
        lastFrameTime = 0;
        long sessionId = ++playbackSessionId;

        if (audioEngine != null) {
            audioEngine.play(videoEngine.getCurrentTime(), playbackSpeed, null, errorCallback);
        }

        startPlayback(sessionId);
    }

    public void pause() {
        if (!isPlaying()) {
            return;
        }

        setState(PlaybackState.PAUSED);
        playbackSessionId++;
        pauseAudio();
        timer.stop();
    }

    public void stop() {
        setState(PlaybackState.STOPPED);
        lastFrameTime = 0;
        playbackSessionId++;
        stopAudio();
        timer.stop();
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
        if (audioEngine != null && isPlaying()) {
            audioEngine.setPlaybackSpeed(playbackSpeed);
        }
    }

    public void seek(double time) {
        try {
            boolean wasPlaying = isPlaying();
            pause();

            videoEngine.seek(time);
            updateFrame();
            seekAudio(time);

            if (wasPlaying) {
                play();
            }
        } catch (Exception e) {
            errorCallback.accept(e);
        }
    }

    public void stepFrames(int frames) {
        if (frames == 0) {
            return;
        }

        double targetTime = videoEngine.getCurrentTime()
                + (frames * videoEngine.getFrameDurationSeconds());
        seek(targetTime);
    }

    public void setMuted(boolean muted) {
        if (audioEngine != null) {
            audioEngine.setMuted(muted);
        }
    }

    public boolean isMuted() {
        return audioEngine != null && audioEngine.isMuted();
    }

    public boolean hasAudio() {
        return audioEngine != null;
    }

    public void dispose() {
        stop();

        if (audioEngine != null) {
            audioEngine.dispose();
        }
    }

    private boolean updateFrame() {
        try {
            BufferedImage frame = videoEngine.getFrame();
            if (frame != null) {
                frameCallback.accept(frame);
                return true;
            }

            return false;
        } catch (Exception e) {
            errorCallback.accept(e);
            return false;
        }
    }

    private long getFrameIntervalNanos() {
        double fps = videoEngine.getFPS() * playbackSpeed;
        return (long) (1_000_000_000.0 / fps);
    }

    private void startPlayback(long sessionId) {
        if (sessionId != playbackSessionId || !isPlaying()) {
            return;
        }

        if (!updateFrame()) {
            stop();
            return;
        }

        syncAudio();

        timer.start();
    }

    private void pauseAudio() {
        if (audioEngine != null) {
            audioEngine.pause();
        }
    }

    private void stopAudio() {
        if (audioEngine != null) {
            audioEngine.stop();
        }
    }

    private void seekAudio(double seconds) {
        if (audioEngine != null) {
            audioEngine.seek(seconds);
        }
    }

    private void syncAudio() {
        if (audioEngine != null) {
            audioEngine.syncTo(videoEngine.getCurrentTime());
        }
    }

    private void setState(PlaybackState state) {
        if (this.state == state) {
            return;
        }

        this.state = state;
        stateCallback.accept(state);
    }
}
