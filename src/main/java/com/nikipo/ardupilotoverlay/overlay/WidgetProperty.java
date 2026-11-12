package com.nikipo.ardupilotoverlay.overlay;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A single editable property, exposed by a widget, its Transform, or its
 * Renderer. The Property Panel collects these from all three sources and
 * never needs to know the concrete widget type (see doc: "Property panel
 * never checks widget types").
 */
public class WidgetProperty<T> {

    private final String label;
    private final Class<T> type;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    public WidgetProperty(String label, Class<T> type, Supplier<T> getter, Consumer<T> setter) {
        this.label = label;
        this.type = type;
        this.getter = getter;
        this.setter = setter;
    }

    public String getLabel() { return label; }
    public Class<T> getType() { return type; }
    public T get() { return getter.get(); }
    public void set(T value) { setter.accept(value); }
}