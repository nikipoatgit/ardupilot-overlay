package com.nikipo.ardupilotoverlay.playback;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class AudioEngine {
    private static final Logger logger = AppLogger.getLogger(AudioEngine.class);
    private static final double BUFFER_AHEAD_SECONDS = 0.25;
    private static final long IDLE_SLEEP_MILLIS = 5L;

    private final Path source;

    private volatile boolean muted;
    private volatile boolean playing;
    private volatile double playbackSpeed = 1.0;
    private volatile double pendingSeekSeconds;
    private volatile double targetSeconds;
    private FFmpegFrameGrabber grabber;
    private volatile SourceDataLine activeLine;
    private Thread playbackThread;
    private int sampleRate;
    private int channels;

    public AudioEngine(Path source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static AudioEngine tryOpen(Path source) {
        if (source == null) {
            return null;
        }

        try (FFmpegFrameGrabber probe = createGrabber(source)) {
            probe.start();
            if (probe.getAudioChannels() <= 0) {
                return null;
            }

            return new AudioEngine(source);
        } catch (Exception e) {
            logger.warn("Audio is not available for {}", source, e);
            return null;
        }
    }

    public synchronized void play(double startSeconds, double playbackSpeed) {
        play(startSeconds, playbackSpeed, null, null);
    }

    public synchronized void play(
            double startSeconds,
            double playbackSpeed,
            Runnable onReady,
            Consumer<Throwable> onError
    ) {
        this.playbackSpeed = playbackSpeed;
        pendingSeekSeconds = Math.max(0.0, startSeconds);
        targetSeconds = pendingSeekSeconds;
        stopPlaybackResources();

        playing = true;

        try {
            openPlaybackResources(pendingSeekSeconds);
            startPlaybackThread(onError);
            if (onReady != null) {
                onReady.run();
            }
        } catch (LineUnavailableException e) {
            logger.warn("Audio output device is not available", e);
            playing = false;
            stopPlaybackResources();
            if (onError != null) {
                onError.accept(e);
            }
        } catch (Exception e) {
            logger.warn("Audio playback failed for {}", source, e);
            playing = false;
            stopPlaybackResources();
            if (onError != null) {
                onError.accept(e);
            }
        }
    }

    public synchronized void pause() {
        playing = false;
        stopPlaybackResources();
    }

    public synchronized void stop() {
        playing = false;
        pendingSeekSeconds = 0.0;
        stopPlaybackResources();
    }

    public synchronized void seek(double seconds) {
        pendingSeekSeconds = Math.max(0.0, seconds);
        targetSeconds = pendingSeekSeconds;

        if (!playing) {
            return;
        }

        stopPlaybackResources();

        try {
            openPlaybackResources(pendingSeekSeconds);
            startPlaybackThread(null);
        } catch (Exception e) {
            playing = false;
            logger.warn("Failed to seek audio for {}", source, e);
        }
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public synchronized void syncTo(double targetSeconds) {
        this.targetSeconds = Math.max(0.0, targetSeconds);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public synchronized void dispose() {
        stop();
    }

    private void startPlaybackThread(Consumer<Throwable> onError) {
        stopPlaybackThread();

        Thread thread = new Thread(() -> runPlaybackLoop(onError), "audio-playback");
        thread.setDaemon(true);
        playbackThread = thread;
        thread.start();
    }

    private void runPlaybackLoop(Consumer<Throwable> onError) {
        try {
            while (playing) {
                double currentTargetSeconds = targetSeconds + BUFFER_AHEAD_SECONDS;
                double currentAudioSeconds = grabber.getTimestamp() / 1_000_000.0;

                if (currentAudioSeconds >= currentTargetSeconds) {
                    sleepQuietly();
                    continue;
                }

                Frame frame = grabber.grabSamples();
                if (frame == null) {
                    break;
                }

                byte[] bytes = samplesToBytes(frame.samples, channels, muted);
                if (bytes.length > 0 && activeLine != null) {
                    activeLine.write(bytes, 0, bytes.length);
                }
            }

            if (activeLine != null) {
                activeLine.drain();
            }
        } catch (Exception e) {
            if (playing) {
                logger.warn("Audio playback failed for {}", source, e);
                if (onError != null) {
                    onError.accept(e);
                }
            }
        } finally {
            synchronized (this) {
                playbackThread = null;
            }
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(IDLE_SLEEP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void openPlaybackResources(double startSeconds) throws Exception {
        grabber = createGrabber(source);
        grabber.start();
        grabber.setTimestamp((long) (startSeconds * 1_000_000));

        sampleRate = grabber.getSampleRate();
        channels = grabber.getAudioChannels();

        if (sampleRate <= 0 || channels <= 0) {
            throw new IllegalStateException("Audio stream is not available.");
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        activeLine = line;
        line.open(format);
        line.start();
    }

    private synchronized void stopPlaybackResources() {
        stopPlaybackThread();

        SourceDataLine line = activeLine;
        activeLine = null;
        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
            }

            try {
                line.flush();
            } catch (Exception ignored) {
            }

            try {
                line.close();
            } catch (Exception ignored) {
            }
        }

        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception ignored) {
            }

            try {
                grabber.release();
            } catch (Exception ignored) {
            }

            grabber = null;
        }
    }

    private synchronized void stopPlaybackThread() {
        Thread thread = playbackThread;
        playbackThread = null;

        if (thread != null) {
            thread.interrupt();
        }
    }

    private static FFmpegFrameGrabber createGrabber(Path source) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(source.toFile());
        grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
        return grabber;
    }

    private static byte[] samplesToBytes(Buffer[] samples, int channels, boolean muted) {
        if (samples == null || samples.length == 0 || channels <= 0 || !(samples[0] instanceof ShortBuffer)) {
            return new byte[0];
        }

        if (samples.length == 1 || samples.length < channels) {
            return packedSamplesToBytes((ShortBuffer) samples[0], muted);
        }

        return planarSamplesToBytes(samples, channels, muted);
    }

    private static byte[] packedSamplesToBytes(ShortBuffer source, boolean muted) {
        ShortBuffer buffer = source.duplicate();
        byte[] bytes = new byte[buffer.remaining() * Short.BYTES];
        int index = 0;

        while (buffer.hasRemaining()) {
            short sample = buffer.get();
            short value = muted ? 0 : sample;
            bytes[index++] = (byte) (value & 0xff);
            bytes[index++] = (byte) ((value >> 8) & 0xff);
        }

        return bytes;
    }

    private static byte[] planarSamplesToBytes(Buffer[] samples, int channels, boolean muted) {
        ShortBuffer[] channelBuffers = new ShortBuffer[channels];
        int frameCount = Integer.MAX_VALUE;

        for (int channel = 0; channel < channels; channel++) {
            if (!(samples[channel] instanceof ShortBuffer shortBuffer)) {
                return new byte[0];
            }

            channelBuffers[channel] = shortBuffer.duplicate();
            frameCount = Math.min(frameCount, channelBuffers[channel].remaining());
        }

        if (frameCount <= 0 || frameCount == Integer.MAX_VALUE) {
            return new byte[0];
        }

        byte[] bytes = new byte[frameCount * channels * Short.BYTES];
        int index = 0;

        for (int frame = 0; frame < frameCount; frame++) {
            for (int channel = 0; channel < channels; channel++) {
                short sample = channelBuffers[channel].get();
                short value = muted ? 0 : sample;
                bytes[index++] = (byte) (value & 0xff);
                bytes[index++] = (byte) ((value >> 8) & 0xff);
            }
        }

        return bytes;
    }
}