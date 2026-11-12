package com.nikipo.ardupilotoverlay.videoplayer;

public class VideoInfo {
    private final double durationSeconds;

    private double startSeconds;
    private double endSeconds;
    private boolean muted;

    private double rotationDegrees;
    private double opacityPercent;
    private double brightness;
    private double contrast;
    private double saturation;
    private double temperature;
    private double tint;

    public VideoInfo(double durationSeconds) {
        this.durationSeconds = Math.max(0.0, durationSeconds);
        this.startSeconds = 0.0;
        this.endSeconds = this.durationSeconds;
        this.muted = false;
        this.rotationDegrees = 0.0;
        this.opacityPercent = 100.0;
        this.brightness = 0.0;
        this.contrast = 100.0;
        this.saturation = 100.0;
        this.temperature = 0.0;
        this.tint = 0.0;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getStartSeconds() {
        return startSeconds;
    }

    public void setStartSeconds(double startSeconds) {
        this.startSeconds = clamp(startSeconds, 0.0, endSeconds);
    }

    public double getEndSeconds() {
        return endSeconds;
    }

    public void setEndSeconds(double endSeconds) {
        this.endSeconds = clamp(endSeconds, startSeconds, durationSeconds);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public double getOpacityPercent() {
        return opacityPercent;
    }

    public void setOpacityPercent(double opacityPercent) {
        this.opacityPercent = clamp(opacityPercent, 0.0, 100.0);
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public double getContrast() {
        return contrast;
    }

    public void setContrast(double contrast) {
        this.contrast = contrast;
    }

    public double getSaturation() {
        return saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTint() {
        return tint;
    }

    public void setTint(double tint) {
        this.tint = tint;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
