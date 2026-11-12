package com.nikipo.ardupilotoverlay.overlay;

import com.nikipo.ardupilotoverlay.telemetry.*;
import com.nikipo.ardupilotoverlay.telemetry.dataType.Home;
import com.nikipo.ardupilotoverlay.videoplayer.VideoPlayer;
import javafx.animation.AnimationTimer;

public final class TelemetrySynchronizer implements AutoCloseable {
    private final Telemetry telemetry;
    private final VideoPlayer player;
    private final OverlayModel model;
    private final long offsetUs;
    private final AnimationTimer timer;

    public TelemetrySynchronizer(Telemetry telemetry, VideoPlayer player,
                                 OverlayModel model, long offsetUs) {
        this.telemetry = telemetry;
        this.player = player;
        this.model = model;
        this.offsetUs = offsetUs;
        timer = new AnimationTimer() {
            @Override public void handle(long now) { update(); }
        };
    }

    public void start() {
        update();
        timer.start();
    }

    public void update() {
        long timeUs = Math.max(0L, Math.round(player.getCurrentTime() * 1_000_000.0) + offsetUs);
        model.telemetryTimeUs.set(timeUs);
        sample(telemetry.roll, timeUs, model.roll);
        sample(telemetry.pitch, timeUs, model.pitch);
        sample(telemetry.heading, timeUs, model.heading);
        sample(telemetry.relativeAltitude, timeUs, model.relativeAltitude);
        sample(telemetry.altitude, timeUs, model.altitude);
        sample(telemetry.groundSpeed, timeUs, model.groundSpeed);
        sample(telemetry.airSpeed, timeUs, model.airSpeed);
        sample(telemetry.batteryVoltage, timeUs, model.batteryVoltage);
        sample(telemetry.batteryCurrent, timeUs, model.batteryCurrent);
        sample(telemetry.batteryRemaining, timeUs, model.batteryRemaining);
        sample(telemetry.satellites, timeUs, model.satellites);
        if (telemetry.gpsFix.size() > 0) model.gpsFix.set(fixLabel((int) telemetry.gpsFix.sample(timeUs)));
        if (telemetry.armed.size() > 0) model.armed.set(telemetry.armed.sample(timeUs));
        if (telemetry.flightMode.size() > 0) model.flightMode.set(telemetry.flightMode.sample(timeUs));
        if (telemetry.latitude.size() > 0 && telemetry.longitude.size() > 0) {
            double lat = telemetry.latitude.sample(timeUs);
            double lon = telemetry.longitude.sample(timeUs);
            model.position.set(String.format("%.5f, %.5f", lat, lon));
            Home home = telemetry.getHome();
            if (home != null) {
                double lat1 = Math.toRadians(home.latitudeDegrees());
                double lat2 = Math.toRadians(lat);
                double dLat = lat2 - lat1;
                double dLon = Math.toRadians(lon - home.longitudeDegrees());
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
                model.homeDistance.set(6_371_000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
                model.homeBearing.set((Math.toDegrees(Math.atan2(
                        Math.sin(dLon) * Math.cos(lat2),
                        Math.cos(lat1) * Math.sin(lat2)
                                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon))) + 360) % 360);
            }
        }
    }

    private static <T extends Number> void sample(ContinuousTimeSeries<T> series, long timeUs,
                                                   javafx.beans.property.DoubleProperty target) {
        if (series.size() > 0) target.set(series.sample(timeUs));
    }

    private static void sample(ContinuousTimeSeries<Integer> series, long timeUs,
                               javafx.beans.property.IntegerProperty target) {
        if (series.size() > 0) target.set((int) Math.round(series.sample(timeUs)));
    }

    private static String fixLabel(int fix) {
        return switch (fix) {
            case 0, 1 -> "NO FIX";
            case 2 -> "2D";
            case 3 -> "3D";
            case 4 -> "DGPS";
            case 5 -> "RTK FLOAT";
            case 6 -> "RTK FIXED";
            default -> "--";
        };
    }

    @Override public void close() { timer.stop(); }
}
