package com.nikipo.ardupilotoverlay.overlay.widgets;

import com.nikipo.ardupilotoverlay.overlay.OverlayWidget;
import com.nikipo.ardupilotoverlay.overlay.WidgetProperty;
import com.nikipo.ardupilotoverlay.overlay.render.GaugeRenderer;
import com.nikipo.ardupilotoverlay.telemetry.ContinuousTimeSeries;

import java.util.List;

/**
 * Minimal end-to-end example widget: reads Telemetry.batteryVoltage and
 * displays it with a GaugeRenderer. Every other widget (Speed, Altitude,
 * Heading, ...) follows this same pattern - only sampleValue() and the
 * default renderer/properties change.
 */
public class BatteryVoltageWidget extends OverlayWidget {

    private final ContinuousTimeSeries<Float> batteryVoltage;

    public BatteryVoltageWidget(ContinuousTimeSeries<Float> batteryVoltage) {
        super(new GaugeRenderer(9.0, 13.0, "V"));
        this.batteryVoltage = batteryVoltage;
        getTransform().setWidth(140);
        getTransform().setHeight(140);
    }

    @Override
    protected double sampleValue(long timeUs) {
        if (batteryVoltage.size() == 0) {
            return 0;
        }
        return batteryVoltage.sample(timeUs);
    }

    @Override
    public List<WidgetProperty<?>> getWidgetProperties() {
        return List.of();
    }
}