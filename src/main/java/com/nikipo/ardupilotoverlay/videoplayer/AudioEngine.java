package com.nikipo.ardupilotoverlay.videoplayer;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

public final class AudioEngine {
    private static final Logger logger = AppLogger.getLogger(AudioEngine.class);
    private final Path source;
    private final Object lock = new Object();
    private volatile boolean muted;
    private volatile boolean requestedPlaying;
    private volatile boolean disposed;
    private volatile boolean available;
    private volatile double playbackSpeed = 1.0;
    private final AtomicReference<ClockAnchor> clock =
            new AtomicReference<>(new ClockAnchor(0.0, 0L, false));
    private volatile SourceDataLine line;
    private Thread worker;
    private Consumer<Throwable> errorCallback;
    private double pendingSeekSeconds;
    private boolean seekPending;

    private AudioEngine(Path source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public static AudioEngine tryOpen(Path source) {
        return source == null ? null : new AudioEngine(source);
    }

    public void play(double startSeconds, double playbackSpeed) {
        play(startSeconds, playbackSpeed, null, null);
    }

    public void play(
            double startSeconds,
            double playbackSpeed,
            Runnable onReady,
            Consumer<Throwable> onError
    ) {
        synchronized (lock) {
            this.playbackSpeed = playbackSpeed;
            this.errorCallback = onError;
            pendingSeekSeconds = Math.max(0.0, startSeconds);
            seekPending = true;
            requestedPlaying = true;
            ensureWorkerLocked();
            lock.notifyAll();
        }
        if (onReady != null) {
            onReady.run();
        }
    }

    public void pause() {
        double frozenSeconds = getClockSeconds();
        synchronized (lock) {
            requestedPlaying = false;
            available = false;
            clock.set(new ClockAnchor(frozenSeconds, 0L, false));
            lock.notifyAll();
        }
        SourceDataLine currentLine = line;
        if (currentLine != null) {
            currentLine.stop();
        }
    }

    public void stop() {
        synchronized (lock) {
            requestedPlaying = false;
            seekPending = true;
            pendingSeekSeconds = 0.0;
            available = false;
            clock.set(new ClockAnchor(0.0, 0L, false));
            lock.notifyAll();
        }
    }

    public void seek(double seconds) {
        synchronized (lock) {
            pendingSeekSeconds = Math.max(0.0, seconds);
            seekPending = true;
            available = false;
            clock.set(new ClockAnchor(pendingSeekSeconds, 0L, false));
            lock.notifyAll();
        }
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        if (playbackSpeed <= 0) {
            throw new IllegalArgumentException("Playback speed must be greater than zero.");
        }
        this.playbackSpeed = playbackSpeed;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public double getClockSeconds() {
        ClockAnchor anchor = clock.get();
        return anchor.running()
                ? anchor.seconds() + Math.max(0L, System.nanoTime() - anchor.nanos())
                / 1_000_000_000.0
                : anchor.seconds();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isAvailable() {
        return available;
    }

    public void dispose() {
        Thread thread;
        synchronized (lock) {
            disposed = true;
            requestedPlaying = false;
            lock.notifyAll();
            thread = worker;
        }
        SourceDataLine currentLine = line;
        if (currentLine != null) {
            currentLine.stop();
            currentLine.flush();
        }
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void ensureWorkerLocked() {
        if (worker != null) {
            return;
        }
        worker = new Thread(this::run, "audio-playback");
        worker.setDaemon(true);
        worker.start();
    }

    private void run() {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = createGrabber();
            grabber.start();
            int sampleRate = grabber.getSampleRate();
            int channels = grabber.getAudioChannels();
            if (sampleRate <= 0 || channels <= 0) {
                return;
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
            SourceDataLine output = AudioSystem.getSourceDataLine(format);
            int bufferBytes = sampleRate * channels * Short.BYTES * 2 / 5;
            bufferBytes -= bufferBytes % format.getFrameSize();
            output.open(format, Math.max(format.getFrameSize(), bufferBytes));
            line = output;
            available = false;

            while (!disposed) {
                waitForCommand();
                if (disposed) {
                    break;
                }

                if (seekPending) {
                    applySeek(grabber);
                }
                if (!requestedPlaying) {
                    output.stop();
                    available = false;
                    continue;
                }
                if (playbackSpeed != 1.0) {
                    output.stop();
                    synchronized (lock) {
                        if (!disposed && requestedPlaying && playbackSpeed != 1.0) {
                            lock.wait();
                        }
                    }
                    continue;
                }

                output.start();
                available = true;
                synchronized (lock) {
                    if (!requestedPlaying) {
                        available = false;
                        continue;
                    }
                    ClockAnchor anchor = clock.get();
                    if (!anchor.running()) {
                        clock.set(new ClockAnchor(anchor.seconds(), System.nanoTime(), true));
                    }
                }
                Frame frame = grabber.grabSamples();
                if (frame == null) {
                    break;
                }
                byte[] bytes = samplesToBytes(frame.samples, channels, muted);
                if (bytes.length > 0) {
                    output.write(bytes, 0, bytes.length);
                }
            }
        } catch (Exception e) {
            if (!disposed) {
                logger.warn("Audio playback failed for {}", source, e);
                Consumer<Throwable> callback = errorCallback;
                if (callback != null) {
                    callback.accept(e);
                }
            }
        } finally {
            available = false;
            SourceDataLine output = line;
            line = null;
            if (output != null) {
                output.stop();
                output.flush();
                output.close();
            }
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception e) {
                    logger.warn("Failed to release audio resources for {}", source, e);
                }
            }
            synchronized (lock) {
                worker = null;
            }
        }
    }

    private void waitForCommand() throws InterruptedException {
        synchronized (lock) {
            while (!disposed && !requestedPlaying && !seekPending) {
                lock.wait();
            }
        }
    }

    private void applySeek(FFmpegFrameGrabber grabber) throws FFmpegFrameGrabber.Exception {
        double target;
        synchronized (lock) {
            target = pendingSeekSeconds;
            seekPending = false;
        }
        grabber.setTimestamp((long) (target * 1_000_000));
        SourceDataLine output = line;
        if (output != null) {
            output.flush();
        }
        available = false;
        clock.set(new ClockAnchor(target, 0L, false));
    }

    private FFmpegFrameGrabber createGrabber() {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(source.toFile());
        grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
        return grabber;
    }

    private static byte[] samplesToBytes(Buffer[] samples, int channels, boolean muted) {
        if (samples == null || samples.length == 0 || channels <= 0
                || !(samples[0] instanceof ShortBuffer)) {
            return new byte[0];
        }
        if (samples.length == 1 || samples.length < channels) {
            return packedSamplesToBytes((ShortBuffer) samples[0], muted);
        }

        ShortBuffer[] channelBuffers = new ShortBuffer[channels];
        int frameCount = Integer.MAX_VALUE;
        for (int channel = 0; channel < channels; channel++) {
            if (!(samples[channel] instanceof ShortBuffer buffer)) {
                return new byte[0];
            }
            channelBuffers[channel] = buffer.duplicate();
            frameCount = Math.min(frameCount, channelBuffers[channel].remaining());
        }
        byte[] bytes = new byte[frameCount * channels * Short.BYTES];
        int index = 0;
        for (int frame = 0; frame < frameCount; frame++) {
            for (ShortBuffer channel : channelBuffers) {
                short value = muted ? 0 : channel.get();
                bytes[index++] = (byte) value;
                bytes[index++] = (byte) (value >> 8);
            }
        }
        return bytes;
    }

    private static byte[] packedSamplesToBytes(ShortBuffer source, boolean muted) {
        ShortBuffer buffer = source.duplicate();
        byte[] bytes = new byte[buffer.remaining() * Short.BYTES];
        int index = 0;
        while (buffer.hasRemaining()) {
            short value = muted ? 0 : buffer.get();
            bytes[index++] = (byte) value;
            bytes[index++] = (byte) (value >> 8);
        }
        return bytes;
    }

    private record ClockAnchor(double seconds, long nanos, boolean running) {
    }
}
