package dev.chatwindows.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/** Small helper so the settings screens can use sliders without boilerplate. */
public class SimpleSlider extends SliderWidget {

    private final String label;
    private final double min;
    private final double max;
    private final boolean integer;
    private final DoubleConsumer onChange;

    public SimpleSlider(int x, int y, int width, int height, String label,
                        double min, double max, double value, boolean integer, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), (value - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.integer = integer;
        this.onChange = onChange;
        updateMessage();
    }

    public double getRealValue() {
        return min + (max - min) * this.value;
    }

    @Override
    protected void updateMessage() {
        if (label == null) return;
        String v = integer
                ? String.valueOf(Math.round(getRealValue()))
                : String.format(Locale.ROOT, "%.2f", getRealValue());
        setMessage(Text.literal(label + ": " + v));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) onChange.accept(getRealValue());
    }
}
