package com.nikipo.ardupilotoverlay.overlay;

import com.nikipo.ardupilotoverlay.overlay.render.Renderer;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every overlay widget. A widget decides WHAT telemetry to
 * display; a Renderer decides HOW it looks. Transform and simple value
 * smoothing (the "Animation" stage from the architecture doc) live here so
 * every concrete widget gets them for free.
 */
public abstract class OverlayWidget {

    private final Transform transform = new Transform();
    private Renderer renderer;

    // Simple exponential smoothing between raw telemetry and displayed value.
    private double displayedValue;
    private boolean initialized;
    private double smoothingFactor = 0.15; // 0 = no smoothing, 1 = instant

    protected OverlayWidget(Renderer renderer) {
        this.renderer = renderer;
    }

    public Transform getTransform() {
        return transform;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public double getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(double smoothingFactor) {
        this.smoothingFactor = Math.max(0, Math.min(1, smoothingFactor));
    }

    /** Reads the current raw value from telemetry. WHAT to display. */
    protected abstract double sampleValue(long timeUs);

    /** Widget-specific properties (independent of Transform/Renderer). */
    public abstract List<WidgetProperty<?>> getWidgetProperties();

    /** Combines widget, transform and renderer properties for the panel. */
    public final List<WidgetProperty<?>> getAllProperties() {
        List<WidgetProperty<?>> all = new ArrayList<>();
        all.addAll(getWidgetProperties());
        all.addAll(renderer.getProperties());
        return all;
    }

    /** Advances animation state, then draws via the current renderer. */
    public final void render(GraphicsContext gc, long timeUs) {
        if (!transform.isVisible()) {
            return;
        }

        double raw = sampleValue(timeUs);
        displayedValue = initialized
                ? displayedValue + (raw - displayedValue) * smoothingFactor
                : raw;
        initialized = true;

        gc.save();

        gc.translate(transform.getLeft(), transform.getTop());
        gc.setGlobalAlpha(transform.getOpacity());

        if (transform.getRotation() != 0) {
            gc.translate(transform.getWidth() / 2, transform.getHeight() / 2);
            gc.rotate(transform.getRotation());
            gc.translate(-transform.getWidth() / 2, -transform.getHeight() / 2);
        }

        renderer.draw(gc, transform.getWidth(), transform.getHeight(), displayedValue);

        gc.restore();
    }
}