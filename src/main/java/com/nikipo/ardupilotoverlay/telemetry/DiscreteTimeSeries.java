package com.nikipo.ardupilotoverlay.telemetry;

import com.nikipo.ardupilotoverlay.telemetry.dataType.TimedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscreteTimeSeries<T> {

    private final List<TimedValue<T>> samples = new ArrayList<>();

    private int cursor = 0;

    public void add(long timeUs, T value) {
        samples.add(new TimedValue<>(timeUs, value));
    }

    public int size() {
        return samples.size();
    }


    public List<TimedValue<T>> getBuffer() {
        return Collections.unmodifiableList(samples);
    }

    public T sample(long timeUs) {

        if (samples.isEmpty())
            throw new IllegalStateException();

        if (timeUs < samples.get(cursor).timeUs())
            cursor = binarySearch(timeUs);

        while (cursor + 1 < samples.size()
                && samples.get(cursor + 1).timeUs() <= timeUs) {
            cursor++;
        }

        return samples.get(cursor).value();
    }

    public T latest() {
        return samples.get(samples.size() - 1).value();
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