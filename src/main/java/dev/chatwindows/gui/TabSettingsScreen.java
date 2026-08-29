package dev.chatwindows.gui;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ChatWindow;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Settings for one tab: name, catch-all toggle, send prefix, filters, highlights. */
public class TabSettingsScreen extends BaseSettingsScreen {

    private final ChatWindow window;
    private final ChatTab tab;

    public TabSettingsScreen(Screen parent, ChatWindow window, ChatTab tab) {
        super(parent, Text.literal("Tab - " + tab.name));
        this.window = window;
        this.tab = tab;
    }

    @Override
    protected void init() {
        clearLabels();
        int cx = this.width / 2;
        int y = 40;

        addLabel(cx - 158, y + 6, "Tab name");
        TextFieldWidget nameField = new TextFieldWidget(this.textRenderer, cx - 90, y, 246, 18, Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setText(tab.name);
        nameField.setChangedListener(s -> tab.name = s.isEmpty() ? "Tab" : s);
        addDrawableChild(nameField);
        y += 24;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(tab.receiveAll)
                .build(cx - 158, y, 314, 20, Text.literal("Receive all messages (ignore filters)"),
                        (b, v) -> tab.receiveAll = v));
        y += 24;

        addLabel(cx - 158, y + 6, "Send prefix");
        TextFieldWidget prefixField = new TextFieldWidget(this.textRenderer, cx - 90, y, 246, 18, Text.literal("Prefix"));
        prefixField.setMaxLength(32);
        prefixField.setText(tab.sendPrefix);
        prefixField.setChangedListener(s -> tab.sendPrefix = s);
        addDrawableChild(prefixField);
        y += 22;
        addLabel(cx - 158, y, "\u00a78Used by the \"Open chat in active tab\" keybind, e.g. \"/pc \"", 0xFF777777);
        y += 18;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Filters (" + tab.filters.size() + ")..."),
                        b -> {
                            if (this.client != null) this.client.setScreen(new FilterListScreen(this, tab));
                        })
                .dimensions(cx - 158, y, 155, 20).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Highlights (" + tab.highlights.size() + ")..."),
                        b -> {
                            if (this.client != null) this.client.setScreen(new HighlightListScreen(this, tab));
                        })
                .dimensions(cx + 1, y, 155, 20).build());
        y += 26;

        addDrawableChild(ButtonWidget.builder(Text.literal("Clear this tab's messages"), b -> tab.clear())
                .dimensions(cx - 158, y, 314, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(cx - 100, this.height - 28, 200, 20).build());
    }
}
