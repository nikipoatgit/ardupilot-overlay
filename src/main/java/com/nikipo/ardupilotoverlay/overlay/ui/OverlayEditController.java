package com.nikipo.ardupilotoverlay.overlay.ui;

import com.nikipo.ardupilotoverlay.overlay.Overlay;
import com.nikipo.ardupilotoverlay.overlay.OverlayWidget;
import com.nikipo.ardupilotoverlay.overlay.Transform;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;

import java.util.function.Consumer;

/**
 * Attaches mouse handling to the overlay Canvas so the user can select,
 * drag-move and drag-scale widgets directly on the preview.
 *
 * A Canvas has no child nodes to hit-test individually, so hit-testing is
 * done manually against each widget's Transform, topmost zIndex first.
 */
public class OverlayEditController {

    private final Canvas canvas;
    private final Overlay overlay;
    private final Consumer<OverlayWidget> onSelectionChanged;
    private final Runnable onChanged;

    private OverlayWidget selected;
    private Transform.Handle activeHandle = Transform.Handle.NONE;

    private double dragStartSceneX;
    private double dragStartSceneY;
    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;
    private double videoWidth = 640;
    private double videoHeight = 360;

    public OverlayEditController(Canvas canvas, Overlay overlay, Consumer<OverlayWidget> onSelectionChanged) {
        this(canvas, overlay, onSelectionChanged, () -> {});
    }

    public OverlayEditController(
            Canvas canvas,
            Overlay overlay,
            Consumer<OverlayWidget> onSelectionChanged,
            Runnable onChanged
    ) {
        this.canvas = canvas;
        this.overlay = overlay;
        this.onSelectionChanged = onSelectionChanged;
        this.onChanged = onChanged;
    }

    public void attach() {
        canvas.setOnMousePressed(this::handlePressed);
        canvas.setOnMouseDragged(this::handleDragged);
        canvas.setOnMouseReleased(this::handleReleased);
        canvas.setOnMouseMoved(this::handleMoved);
    }

    public void setVideoSpace(double width, double height) {
        if (width > 0 && height > 0) {
            videoWidth = width;
            videoHeight = height;
        }
    }

    private double toVideoX(double x) {
        return canvas.getWidth() > 0 ? x * videoWidth / canvas.getWidth() : x;
    }

    private double toVideoY(double y) {
        return canvas.getHeight() > 0 ? y * videoHeight / canvas.getHeight() : y;
    }

    private void handlePressed(MouseEvent event) {
        double px = toVideoX(event.getX());
        double py = toVideoY(event.getY());

        selected = null;
        activeHandle = Transform.Handle.NONE;

        for (OverlayWidget widget : overlay.getWidgetsTopFirst()) {
            if (widget.getTransform().isLocked()) {
                continue;
            }
            Transform.Handle handle = widget.getTransform().hitTest(px, py);
            if (handle != Transform.Handle.NONE) {
                selected = widget;
                activeHandle = handle;
                break;
            }
        }

        if (onSelectionChanged != null) {
            onSelectionChanged.accept(selected);
        }

        if (selected == null) {
            return;
        }

        dragStartSceneX = px;
        dragStartSceneY = py;

        Transform t = selected.getTransform();
        startX = t.getX();
        startY = t.getY();
        startWidth = t.getWidth();
        startHeight = t.getHeight();
    }

    private void handleDragged(MouseEvent event) {
        if (selected == null || activeHandle == Transform.Handle.NONE) {
            return;
        }

        double dx = toVideoX(event.getX()) - dragStartSceneX;
        double dy = toVideoY(event.getY()) - dragStartSceneY;

        Transform t = selected.getTransform();

        if (activeHandle == Transform.Handle.MOVE) {
            t.setX(startX + dx);
            t.setY(startY + dy);
        } else if (activeHandle == Transform.Handle.RESIZE_BR) {
            t.setWidth(startWidth + dx);
            t.setHeight(startHeight + dy);
        }
        onChanged.run();
    }

    private void handleReleased(MouseEvent event) {
        activeHandle = Transform.Handle.NONE;
    }

    /** Updates the cursor to hint move vs. resize as the mouse hovers. */
    private void handleMoved(MouseEvent event) {
        double px = toVideoX(event.getX());
        double py = toVideoY(event.getY());

        for (OverlayWidget widget : overlay.getWidgetsTopFirst()) {
            if (widget.getTransform().isLocked()) {
                continue;
            }
            Transform.Handle handle = widget.getTransform().hitTest(px, py);
            if (handle == Transform.Handle.RESIZE_BR) {
                canvas.setCursor(Cursor.SE_RESIZE);
                return;
            }
            if (handle == Transform.Handle.MOVE) {
                canvas.setCursor(Cursor.MOVE);
                return;
            }
        }

        canvas.setCursor(Cursor.DEFAULT);
    }

    public OverlayWidget getSelected() {
        return selected;
    }
}