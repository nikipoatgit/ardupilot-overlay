package com.nikipo.ardupilotoverlay.overlay;

import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A simple ordered collection of widgets. Rendering just loops over them. */
public class Overlay {

    private final List<OverlayWidget> widgets = new ArrayList<>();

    public void addWidget(OverlayWidget widget) {
        widgets.add(widget);
    }

    public void removeWidget(OverlayWidget widget) {
        widgets.remove(widget);
    }

    public List<OverlayWidget> getWidgets() {
        return widgets;
    }

    /** Widgets sorted for hit-testing (topmost/highest zIndex first). */
    public List<OverlayWidget> getWidgetsTopFirst() {
        List<OverlayWidget> sorted = new ArrayList<>(widgets);
        sorted.sort(Comparator.comparingInt((OverlayWidget w) -> w.getTransform().getZIndex()).reversed());
        return sorted;
    }

    public void render(GraphicsContext gc, long timeUs) {
        List<OverlayWidget> sorted = new ArrayList<>(widgets);
        sorted.sort(Comparator.comparingInt(w -> w.getTransform().getZIndex()));

        for (OverlayWidget widget : sorted) {
            widget.render(gc, timeUs);
        }
    }
}