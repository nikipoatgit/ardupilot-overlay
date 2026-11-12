package com.nikipo.ardupilotoverlay.overlay.render;

import com.nikipo.ardupilotoverlay.overlay.WidgetProperty;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * Draws a widget. A Renderer never knows about telemetry sources - it is
 * only ever given the widget's current display value and its Transform's
 * width/height to draw within.
 */
public interface Renderer {

    /**
     * @param gc      target graphics context, already translated/rotated/
     *                clipped to the widget's Transform by OverlayWidget.render()
     * @param width   local drawing width (Transform.getWidth())
     * @param height  local drawing height (Transform.getHeight())
     * @param value   current (animated) display value from the widget
     */
    void draw(GraphicsContext gc, double width, double height, double value);

    /** Renderer-specific styling knobs (colors, needle length, etc). */
    List<WidgetProperty<?>> getProperties();
}