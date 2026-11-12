package com.nikipo.ardupilotoverlay.overlay;

import javafx.beans.property.*;

public final class OverlayModel {
    public final DoubleProperty roll = new SimpleDoubleProperty();
    public final DoubleProperty pitch = new SimpleDoubleProperty();
    public final DoubleProperty heading = new SimpleDoubleProperty();
    public final DoubleProperty relativeAltitude = new SimpleDoubleProperty();
    public final DoubleProperty altitude = new SimpleDoubleProperty();
    public final DoubleProperty groundSpeed = new SimpleDoubleProperty();
    public final DoubleProperty airSpeed = new SimpleDoubleProperty();
    public final DoubleProperty batteryVoltage = new SimpleDoubleProperty(Double.NaN);
    public final DoubleProperty batteryCurrent = new SimpleDoubleProperty(Double.NaN);
    public final IntegerProperty batteryRemaining = new SimpleIntegerProperty(-1);
    public final IntegerProperty satellites = new SimpleIntegerProperty(-1);
    public final StringProperty gpsFix = new SimpleStringProperty("--");
    public final BooleanProperty armed = new SimpleBooleanProperty();
    public final StringProperty flightMode = new SimpleStringProperty("--");
    public final DoubleProperty homeDistance = new SimpleDoubleProperty(Double.NaN);
    public final DoubleProperty homeBearing = new SimpleDoubleProperty(Double.NaN);
    public final StringProperty position = new SimpleStringProperty("--");
    public final LongProperty telemetryTimeUs = new SimpleLongProperty();
}
