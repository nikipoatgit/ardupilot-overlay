package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.overlay.Overlay;
import com.nikipo.ardupilotoverlay.overlay.OverlayWidget;
import com.nikipo.ardupilotoverlay.overlay.Transform;
import com.nikipo.ardupilotoverlay.overlay.ui.OverlayEditController;
import com.nikipo.ardupilotoverlay.overlay.widgets.BatteryVoltageWidget;
import com.nikipo.ardupilotoverlay.overlay.OverlayModel;
import com.nikipo.ardupilotoverlay.overlay.OverlayView;
import com.nikipo.ardupilotoverlay.overlay.TelemetrySynchronizer;
import com.nikipo.ardupilotoverlay.project.Project;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.telemetry.Telemetry;
import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import com.nikipo.ardupilotoverlay.ui.VideoStage;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import com.nikipo.ardupilotoverlay.videoplayer.VideoEngine;
import com.nikipo.ardupilotoverlay.videoplayer.VideoPlayer;
import com.nikipo.ardupilotoverlay.videoplayer.PlaybackState;
import com.nikipo.ardupilotoverlay.logging.AppLogger;
import org.slf4j.Logger;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.embed.swing.SwingFXUtils;

public class OverlayController implements Navigable {
    private static final Logger logger = AppLogger.getLogger(OverlayController.class);

    // ---- Preview canvas ----
    @FXML private VideoStage videoStage;

    // ---- Timeline ----
    @FXML private Button playButton;
    @FXML private Button muteButton;
    @FXML private Button markerButton;
    @FXML private Label currentTimeLabel;
    @FXML private Slider timelineSlider;
    @FXML private Label totalTimeLabel;
    @FXML private Button expandTimelineButton;

    // ---- Gauge sidebar / picker ----
    @FXML private Button addGaugeButton;
    @FXML private Button editGaugesButton;
    @FXML private Button syncTelemetryButton;
    @FXML private Button trimGaugeButton;
    @FXML private Button patternsButton;
    @FXML private FlowPane gaugeTypeFlowPane;
    @FXML private Button speedGaugeButton;

    // ---- Property panel ----
    @FXML private VBox propertyPanel;
    @FXML private Label propertyPanelTitle;
    @FXML private Label propertyPanelSubtitle;
    @FXML private Button toggleVisibleButton;
    @FXML private Button toggleLockButton;
    @FXML private Button duplicateWidgetButton;
    @FXML private Button colorTagButton;
    @FXML private Button deleteWidgetButton;
    @FXML private Label propertyPanelHint;

    @FXML private Slider sizeSlider;
    @FXML private Label sizeValueLabel;

    @FXML private ColorPicker textColorPicker;
    @FXML private ColorPicker ticksColorPicker;
    @FXML private ColorPicker gaugeColorPicker;
    @FXML private ColorPicker labelsColorPicker;
    @FXML private ColorPicker backColorPicker;

    @FXML private TabPane propertyTabPane;
    @FXML private Slider textValuesSlider;
    @FXML private Label textValuesLabel;
    @FXML private Slider decimalsSlider;
    @FXML private Label decimalsLabel;

    @FXML private ComboBox<String> unitsComboBox;
    @FXML private CheckBox maxAutoCheckBox;
    @FXML private Slider maxSlider;
    @FXML private Label maxValueLabel;
    @FXML private CheckBox minAutoCheckBox;
    @FXML private Slider minSlider;
    @FXML private Label minValueLabel;
    @FXML private Slider offsetSlider;
    @FXML private Label offsetValueLabel;
    @FXML private Slider gapSlider;
    @FXML private Label gapValueLabel;
    @FXML private ToggleButton invertToggle;
    @FXML private ToggleButton wrapToggle;
    @FXML private Slider smoothingSlider;
    @FXML private Label smoothingValueLabel;

    private static final double BASE_WIDGET_SIZE = 140;

    private UIOrchestrator orchestrator;
    private Overlay overlay;
    private OverlayWidget selectedWidget;
    private AnimationTimer previewTimer;
    private VideoPlayer playback;
    private TelemetrySynchronizer synchronizer;
    private OverlayView hudView;
    private OverlayModel hudModel;
    private boolean overlayDirty = true;
    private double sourceWidth = 640;
    private double sourceHeight = 360;

    @Override
    public void setOrchestrator(UIOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @FXML
    private void initialize() {
        overlay = new Overlay();

        Project project = ProjectManager.getInstance().getCurrentProject();
        Telemetry telemetry = project != null ? project.getTelemetry() : null;

        if (telemetry != null) {
            BatteryVoltageWidget battery = new BatteryVoltageWidget(telemetry.batteryVoltage);
            battery.getTransform().setX(40);
            battery.getTransform().setY(40);
            overlay.addWidget(battery);
        }

        VideoEngine videoEngine = ProjectManager.getInstance().getVideoEngine();
        if (videoEngine != null) {
            videoStage.setMediaAspect(videoEngine.getSourceAspect());
            sourceWidth = videoEngine.getSourceWidth();
            sourceHeight = videoEngine.getSourceHeight();
        }
        videoStage.videoRectProperty().addListener((obs, oldRect, newRect) -> overlayDirty = true);
        Canvas overlayCanvas = videoStage.getOverlayCanvas();
        sourceWidth = videoEngine != null ? videoEngine.getSourceWidth() : 640;
        sourceHeight = videoEngine != null ? videoEngine.getSourceHeight() : 360;
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            sourceWidth = 640;
            sourceHeight = 360;
        }

        OverlayEditController editController = new OverlayEditController(
                overlayCanvas,
                overlay,
                this::onWidgetSelected,
                () -> overlayDirty = true
        );
        editController.setVideoSpace(sourceWidth, sourceHeight);
        editController.attach();

        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedWidget == null) {
                return;
            }
            double scale = newVal.doubleValue() / 5.0; // slider default (5.0) == 1x
            selectedWidget.getTransform().setWidth(BASE_WIDGET_SIZE * scale);
            selectedWidget.getTransform().setHeight(BASE_WIDGET_SIZE * scale);
            sizeValueLabel.setText(String.format("%.1f", newVal.doubleValue()));
            overlayDirty = true;
        });

        onWidgetSelected(null);
        initializeHudPlayback(videoEngine, telemetry);
        startPreviewLoop();
    }

    private void initializeHudPlayback(VideoEngine videoEngine, Telemetry telemetry) {
        if (videoEngine == null || telemetry == null || !videoEngine.isOpen()) {
            return;
        }
        hudModel = new OverlayModel();
        hudView = new OverlayView(videoStage.getHudCanvas(), telemetry, hudModel);
        playback = new VideoPlayer(videoEngine, this::updateVideoFrame,
                error -> javafx.application.Platform.runLater(() -> {
                    if (orchestrator != null) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.ERROR,
                                "Video playback failed: " + error.getMessage());
                        alert.show();
                    }
                }), state -> updatePlayPauseButton(state));
        synchronizer = new TelemetrySynchronizer(telemetry, playback, hudModel, 0L);
        synchronizer.start();
        try {
            java.awt.image.BufferedImage firstFrame = videoEngine.getFrame();
            if (firstFrame != null) {
                videoStage.getImageView().setImage(SwingFXUtils.toFXImage(firstFrame, null));
            }
        } catch (Exception e) {
            logger.error("Failed to load the initial video frame", e);
        }
        double duration = videoEngine.getDuration();
        timelineSlider.setMin(0);
        timelineSlider.setMax(Math.max(0.0, duration));
        totalTimeLabel.setText(formatTime(duration));
        timelineSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (timelineSlider.isValueChanging() && playback != null) {
                playback.seek(newValue.doubleValue());
                synchronizer.update();
            }
        });
        updatePlayPauseButton(PlaybackState.STOPPED);
    }

    private void updateVideoFrame(VideoEngine.VideoFrame frame) {
        videoStage.getImageView().setImage(SwingFXUtils.toFXImage(frame.image(), null));
        if (!timelineSlider.isValueChanging()) {
            timelineSlider.setValue(frame.ptsSeconds());
        }
        currentTimeLabel.setText(formatTime(frame.ptsSeconds()));
    }

    private void updatePlayPauseButton(PlaybackState state) {
        if (playButton != null) {
            playButton.setText(state == PlaybackState.PLAYING ? "Pause" : "Play");
        }
    }

    private static String formatTime(double seconds) {
        int total = Math.max(0, (int) Math.round(seconds));
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private void startPreviewLoop() {
        Canvas overlayCanvas = videoStage.getOverlayCanvas();
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();

        previewTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!overlayDirty) {
                    return;
                }
                gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
                gc.save();
                gc.scale(overlayCanvas.getWidth() / sourceWidth,
                        overlayCanvas.getHeight() / sourceHeight);
                overlay.render(gc, now / 1000L);
                gc.restore();
                overlayDirty = false;
            }
        };

        previewTimer.start();
    }

    private void onWidgetSelected(OverlayWidget widget) {
        selectedWidget = widget;
        overlayDirty = true;

        boolean hasSelection = widget != null;
        propertyPanel.setDisable(!hasSelection);

        propertyPanelTitle.setText(hasSelection ? widget.getClass().getSimpleName() : "No selection");
        propertyPanelSubtitle.setText("");
        propertyPanelHint.setText(hasSelection ? "" : "Select a widget to edit it.");

        if (hasSelection) {
            double scale = widget.getTransform().getWidth() / BASE_WIDGET_SIZE;
            sizeSlider.setValue(scale * 5.0);
        }

        toggleVisibleButton.setText(hasSelection && widget.getTransform().isVisible() ? "Hide" : "Show");
        toggleLockButton.setText(hasSelection && widget.getTransform().isLocked() ? "Unlock" : "Lock");
        deleteWidgetButton.setDisable(!hasSelection);
        duplicateWidgetButton.setDisable(!hasSelection);
        toggleVisibleButton.setDisable(!hasSelection);
        toggleLockButton.setDisable(!hasSelection);
    }

    // ---- Timeline controls (stubs - wire to VideoPlayer later) ----

    @FXML
    private void onPlayPause(ActionEvent event) {
        if (playback != null) playback.togglePlayPause();
    }

    @FXML
    private void onMuteToggle(ActionEvent event) {
        if (playback != null) {
            playback.setMuted(!playback.isMuted());
            muteButton.setText(playback.isMuted() ? "Unmute" : "Mute");
        }
    }

    @FXML
    private void onAddMarker(ActionEvent event) {
        // TODO: add a marker at the current timeline position
    }

    @FXML
    private void onExpandTimeline(ActionEvent event) {
        // TODO: expand/collapse the timeline panel
    }

    // ---- Gauge sidebar (stubs) ----

    @FXML
    private void onAddGauge(ActionEvent event) {
        // TODO: open the gauge type picker / WidgetRegistry
    }

    @FXML
    private void onEditGauges(ActionEvent event) {
        // TODO: switch property panel into multi-edit mode
    }

    @FXML
    private void onSyncTelemetry(ActionEvent event) {
        // TODO: re-sync telemetry offset against video
    }

    @FXML
    private void onTrimGauge(ActionEvent event) {
        // TODO: trim the selected widget's visible time range
    }

    @FXML
    private void onPatterns(ActionEvent event) {
        // TODO: open saved layout patterns
    }

    @FXML
    private void onSelectGaugeType(ActionEvent event) {
        // Only Battery Voltage is implemented so far; every button adds one
        // as a placeholder until WidgetRegistry maps names -> widget types.
        Project project = ProjectManager.getInstance().getCurrentProject();
        Telemetry telemetry = project != null ? project.getTelemetry() : null;
        if (telemetry == null) {
            return;
        }

        BatteryVoltageWidget widget = new BatteryVoltageWidget(telemetry.batteryVoltage);
        widget.getTransform().setX(40);
        widget.getTransform().setY(40);
        overlay.addWidget(widget);
        onWidgetSelected(widget);
    }

    // ---- Property panel quick actions ----

    @FXML
    private void onToggleVisible(ActionEvent event) {
        if (selectedWidget == null) {
            return;
        }
        selectedWidget.getTransform().setVisible(!selectedWidget.getTransform().isVisible());
        toggleVisibleButton.setText(selectedWidget.getTransform().isVisible() ? "Hide" : "Show");
    }

    @FXML
    private void onToggleLock(ActionEvent event) {
        if (selectedWidget == null) {
            return;
        }
        Transform transform = selectedWidget.getTransform();
        transform.setLocked(!transform.isLocked());
        toggleLockButton.setText(transform.isLocked() ? "Unlock" : "Lock");
    }

    @FXML
    private void onDuplicateWidget(ActionEvent event) {
        if (!(selectedWidget instanceof BatteryVoltageWidget original)) {
            return;
        }

        Project project = ProjectManager.getInstance().getCurrentProject();
        Telemetry telemetry = project != null ? project.getTelemetry() : null;
        if (telemetry == null) {
            return;
        }

        BatteryVoltageWidget copy = new BatteryVoltageWidget(telemetry.batteryVoltage);
        copy.getTransform().setX(original.getTransform().getX() + 20);
        copy.getTransform().setY(original.getTransform().getY() + 20);
        copy.getTransform().setWidth(original.getTransform().getWidth());
        copy.getTransform().setHeight(original.getTransform().getHeight());

        overlay.addWidget(copy);
        onWidgetSelected(copy);
    }

    @FXML
    private void onColorTag(ActionEvent event) {
        // TODO: tag the selected widget with a color label
    }

    @FXML
    private void onDeleteWidget(ActionEvent event) {
        if (selectedWidget == null) {
            return;
        }
        overlay.removeWidget(selectedWidget);
        onWidgetSelected(null);
    }

    public void onContinue(ActionEvent event) {
        if (previewTimer != null) {
            previewTimer.stop();
        }
        if (synchronizer != null) synchronizer.close();
        if (hudView != null) hudView.close();
        if (playback != null) playback.dispose();
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project != null) {
            project.setCurrentStep(WizardStep.OVERLAY);
        }
        orchestrator.showExport();
    }
}