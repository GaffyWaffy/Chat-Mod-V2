package dev.chatwindows.gui;

import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Shared plumbing: labels, a parent screen, and save-on-close. */
public abstract class BaseSettingsScreen extends Screen {

    protected final Screen parent;
    private final List<Label> labels = new ArrayList<>();

    protected record Label(int x, int y, String text, int color) {}

    protected BaseSettingsScreen(Screen parent, Text title) {
        super(title);
        this.parent = parent;
    }

    protected void clearLabels() {
        labels.clear();
    }

    protected void addLabel(int x, int y, String text) {
        labels.add(new Label(x, y, text, 0xFFA0A0A0));
    }

    protected void addLabel(int x, int y, String text, int color) {
        labels.add(new Label(x, y, text, color));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        for (Label label : labels) {
            context.drawTextWithShadow(this.textRenderer, label.text(), label.x(), label.y(), label.color());
        }
    }

    @Override
    public void close() {
        ConfigManager.save();
        ChatRouter.get().rebuildAll();
        if (this.client != null) this.client.setScreen(parent);
    }
}
