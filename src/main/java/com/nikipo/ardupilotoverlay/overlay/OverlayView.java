package com.nikipo.ardupilotoverlay.overlay;

import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TelemetryCapability;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Locale;

public final class OverlayView implements AutoCloseable {
    private final Canvas canvas;
    private final OverlayModel model;
    private final Telemetry telemetry;
    private final ChangeListener<Object> redraw = (obs, oldValue, newValue) -> render();

    public OverlayView(Canvas canvas, Telemetry telemetry, OverlayModel model) {
        this.canvas = canvas;
        this.telemetry = telemetry;
        this.model = model;
        model.roll.addListener(redraw); model.pitch.addListener(redraw); model.heading.addListener(redraw);
        model.relativeAltitude.addListener(redraw); model.altitude.addListener(redraw);
        model.groundSpeed.addListener(redraw); model.airSpeed.addListener(redraw);
        model.batteryVoltage.addListener(redraw); model.batteryCurrent.addListener(redraw);
        model.batteryRemaining.addListener(redraw); model.satellites.addListener(redraw);
        model.gpsFix.addListener(redraw); model.armed.addListener(redraw); model.flightMode.addListener(redraw);
        model.homeDistance.addListener(redraw); model.homeBearing.addListener(redraw);
        model.position.addListener(redraw);
        canvas.widthProperty().addListener(redraw);
        canvas.heightProperty().addListener(redraw);
        render();
    }

    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFont(Font.font("Noto Sans Mono", Math.max(12, w / 55)));
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        if (has(TelemetryCapability.ROLL_PITCH)) drawHorizon(gc, w * .5, h * .54, Math.min(w, h) * .22);
        if (has(TelemetryCapability.HEADING)) drawHeading(gc, w * .5, 28, w * .28);
        drawTextBlocks(gc, w, h);
    }

    private void drawHorizon(GraphicsContext gc, double cx, double cy, double radius) {
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(model.roll.get());
        gc.setStroke(Color.rgb(255, 215, 0, .9));
        gc.strokeOval(-radius, -radius, radius * 2, radius * 2);
        gc.strokeLine(-radius * .8, 0, radius * .8, 0);
        double pitch = model.pitch.get() * radius / 45.0;
        gc.strokeLine(-radius * .55, pitch, -radius * .25, pitch);
        gc.strokeLine(radius * .25, pitch, radius * .55, pitch);
        gc.restore();
        label(gc, String.format(Locale.ROOT, "R %+.1f  P %+.1f", model.roll.get(), model.pitch.get()),
                cx - radius, cy + radius + 20);
    }

    private void drawHeading(GraphicsContext gc, double cx, double y, double width) {
        double heading = model.heading.get();
        gc.strokeLine(cx - width, y, cx + width, y);
        gc.setFill(Color.YELLOW);
        label(gc, String.format(Locale.ROOT, "%03.0f°", heading), cx - 28, y + 22);
        gc.setFill(Color.WHITE);
        for (int i = -3; i <= 3; i++) {
            double x = cx + i * width / 3;
            gc.strokeLine(x, y - 7, x, y + 7);
        }
    }

    private void drawTextBlocks(GraphicsContext gc, double w, double h) {
        double x = 16, y = 28;
        if (has(TelemetryCapability.RELATIVE_ALTITUDE) || has(TelemetryCapability.ALTITUDE)) {
            label(gc, String.format(Locale.ROOT, "ALT  %s  MSL %s",
                    value(model.relativeAltitude.get(), "m"), value(model.altitude.get(), "m")), x, y);
            y += 24;
        }
        if (has(TelemetryCapability.GROUND_SPEED) || has(TelemetryCapability.AIR_SPEED)) {
            label(gc, String.format(Locale.ROOT, "SPD  %s", value(model.groundSpeed.get(), "m/s")), x, y);
            y += 24;
        }
        if (has(TelemetryCapability.BATTERY_VOLTAGE) || has(TelemetryCapability.BATTERY_CURRENT)
                || has(TelemetryCapability.BATTERY_REMAINING)) {
            int pct = model.batteryRemaining.get();
            gc.setFill(pct >= 0 && pct < 20 ? Color.ORANGERED : Color.LIME);
            label(gc, String.format(Locale.ROOT, "BAT  %s  %s  %s",
                    value(model.batteryVoltage.get(), "V"), value(model.batteryCurrent.get(), "A"),
                    pct < 0 || pct > 100 ? "--" : pct + "%"), x, y);
            gc.setFill(Color.WHITE);
            y += 24;
        }
        if (has(TelemetryCapability.GPS) || has(TelemetryCapability.SATELLITES)) {
            label(gc, "GPS  " + (model.satellites.get() < 0 ? "--" : model.satellites.get())
                    + "  " + model.gpsFix.get(), x, y);
            y += 24;
        }
        if (has(TelemetryCapability.ARMED) || has(TelemetryCapability.FLIGHT_MODE)) {
            label(gc, (model.armed.get() ? "ARMED" : "DISARMED") + "  " + model.flightMode.get(), x, y);
        }
        if (has(TelemetryCapability.HOME)) {
            label(gc, String.format(Locale.ROOT, "HOME  %s  %s",
                    value(model.homeDistance.get(), "m"), value(model.homeBearing.get(), "°")), 16, h - 42);
        }
        if (has(TelemetryCapability.LATITUDE) && has(TelemetryCapability.LONGITUDE)) {
            label(gc, model.position.get(), w - 190, h - 18);
        }
    }

    private boolean has(TelemetryCapability capability) {
        if (!telemetry.hasCapability(capability)) {
            return false;
        }
        return switch (capability) {
            case ROLL_PITCH -> telemetry.roll.size() > 0 || telemetry.pitch.size() > 0;
            case HEADING -> telemetry.heading.size() > 0;
            case LATITUDE -> telemetry.latitude.size() > 0;
            case LONGITUDE -> telemetry.longitude.size() > 0;
            case ALTITUDE -> telemetry.altitude.size() > 0;
            case RELATIVE_ALTITUDE -> telemetry.relativeAltitude.size() > 0;
            case GROUND_SPEED -> telemetry.groundSpeed.size() > 0;
            case AIR_SPEED -> telemetry.airSpeed.size() > 0;
            case BATTERY_VOLTAGE -> telemetry.batteryVoltage.size() > 0;
            case BATTERY_CURRENT -> telemetry.batteryCurrent.size() > 0;
            case BATTERY_REMAINING -> telemetry.batteryRemaining.size() > 0;
            case GPS -> telemetry.gpsFix.size() > 0 || telemetry.satellites.size() > 0;
            case SATELLITES -> telemetry.satellites.size() > 0;
            case ARMED -> telemetry.armed.size() > 0;
            case FLIGHT_MODE -> telemetry.flightMode.size() > 0;
            case HOME -> telemetry.getHome() != null;
            default -> true;
        };
    }

    private static String value(double value, String suffix) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.1f%s", value, suffix) : "--";
    }

    private static void label(GraphicsContext gc, String text, double x, double y) {
        gc.fillText(text, x, y);
    }

    @Override public void close() {
        model.roll.removeListener(redraw); model.pitch.removeListener(redraw); model.heading.removeListener(redraw);
        model.relativeAltitude.removeListener(redraw); model.altitude.removeListener(redraw);
        model.groundSpeed.removeListener(redraw); model.airSpeed.removeListener(redraw);
        model.batteryVoltage.removeListener(redraw); model.batteryCurrent.removeListener(redraw);
        model.batteryRemaining.removeListener(redraw); model.satellites.removeListener(redraw);
        model.gpsFix.removeListener(redraw); model.armed.removeListener(redraw); model.flightMode.removeListener(redraw);
        model.homeDistance.removeListener(redraw); model.homeBearing.removeListener(redraw);
        model.position.removeListener(redraw);
        canvas.widthProperty().removeListener(redraw);
        canvas.heightProperty().removeListener(redraw);
    }
}
