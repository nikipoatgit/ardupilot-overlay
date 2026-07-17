package com.nikipo.ardupilotoverlay.telemetry.dataType;

public record TimedValue<T>(
        long timeUs,
        T value
) {}
