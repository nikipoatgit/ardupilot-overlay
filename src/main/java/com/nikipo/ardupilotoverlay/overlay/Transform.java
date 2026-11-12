package com.nikipo.ardupilotoverlay.overlay;

/**
 * Common spatial state every OverlayWidget has: position, size, rotation,
 * opacity, visibility, stacking order and anchor point.
 *
 * Also owns the hit-testing logic needed for drag-to-move and
 * drag-to-scale in the editor (see OverlayEditController).
 */
public class Transform {

    public enum Anchor { TOP_LEFT, CENTER }

    public enum Handle { NONE, MOVE, RESIZE_BR }

    private double x;
    private double y;
    private double width = 120;
    private double height = 120;

    private double rotation;
    private double opacity = 1.0;
    private boolean visible = true;
    private boolean locked;
    private int zIndex;
    private Anchor anchor = Anchor.TOP_LEFT;

    /** Size in pixels of the resize handle hit-box, in screen space. */
    private static final double HANDLE_SIZE = 14;

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = Math.max(8, width); }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = Math.max(8, height); }

    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }

    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = Math.max(0, Math.min(1, opacity)); }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getZIndex() { return zIndex; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }

    public Anchor getAnchor() { return anchor; }
    public void setAnchor(Anchor anchor) { this.anchor = anchor; }

    /** Top-left corner in scene space, regardless of anchor mode. */
    public double getLeft() {
        return anchor == Anchor.CENTER ? x - width / 2 : x;
    }

    public double getTop() {
        return anchor == Anchor.CENTER ? y - height / 2 : y;
    }

    public boolean contains(double px, double py) {
        double left = getLeft();
        double top = getTop();
        return px >= left && px <= left + width
                && py >= top && py <= top + height;
    }

    /** Returns which interaction handle (if any) the point lands on. */
    public Handle hitTest(double px, double py) {
        double left = getLeft();
        double top = getTop();
        double right = left + width;
        double bottom = top + height;

        if (Math.abs(px - right) <= HANDLE_SIZE / 2 && Math.abs(py - bottom) <= HANDLE_SIZE / 2) {
            return Handle.RESIZE_BR;
        }

        if (contains(px, py)) {
            return Handle.MOVE;
        }

        return Handle.NONE;
    }

    public void moveBy(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void resizeBy(double dw, double dh) {
        setWidth(width + dw);
        setHeight(height + dh);
    }
}