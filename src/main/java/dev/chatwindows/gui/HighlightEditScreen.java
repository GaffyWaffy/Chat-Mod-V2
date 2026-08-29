package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.config.HighlightRule;
import dev.chatwindows.util.ColorUtil;
import dev.chatwindows.util.MatchType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class HighlightEditScreen extends BaseSettingsScreen {

    private static final String[] PRESETS = {
            "#FFAA00", "#FF5555", "#55FF55", "#55FFFF", "#FF55FF", "#5555FF", "#FFFF55", "#FFFFFF"
    };

    private final ChatTab tab;
    private final HighlightRule rule;
    private TextFieldWidget colorField;

    public HighlightEditScreen(Screen parent, ChatTab tab, HighlightRule rule) {
        super(parent, Text.literal("Edit highlight"));
        this.tab = tab;
        this.rule = rule;
    }

    @Override
    protected void init() {
        clearLabels();
        int cx = this.width / 2;
        int y = 36;

        addLabel(cx - 158, y + 6, "Keyword / phrase");
        TextFieldWidget patternField = new TextFieldWidget(this.textRenderer, cx - 50, y, 206, 18, Text.literal("Pattern"));
        patternField.setMaxLength(200);
        patternField.setText(rule.pattern);
        patternField.setChangedListener(s -> rule.pattern = s);
        addDrawableChild(patternField);
        setInitialFocus(patternField);
        y += 24;

        addLabel(cx - 158, y + 6, "Colour (hex)");
        colorField = new TextFieldWidget(this.textRenderer, cx - 50, y, 80, 18, Text.literal("Hex"));
        colorField.setMaxLength(7);
        colorField.setText(rule.color);
        colorField.setChangedListener(s -> {
            if (ColorUtil.isValidHex(s)) rule.color = s.startsWith("#") ? s : "#" + s;
        });
        addDrawableChild(colorField);
        y += 24;

        // quick presets
        int px = cx - 50;
        for (String preset : PRESETS) {
            addDrawableChild(ButtonWidget.builder(Text.literal(" "), b -> {
                rule.color = preset;
                colorField.setText(preset);
            }).dimensions(px, y, 18, 18).build());
            px += 20;
        }
        addLabel(cx - 158, y + 5, "Presets");
        y += 24;

        addDrawableChild(new SimpleSlider(cx - 158, y, 314, 18, "Highlight opacity",
                0, 255, rule.alpha, true, v -> rule.alpha = (int) Math.round(v)));
        y += 24;

        addDrawableChild(CyclingButtonWidget.builder((MatchType m) -> Text.literal(m.display()), rule.match)
                .values(MatchType.values())
                .build(cx - 158, y, 155, 20, Text.literal("Match"), (b, v) -> rule.match = v));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(rule.caseSensitive)
                .build(cx + 1, y, 155, 20, Text.literal("Case sensitive"), (b, v) -> rule.caseSensitive = v));
        y += 24;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(rule.enabled)
                .build(cx - 158, y, 314, 20, Text.literal("Enabled"), (b, v) -> rule.enabled = v));
        y += 30;

        addLabel(cx - 158, y, "\u00a77Preview:");

        addDrawableChild(ButtonWidget.builder(Text.literal("\u00a7cDelete"), b -> {
            tab.highlights.remove(rule);
            ConfigManager.save();
            close();
        }).dimensions(cx - 158, this.height - 28, 155, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx + 1, this.height - 28, 155, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (colorField != null) {
            int sx = colorField.getX() + colorField.getWidth() + 6;
            context.fill(sx, colorField.getY(), sx + 18, colorField.getY() + 18,
                    ColorUtil.argb(255, ColorUtil.parseHex(rule.color, 0xFFAA00)));
        }

        // Live preview of exactly how a highlighted line will look.
        int px = this.width / 2 - 158;
        int py = this.height - 62;
        context.fill(px, py - 2, px + 314, py + 22, 0x80000000);
        context.fill(px, py + 4, px + 314, py + 14, rule.argb());
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("\u00a7b<Steve> \u00a7fhighlighted line keeps its own text colours"),
                px + 3, py + 5, 0xFFFFFFFF);
    }
}
