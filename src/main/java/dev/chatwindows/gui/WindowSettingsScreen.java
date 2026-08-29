package dev.chatwindows.gui;

import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Per-window appearance and geometry. Every window has its own copy of all of this. */
public class WindowSettingsScreen extends BaseSettingsScreen {

    private final ChatWindow window;

    private TextFieldWidget nameField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private TextFieldWidget colorField;

    public WindowSettingsScreen(Screen parent, ChatWindow window) {
        super(parent, Text.literal("Window settings - " + window.name));
        this.window = window;
    }

    @Override
    protected void init() {
        clearLabels();

        int leftLabel = this.width / 2 - 164;
        int leftWidget = this.width / 2 - 100;
        int rightWidget = this.width / 2 + 8;
        int fieldWidth = 88;
        int wideWidth = 156;
        int rowHeight = 22;
        int y = 34;

        // ---- row: name + anchor
        addLabel(leftLabel, y + 6, "Name");
        nameField = new TextFieldWidget(this.textRenderer, leftWidget, y, fieldWidth, 18, Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setText(window.name);
        nameField.setChangedListener(s -> window.name = s.isEmpty() ? "Chat" : s);
        addDrawableChild(nameField);

        addDrawableChild(CyclingButtonWidget.<ChatWindow.Anchor>builder(a -> Text.literal(a.display()))
                .values(ChatWindow.Anchor.values())
                .initially(window.anchor)
                .build(rightWidget, y, wideWidth, 18, Text.literal("Anchor"), (b, v) -> {
                    int left = window.resolveLeft(this.width);
                    int top = window.resolveTop(this.height);
                    window.anchor = v;
                    window.setAbsolute(left, top, this.width, this.height);
                }));
        y += rowHeight;

        // ---- row: width + height
        addLabel(leftLabel, y + 6, "Width");
        widthField = new TextFieldWidget(this.textRenderer, leftWidget, y, fieldWidth, 18, Text.literal("Width"));
        widthField.setText(String.valueOf(window.width));
        widthField.setChangedListener(s -> window.width = MathHelper.clamp(parseInt(s, window.width), 80, 1920));
        addDrawableChild(widthField);

        addLabel(rightWidget - 64, y + 6, "Height");
        heightField = new TextFieldWidget(this.textRenderer, rightWidget, y, fieldWidth, 18, Text.literal("Height"));
        heightField.setText(String.valueOf(window.height));
        heightField.setChangedListener(s -> window.height = MathHelper.clamp(parseInt(s, window.height), 40, 1080));
        addDrawableChild(heightField);
        y += rowHeight;

        // ---- row: scale + line spacing
        addDrawableChild(new SimpleSlider(leftWidget - 64, y, wideWidth, 18, "Scale",
                0.5, 2.5, window.scale, false, v -> window.scale = (float) v));
        addDrawableChild(new SimpleSlider(rightWidget, y, wideWidth, 18, "Line spacing",
                7, 20, window.lineSpacing, true, v -> window.lineSpacing = (int) Math.round(v)));
        y += rowHeight;

        // ---- row: background colour + closed opacity
        addLabel(leftLabel, y + 6, "BG hex");
        colorField = new TextFieldWidget(this.textRenderer, leftWidget, y, fieldWidth - 22, 18, Text.literal("Background"));
        colorField.setMaxLength(7);
        colorField.setText(window.backgroundColor);
        colorField.setChangedListener(s -> {
            if (ColorUtil.isValidHex(s)) window.backgroundColor = s.startsWith("#") ? s : "#" + s;
        });
        addDrawableChild(colorField);

        addDrawableChild(new SimpleSlider(rightWidget, y, wideWidth, 18, "Opacity (chat closed)",
                0, 255, window.backgroundAlpha, true, v -> window.backgroundAlpha = (int) Math.round(v)));
        y += rowHeight;

        // ---- row: open opacity + fade
        addDrawableChild(new SimpleSlider(leftWidget - 64, y, wideWidth, 18, "Opacity (chat open)",
                0, 255, window.backgroundAlphaFocused, true, v -> window.backgroundAlphaFocused = (int) Math.round(v)));
        addDrawableChild(new SimpleSlider(rightWidget, y, wideWidth, 18, "Fade out (s, 0 = never)",
                0, 120, window.fadeOutSeconds, true, v -> window.fadeOutSeconds = (int) Math.round(v)));
        y += rowHeight;

        // ---- row: history + tab bar
        addDrawableChild(new SimpleSlider(leftWidget - 64, y, wideWidth, 18, "Stored messages",
                20, 2000, window.maxMessages, true, v -> window.maxMessages = (int) Math.round(v)));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(window.showTabBar)
                .build(rightWidget, y, wideWidth, 18, Text.literal("Tab bar"), (b, v) -> window.showTabBar = v));
        y += rowHeight;

        // ---- row: shadow + visible
        addDrawableChild(CyclingButtonWidget.onOffBuilder(window.textShadow)
                .build(leftWidget - 64, y, wideWidth, 18, Text.literal("Text shadow"), (b, v) -> window.textShadow = v));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(window.visible)
                .build(rightWidget, y, wideWidth, 18, Text.literal("Visible"), (b, v) -> window.visible = v));
        y += rowHeight + 4;

        // ---- tabs
        addDrawableChild(ButtonWidget.builder(Text.literal("Tabs, filters & highlights..."), b -> {
            if (this.client != null) this.client.setScreen(new TabListScreen(this, window));
        }).dimensions(this.width / 2 - 158, y, 316, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - 100, Math.min(y, this.height - 28), 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (colorField != null) {
            int swatchX = colorField.getX() + colorField.getWidth() + 4;
            int swatchY = colorField.getY() + 2;
            context.fill(swatchX, swatchY, swatchX + 14, swatchY + 14,
                    ColorUtil.argb(255, ColorUtil.parseHex(window.backgroundColor, 0x000000)));
            context.fill(swatchX, swatchY, swatchX + 14, swatchY + 1, 0xFF555555);
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
