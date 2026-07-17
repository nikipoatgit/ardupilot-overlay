package com.nikipo.ardupilotoverlay.telemetry;

import java.util.EnumSet;

import com.nikipo.ardupilotoverlay.telemetry.dataType.Home;
import com.nikipo.ardupilotoverlay.telemetry.dataType.Interpolation;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TelemetryCapability;

public class Telemetry {

    private final EnumSet<TelemetryCapability> capabilities =
            EnumSet.noneOf(TelemetryCapability.class);

    public final ContinuousTimeSeries<Float> roll = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> pitch = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> heading = new ContinuousTimeSeries<>(Interpolation.ANGLE);

    public final ContinuousTimeSeries<Float> latitude = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> longitude = new ContinuousTimeSeries<>(Interpolation.LINEAR);

    public final ContinuousTimeSeries<Float> altitude = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> relativeAltitude = new ContinuousTimeSeries<>(Interpolation.LINEAR);

    public final ContinuousTimeSeries<Float> groundSpeed = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> airSpeed = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> verticalSpeed = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> climbRate = new ContinuousTimeSeries<>(Interpolation.LINEAR);

    public final ContinuousTimeSeries<Float> batteryVoltage = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> batteryCurrent = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Integer> batteryRemaining = new ContinuousTimeSeries<>(Interpolation.NONE);

    public final ContinuousTimeSeries<Integer> satellites = new ContinuousTimeSeries<>(Interpolation.NONE);
    public final ContinuousTimeSeries<Integer> gpsFix = new ContinuousTimeSeries<>(Interpolation.NONE);

    public final ContinuousTimeSeries<Integer> rssi = new ContinuousTimeSeries<>(Interpolation.NONE);

    public final ContinuousTimeSeries<Float> windSpeed = new ContinuousTimeSeries<>(Interpolation.LINEAR);
    public final ContinuousTimeSeries<Float> windDirection = new ContinuousTimeSeries<>(Interpolation.ANGLE);

    public final DiscreteTimeSeries<Boolean> armed = new DiscreteTimeSeries<>();
    public final DiscreteTimeSeries<String> flightMode = new DiscreteTimeSeries<>();

    private Home home;


    public void addCapability(TelemetryCapability capability) {
        capabilities.add(capability);
    }

    public boolean hasCapability(TelemetryCapability capability) {
        return capabilities.contains(capability);
    }

    public EnumSet<TelemetryCapability> getCapabilities() {
        return EnumSet.copyOf(capabilities);
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
    }

}