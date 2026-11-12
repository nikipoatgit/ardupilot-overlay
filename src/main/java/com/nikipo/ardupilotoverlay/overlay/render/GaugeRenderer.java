package com.nikipo.ardupilotoverlay.overlay.render;

import com.nikipo.ardupilotoverlay.overlay.WidgetProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Draws a circular needle gauge. Works for any numeric telemetry value -
 * the widget decides WHAT value flows in, this only decides HOW it looks.
 */
public class GaugeRenderer implements Renderer {

    private double minValue;
    private double maxValue;
    private double startAngle = 210;   // degrees, gauge sweep start
    private double sweepAngle = 240;   // degrees, total sweep
    private int tickCount = 6;
    private Color needleColor = Color.web("#ff3b30");
    private Color faceColor = Color.web("#1c1c1e");
    private Color tickColor = Color.web("#e5e5ea");
    private String unitLabel = "";
    private String formatPattern = "%.1f";

    public GaugeRenderer(double minValue, double maxValue, String unitLabel) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unitLabel = unitLabel;
    }

    @Override
    public void draw(GraphicsContext gc, double width, double height, double value) {
        double cx = width / 2;
        double cy = height / 2;
        double radius = Math.min(width, height) / 2 - 4;

        // Face
        gc.setFill(faceColor);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Ticks
        gc.setStroke(tickColor);
        gc.setLineWidth(2);
        for (int i = 0; i <= tickCount; i++) {
            double t = (double) i / tickCount;
            double angle = Math.toRadians(startAngle - t * sweepAngle);
            double innerR = radius * 0.82;
            double outerR = radius * 0.95;
            double x1 = cx + Math.cos(angle) * innerR;
            double y1 = cy - Math.sin(angle) * innerR;
            double x2 = cx + Math.cos(angle) * outerR;
            double y2 = cy - Math.sin(angle) * outerR;
            gc.strokeLine(x1, y1, x2, y2);
        }

        // Needle
        double clamped = Math.max(minValue, Math.min(maxValue, value));
        double ratio = (maxValue > minValue) ? (clamped - minValue) / (maxValue - minValue) : 0;
        double needleAngle = Math.toRadians(startAngle - ratio * sweepAngle);
        double needleLength = radius * 0.75;

        gc.setStroke(needleColor);
        gc.setLineWidth(3);
        gc.strokeLine(
                cx, cy,
                cx + Math.cos(needleAngle) * needleLength,
                cy - Math.sin(needleAngle) * needleLength
        );

        gc.setFill(needleColor);
        gc.fillOval(cx - 4, cy - 4, 8, 8);

        // Digital readout
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Noto Sans", FontWeight.BOLD, radius * 0.22));
        gc.fillText(String.format(formatPattern, clamped) + unitLabel, cx, cy + radius * 0.5);
    }

    @Override
    public List<WidgetProperty<?>> getProperties() {
        return List.of(
                new WidgetProperty<>("Min Value", Double.class, () -> minValue, v -> minValue = v),
                new WidgetProperty<>("Max Value", Double.class, () -> maxValue, v -> maxValue = v),
                new WidgetProperty<>("Tick Count", Integer.class, () -> tickCount, v -> tickCount = v),
                new WidgetProperty<>("Needle Color", Color.class, () -> needleColor, v -> needleColor = v),
                new WidgetProperty<>("Face Color", Color.class, () -> faceColor, v -> faceColor = v),
                new WidgetProperty<>("Unit Label", String.class, () -> unitLabel, v -> unitLabel = v)
        );
    }
}