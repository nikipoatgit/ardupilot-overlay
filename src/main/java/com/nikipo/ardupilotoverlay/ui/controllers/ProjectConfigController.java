package com.nikipo.ardupilotoverlay.ui.controllers;

import com.nikipo.ardupilotoverlay.logging.AppLogger;
import com.nikipo.ardupilotoverlay.project.Project;
import com.nikipo.ardupilotoverlay.project.ProjectManager;
import com.nikipo.ardupilotoverlay.ui.Navigable;
import com.nikipo.ardupilotoverlay.ui.UIOrchestrator;
import com.nikipo.ardupilotoverlay.ui.VideoStage;
import com.nikipo.ardupilotoverlay.ui.WizardStep;
import com.nikipo.ardupilotoverlay.videoplayer.VideoEngine;
import com.nikipo.ardupilotoverlay.videoplayer.VideoInfo;
import com.nikipo.ardupilotoverlay.videoplayer.VideoPlayer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

public class ProjectConfigController implements Navigable {
    private static final Logger logger = AppLogger.getLogger(ProjectConfigController.class);

    private final ColorAdjust colorAdjust = new ColorAdjust();

    private UIOrchestrator orchestrator;
    private Project project;
    private VideoInfo videoInfo;
    private VideoPlayer playback;
    private VideoEngine videoEngine;
    private double durationSeconds;
    private WritableImage videoImage;
    private PixelBuffer<ByteBuffer> videoPixelBuffer;
    private ByteBuffer videoBackingBuffer;
    private boolean scrubSliderDragged;

    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane editorSplitPane;

    @FXML private StackPane previewStack;
    @FXML private VideoStage videoStage;
    private ImageView videoImageView;
    @FXML private Label previewMessageLabel;

    @FXML private VBox timelineBox;
    @FXML private HBox rulerBox;
    @FXML private Label hoverTimeLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Slider scrubSlider;
    @FXML private StackPane inOutStack;
    @FXML private Rectangle inOutTrack;
    @FXML private Label inOutLabel;

    @FXML private HBox transportBox;
    @FXML private Button playPauseButton;
    @FXML private Button setStartButton;
    @FXML private Button setEndButton;
    @FXML private Button stepBackButton;
    @FXML private Button stepForwardButton;
    @FXML private Pane transportSpacer;
    @FXML private Button muteButton;
    @FXML private Button prevMarkerButton;
    @FXML private Button nextMarkerButton;
    @FXML private Button nextButton;

    @FXML private ScrollPane propertiesScrollPane;
    @FXML private VBox propertiesBox;
    @FXML private TextField projectNameField;

    @FXML private Slider videoRotationSlider;
    @FXML private Slider videoOpacitySlider;
    @FXML private Slider brightnessSlider;
    @FXML private Slider contrastSlider;
    @FXML private Slider saturationSlider;
    @FXML private Slider temperatureSlider;
    @FXML private Slider tintSlider;
    @FXML private Button resetGradingButton;

    @FXML
    private void initialize() {
        project = ProjectManager.getInstance().getCurrentProject();
        videoInfo = project == null ? null : project.getVideoInfo();
        videoEngine = ProjectManager.getInstance().getVideoEngine();
        durationSeconds = videoInfo == null ? 0.0 : videoInfo.getDurationSeconds();

        initializeProject();
        initializeVideoPreview();
        initializePlayback();
        initializeTimeline();
        initializeTransport();
        initializeVideoControls();
        initializeColorGrading();
        updateVideoAvailability();
    }

    @Override
    public void setOrchestrator(UIOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    private void initializeProject() {
        if (project == null) {
            projectNameField.setText("");
            projectNameField.setDisable(true);
            return;
        }

        projectNameField.setText(project.getConfig().getProjectName());
        projectNameField.textProperty().addListener((obs, oldValue, newValue) ->
                project.getConfig().setProjectName(newValue));
    }

    private void initializeVideoPreview() {
        videoImageView = videoStage.getImageView();
        videoImageView.setEffect(colorAdjust);
        if (videoEngine != null && videoEngine.isOpen()) {
            videoStage.setMediaAspect(videoEngine.getSourceAspect());
        }
        javafx.application.Platform.runLater(() -> {
            if (mainSplitPane.getWidth() > 0) {
                mainSplitPane.setDividerPositions(
                        Math.max(0.0, Math.min(1.0, 1.0 - 380.0 / mainSplitPane.getWidth())));
            }
            if (editorSplitPane.getHeight() > 0) {
                editorSplitPane.setDividerPositions(
                        Math.max(0.0, Math.min(1.0, 1.0 - 160.0 / editorSplitPane.getHeight())));
            }
        });
    }

    private void initializePlayback() {
        if (project == null || project.getVideoFile() == null) {
            return;
        }

        if (videoEngine == null || !videoEngine.isOpen()) {
            return;
        }

        playback = new VideoPlayer(
                videoEngine,
                this::updateVideoPixels,
                this::handlePlaybackError,
                state -> updatePlayPauseButton()
        );
        if (videoInfo != null) {
            playback.setMuted(videoInfo.isMuted());
        }
        playback.seek(0);
        updateVideoAvailability();
    }

    private void updateVideoPixels(com.nikipo.ardupilotoverlay.videoplayer.VideoEngine.VideoFrame frame) {
            if (videoPixelBuffer == null
                    || videoImage == null
                    || videoImage.getWidth() != frame.image().getWidth()
                    || videoImage.getHeight() != frame.image().getHeight()) {
                videoBackingBuffer = ByteBuffer.allocateDirect(
                        frame.image().getWidth() * frame.image().getHeight() * 4);
                videoPixelBuffer = new PixelBuffer<>(
                        frame.image().getWidth(),
                        frame.image().getHeight(),
                        videoBackingBuffer,
                        PixelFormat.getByteBgraPreInstance());
                videoImage = new WritableImage(videoPixelBuffer);
                videoStage.getImageView().setImage(videoImage);
            }

            videoPixelBuffer.updateBuffer(buffer -> {
                videoBackingBuffer.clear();
                videoBackingBuffer.put(frame.bgraPrePixels());
                videoBackingBuffer.flip();
                return null;
            });
            if (!scrubSlider.isValueChanging()) {
                scrubSlider.setValue(clamp(frame.ptsSeconds(), 0, durationSeconds));
            }
    }

    private void initializeTimeline() {
        scrubSlider.setMin(0);
        scrubSlider.setMax(durationSeconds);
        scrubSlider.setValue(0);
        updateTrimLabels();

        inOutTrack.widthProperty().bind(inOutStack.widthProperty().subtract(16));

        scrubSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (scrubSlider.isValueChanging()) {
                inOutLabel.setText("Current " + formatTime(newVal.doubleValue()));
            }
        });

        scrubSlider.setOnMousePressed(e -> scrubSliderDragged = false);
        scrubSlider.setOnMouseClicked(this::seekToMousePosition);
        scrubSlider.setOnMouseMoved(this::showScrubHover);
        scrubSlider.setOnMouseDragged(event -> {
            scrubSliderDragged = true;
            showScrubHover(event);
        });
        scrubSlider.setOnMouseReleased(event -> {
            if (scrubSliderDragged) {
                seekToSliderValue();
            }
        });
        scrubSlider.setOnMouseExited(e -> hoverTimeLabel.setVisible(false));
    }

    private void initializeTransport() {
        playPauseButton.setText("Play");
        playPauseButton.setOnAction(e -> {
            if (playback == null) {
                return;
            }

            playback.togglePlayPause();
            updatePlayPauseButton();
        });

        stepBackButton.setOnAction(e -> stepFrames(-1));
        stepForwardButton.setOnAction(e -> stepFrames(1));
        muteButton.setOnAction(e -> toggleMute());
        setStartButton.setOnAction(e -> setStartAtCurrentTime());
        setEndButton.setOnAction(e -> setEndAtCurrentTime());
        prevMarkerButton.setOnAction(e -> seekToSeconds(videoInfo == null ? 0.0 : videoInfo.getStartSeconds()));
        nextMarkerButton.setOnAction(e -> seekToSeconds(videoInfo == null ? durationSeconds : videoInfo.getEndSeconds()));
        nextButton.setOnAction(e -> goToTelemetry());

        updateTransportAvailability();
    }

    private void initializeVideoControls() {
        if (videoInfo != null) {
            videoRotationSlider.setValue(videoInfo.getRotationDegrees());
            videoOpacitySlider.setValue(videoInfo.getOpacityPercent());
        }

        videoRotationSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            videoStage.setRotationDegrees(value);
            if (videoInfo != null) {
                videoInfo.setRotationDegrees(value);
            }
        });

        videoOpacitySlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            videoStage.getImageView().setOpacity(clamp(value / 100.0, 0, 1));
            if (videoInfo != null) {
                videoInfo.setOpacityPercent(value);
            }
        });
    }

    private void initializeColorGrading() {
        resetGradingButton.setOnAction(e -> resetGradingControls());

        brightnessSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyColorGrading());
        contrastSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyColorGrading());
        saturationSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyColorGrading());
        temperatureSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyColorGrading());
        tintSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyColorGrading());

        if (videoInfo == null) {
            resetGradingControls();
        } else {
            brightnessSlider.setValue(videoInfo.getBrightness());
            contrastSlider.setValue(videoInfo.getContrast());
            saturationSlider.setValue(videoInfo.getSaturation());
            temperatureSlider.setValue(videoInfo.getTemperature());
            tintSlider.setValue(videoInfo.getTint());
            applyColorGrading();
        }
    }

    private void resetGradingControls() {
        videoRotationSlider.setValue(0);
        videoOpacitySlider.setValue(100);
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        saturationSlider.setValue(100);
        temperatureSlider.setValue(0);
        tintSlider.setValue(0);
        applyColorGrading();
    }

    private void applyColorGrading() {
        if (videoInfo != null) {
            videoInfo.setBrightness(brightnessSlider.getValue());
            videoInfo.setContrast(contrastSlider.getValue());
            videoInfo.setSaturation(saturationSlider.getValue());
            videoInfo.setTemperature(temperatureSlider.getValue());
            videoInfo.setTint(tintSlider.getValue());
        }

        colorAdjust.setBrightness(clamp(brightnessSlider.getValue() / 100.0, -1, 1));
        colorAdjust.setContrast(clamp((contrastSlider.getValue() - 100.0) / 200.0, -1, 1));
        colorAdjust.setSaturation(clamp((saturationSlider.getValue() - 100.0) / 100.0, -1, 1));

        double hueShift = (temperatureSlider.getValue() + tintSlider.getValue()) / 400.0;
        colorAdjust.setHue(clamp(hueShift, -1, 1));
    }

    private void updateVideoAvailability() {
        boolean hasVideo = playback != null && videoEngine != null && videoEngine.isOpen();

        previewMessageLabel.setVisible(!hasVideo);
        previewMessageLabel.setManaged(!hasVideo);
        videoStage.getImageView().setVisible(hasVideo);

        scrubSlider.setDisable(!hasVideo);
        playPauseButton.setDisable(!hasVideo);
        stepBackButton.setDisable(!hasVideo);
        stepForwardButton.setDisable(!hasVideo);
        muteButton.setDisable(!hasVideo || !playback.hasAudio());
        updateTransportAvailability();
        updateMuteButton();
    }

    private void seekToSliderValue() {
        if (playback == null) {
            return;
        }

        double seconds = clamp(scrubSlider.getValue(), 0, durationSeconds);
        playback.seek(seconds);
        updatePlayPauseButton();
    }

    private void seekToMousePosition(MouseEvent event) {
        if (scrubSliderDragged
                || playback == null || durationSeconds <= 0 || scrubSlider.getWidth() <= 0) {
            return;
        }

        double seconds = secondsFromTimelinePosition(event);
        scrubSlider.setValue(seconds);
        playback.seek(seconds);
        updatePlayPauseButton();
        event.consume();
    }

    private void toggleMute() {
        if (playback == null) {
            return;
        }

        boolean muted = !playback.isMuted();
        playback.setMuted(muted);
        if (videoInfo != null) {
            videoInfo.setMuted(muted);
        }
        updateMuteButton();
    }

    private void stepFrames(int frames) {
        if (playback == null) {
            return;
        }

        playback.stepFrames(frames);
        updatePlayPauseButton();
    }

    private void setStartAtCurrentTime() {
        if (videoInfo == null || !hasOpenVideo()) {
            return;
        }

        videoInfo.setStartSeconds(getCurrentPreviewSeconds());
        updateTrimLabels();
    }

    private void setEndAtCurrentTime() {
        if (videoInfo == null || !hasOpenVideo()) {
            return;
        }

        videoInfo.setEndSeconds(getCurrentPreviewSeconds());
        updateTrimLabels();
    }

    private double getCurrentPreviewSeconds() {
        return clamp(scrubSlider.getValue(), 0, durationSeconds);
    }

    private void seekToSeconds(double seconds) {
        if (playback == null) {
            return;
        }

        double clampedSeconds = clamp(seconds, 0, durationSeconds);
        scrubSlider.setValue(clampedSeconds);
        playback.seek(clampedSeconds);
        updatePlayPauseButton();
    }

    private void updateTrimLabels() {
        double start = videoInfo == null ? 0.0 : videoInfo.getStartSeconds();
        double end = videoInfo == null ? durationSeconds : videoInfo.getEndSeconds();

        startTimeLabel.setText(formatTime(start));
        endTimeLabel.setText(formatTime(end));
        inOutLabel.setText("Start " + formatTime(start) + "   End " + formatTime(end));
    }

    private void updateTransportAvailability() {
        boolean hasVideo = hasOpenVideo();

        setStartButton.setDisable(!hasVideo);
        setEndButton.setDisable(!hasVideo);
        prevMarkerButton.setDisable(!hasVideo);
        nextMarkerButton.setDisable(!hasVideo);
    }

    private boolean hasOpenVideo() {
        return playback != null && videoEngine != null && videoEngine.isOpen();
    }


    private void goToTelemetry() {
        project.setCurrentStep(WizardStep.PROJECT);
        logger.info("VideoInfo:"
                        + "\nDuration: {}"
                        + "\nStart: {}"
                        + "\nEnd: {}"
                        + "\nMuted: {}"
                        + "\nRotation: {}"
                        + "\nOpacity: {}"
                        + "\nBrightness: {}"
                        + "\nContrast: {}"
                        + "\nSaturation: {}"
                        + "\nTemperature: {}"
                        + "\nTint: {}",
                videoInfo.getDurationSeconds(),
                videoInfo.getStartSeconds(),
                videoInfo.getEndSeconds(),
                videoInfo.isMuted(),
                videoInfo.getRotationDegrees(),
                videoInfo.getOpacityPercent(),
                videoInfo.getBrightness(),
                videoInfo.getContrast(),
                videoInfo.getSaturation(),
                videoInfo.getTemperature(),
                videoInfo.getTint());

        if (playback != null) {
            playback.stop();
        }

        orchestrator.showTelemetrySelector();
    }

    private void showScrubHover(MouseEvent event) {
        if (durationSeconds <= 0 || scrubSlider.getWidth() <= 0) {
            hoverTimeLabel.setVisible(false);
            return;
        }

        double seconds = secondsFromTimelinePosition(event);

        hoverTimeLabel.setText(formatTime(seconds));
        hoverTimeLabel.setTranslateX(event.getX() - (hoverTimeLabel.getWidth() / 2.0));
        hoverTimeLabel.setTranslateY(-22);
        hoverTimeLabel.setVisible(true);
    }

    private double secondsFromTimelinePosition(MouseEvent event) {
        double fraction = clamp(event.getX() / scrubSlider.getWidth(), 0, 1);
        return fraction * durationSeconds;
    }

    private void updatePlayPauseButton() {
        playPauseButton.setText(playback != null && playback.isPlaying() ? "Pause" : "Play");
    }

    private void updateMuteButton() {
        muteButton.setText(playback != null && playback.isMuted() ? "Unmute" : "Mute");
    }

    private void handlePlaybackError(Throwable error) {
        logger.error("Video playback error", error);

        if (playback != null) {
            playback.stop();
        }

        updatePlayPauseButton();
        updateVideoAvailability();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatTime(double seconds) {
        int totalSeconds = (int) Math.max(0, Math.floor(seconds));
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int remainingSeconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
        }

        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}
