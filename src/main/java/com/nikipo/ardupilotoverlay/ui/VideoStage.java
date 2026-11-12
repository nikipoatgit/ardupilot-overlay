package com.nikipo.ardupilotoverlay.ui;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class VideoStage extends Region {

    private final ImageView imageView = new ImageView();
    private final Canvas overlayCanvas = new Canvas();
    private final Canvas hudCanvas = new Canvas();
    private final ReadOnlyObjectWrapper<Rectangle2D> videoRect =
            new ReadOnlyObjectWrapper<>(this, "videoRect", Rectangle2D.EMPTY);

    private double mediaAspect = 16.0 / 9.0;
    private double rotationDegrees;

    public VideoStage() {
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        getChildren().addAll(imageView, overlayCanvas, hudCanvas);
        setStyle("-fx-background-color: black;");
    }

    public ImageView getImageView() {
        return imageView;
    }

    public Canvas getOverlayCanvas() {
        return overlayCanvas;
    }

    public Canvas getHudCanvas() {
        return hudCanvas;
    }

    public ReadOnlyObjectProperty<Rectangle2D> videoRectProperty() {
        return videoRect.getReadOnlyProperty();
    }

    public Rectangle2D getVideoRect() {
        return videoRect.get();
    }

    public void setMediaAspect(double mediaAspect) {
        if (Double.isFinite(mediaAspect) && mediaAspect > 0) {
            this.mediaAspect = mediaAspect;
            requestLayout();
        }
    }

    public void setRotationDegrees(double rotationDegrees) {
        this.rotationDegrees = Double.isFinite(rotationDegrees) ? rotationDegrees : 0.0;
        requestLayout();
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 360;
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double radians = Math.toRadians(rotationDegrees);
        double cosine = Math.abs(Math.cos(radians));
        double sine = Math.abs(Math.sin(radians));
        double boxWidth = mediaAspect * cosine + sine;
        double boxHeight = mediaAspect * sine + cosine;
        double videoHeight = Math.min(width / boxWidth, height / boxHeight);
        double videoWidth = videoHeight * mediaAspect;
        double x = (width - videoWidth) / 2.0;
        double y = (height - videoHeight) / 2.0;

        imageView.setFitWidth(videoWidth);
        imageView.setFitHeight(videoHeight);
        imageView.relocate(x, y);
        imageView.setRotate(rotationDegrees);

        overlayCanvas.setWidth(videoWidth);
        overlayCanvas.setHeight(videoHeight);
        overlayCanvas.relocate(x, y);
        hudCanvas.setWidth(videoWidth);
        hudCanvas.setHeight(videoHeight);
        hudCanvas.relocate(x, y);

        videoRect.set(new Rectangle2D(x, y, videoWidth, videoHeight));
    }
}
