package com.nikipo.ardupilotoverlay.telemetry;

import com.nikipo.ardupilotoverlay.telemetry.dataType.Interpolation;
import com.nikipo.ardupilotoverlay.telemetry.dataType.TimedValue;

import java.util.ArrayList;
import java.util.List;

public class ContinuousTimeSeries<T extends Number> {

    private final List<TimedValue<T>> samples = new ArrayList<>();

    private final Interpolation interpolation;

    private int cursor = 0;

    public ContinuousTimeSeries(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void add(long timeUs, T value) {
        samples.add(new TimedValue<>(timeUs, value));
    }

    public int size() {
        return samples.size();
    }

    public T latest() {
        return samples.get(samples.size() - 1).value();
    }

    public double sample(long timeUs) {

        if (samples.isEmpty())
            throw new IllegalStateException();

        if (samples.size() == 1)
            return samples.get(0).value().doubleValue();

        if (timeUs < samples.get(cursor).timeUs())
            cursor = binarySearch(timeUs);

        while (cursor + 1 < samples.size()
                && samples.get(cursor + 1).timeUs() <= timeUs) {
            cursor++;
        }

        TimedValue<T> a = samples.get(cursor);

        if (cursor == samples.size() - 1)
            return a.value().doubleValue();

        TimedValue<T> b = samples.get(cursor + 1);

        switch (interpolation) {

            case NONE:
                return a.value().doubleValue();

            case LINEAR:
                return interpolateLinear(a, b, timeUs);

            case ANGLE:
                return interpolateAngle(a, b, timeUs);

            default:
                throw new IllegalStateException();
        }
    }

    private double interpolateLinear(
            TimedValue<T> a,
            TimedValue<T> b,
            long time) {

        double ratio =
                (double) (time - a.timeUs()) /
                        (double) (b.timeUs() - a.timeUs());

        return a.value().doubleValue() +
                ratio *
                        (b.value().doubleValue() -
                                a.value().doubleValue());
    }

    private double interpolateAngle(
            TimedValue<T> a,
            TimedValue<T> b,
            long time) {

        double v0 = a.value().doubleValue();
        double v1 = b.value().doubleValue();

        double diff = v1 - v0;

        if (diff > 180)
            diff -= 360;

        if (diff < -180)
            diff += 360;

        double ratio =
                (double) (time - a.timeUs()) /
                        (double) (b.timeUs() - a.timeUs());

        double angle = v0 + ratio * diff;

        if (angle < 0)
            angle += 360;

        if (angle >= 360)
            angle -= 360;

        return angle;
    }

    private int binarySearch(long timeUs) {

        int lo = 0;
        int hi = samples.size() - 1;

        while (lo <= hi) {

            int mid = (lo + hi) >>> 1;

            long t = samples.get(mid).timeUs();

            if (t < timeUs)
                lo = mid + 1;
            else if (t > timeUs)
                hi = mid - 1;
            else
                return mid;
        }

        return Math.max(0, hi);
    }

}