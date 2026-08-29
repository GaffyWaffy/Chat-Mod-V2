package dev.chatwindows.gui;

import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.config.ChatWindowsConfig;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.render.ChatWindowRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * The drag-and-drop layout editor. Left-drag moves a window, dragging the
 * bottom-right corner resizes it, right-click opens that window's settings.
 */
public class ChatLayoutScreen extends Screen {

    private static final int SNAP = 6;

    private final Screen parent;
    private ChatWindow selected;

    private int dragMode = 0; // 0 = none, 1 = move, 2 = resize
    private double dragOffsetX, dragOffsetY;
    private double dragStartX, dragStartY;
    private int startWidth, startHeight, startLeft, startTop;

    public ChatLayoutScreen(Screen parent) {
        super(Text.literal("Chat Windows - Layout"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        ChatWindowsConfig cfg = ConfigManager.get();
        if (selected == null && !cfg.windows.isEmpty()) selected = cfg.windows.get(cfg.windows.size() - 1);

        int y = this.height - 26;
        int w = 78;
        int gap = 4;
        int total = w * 5 + gap * 4;
        int x = (this.width - total) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Add window"), b -> {
            ChatWindow window = new ChatWindow("Window " + (cfg.windows.size() + 1));
            window.x = 8 + cfg.windows.size() * 12;
            window.y = 60 + cfg.windows.size() * 12;
            cfg.windows.add(window);
            selected = window;
            ConfigManager.save();
            ChatRouter.get().rebuildAll();
        }).dimensions(x, y, w, 20).build());
        x += w + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), b -> {
            if (selected != null && this.client != null) {
                this.client.setScreen(new WindowSettingsScreen(this, selected));
            }
        }).dimensions(x, y, w, 20).build());
        x += w + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Show/hide"), b -> {
            if (selected != null) {
                selected.visible = !selected.visible;
                ConfigManager.save();
            }
        }).dimensions(x, y, w, 20).build());
        x += w + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
            if (selected != null && cfg.windows.size() > 1) {
                cfg.windows.remove(selected);
                selected = cfg.windows.get(cfg.windows.size() - 1);
                ConfigManager.save();
            }
        }).dimensions(x, y, w, 20).build());
        x += w + gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(x, y, w, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // handled in render() so the dim is only drawn once, underneath the windows
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);

        ChatWindowsConfig cfg = ConfigManager.get();
        for (ChatWindow window : cfg.windows) {
            ChatWindowRenderer.render(context, window, true, true);

            int left = window.resolveLeft(this.width);
            int top = window.resolveTop(this.height);
            int right = left + window.scaledWidth();
            int bottom = top + window.scaledHeight();

            boolean isSelected = window == selected;
            int outline = isSelected ? 0xFF55FF55 : (window.visible ? 0x88FFFFFF : 0x88FF5555);
            drawOutline(context, left, top, right, bottom, outline);

            // resize grip
            context.fill(right - ChatWindowRenderer.RESIZE_HANDLE, bottom - ChatWindowRenderer.RESIZE_HANDLE,
                    right, bottom, isSelected ? 0xCC55FF55 : 0x66FFFFFF);

            String label = window.name + (window.visible ? "" : " (hidden)");
            context.drawTextWithShadow(this.textRenderer, label, left + 2, top - 10,
                    isSelected ? 0xFF55FF55 : 0xFFCCCCCC);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Drag to move  -  drag the corner to resize  -  right-click a window for its settings"),
                this.width / 2, 20, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Hold SHIFT while dragging to disable edge snapping"),
                this.width / 2, 30, 0xFF777777);
    }

    private void drawOutline(DrawContext context, int left, int top, int right, int bottom, int color) {
        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        ChatWindowsConfig cfg = ConfigManager.get();
        ChatWindow hit = null;
        for (int i = cfg.windows.size() - 1; i >= 0; i--) {
            ChatWindow window = cfg.windows.get(i);
            int left = window.resolveLeft(this.width);
            int top = window.resolveTop(this.height);
            if (mouseX >= left && mouseX < left + window.scaledWidth()
                    && mouseY >= top && mouseY < top + window.scaledHeight()) {
                hit = window;
                break;
            }
        }
        if (hit == null) return false;

        selected = hit;
        // bring to front
        cfg.windows.remove(hit);
        cfg.windows.add(hit);

        if (button == 1) {
            if (this.client != null) this.client.setScreen(new WindowSettingsScreen(this, hit));
            return true;
        }
        if (button == 2) {
            hit.visible = !hit.visible;
            ConfigManager.save();
            return true;
        }

        startLeft = hit.resolveLeft(this.width);
        startTop = hit.resolveTop(this.height);
        int right = startLeft + hit.scaledWidth();
        int bottom = startTop + hit.scaledHeight();

        if (mouseX >= right - ChatWindowRenderer.RESIZE_HANDLE && mouseY >= bottom - ChatWindowRenderer.RESIZE_HANDLE) {
            dragMode = 2;
            startWidth = hit.width;
            startHeight = hit.height;
            dragStartX = mouseX;
            dragStartY = mouseY;
        } else {
            dragMode = 1;
            dragOffsetX = mouseX - startLeft;
            dragOffsetY = mouseY - startTop;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (selected == null || dragMode == 0) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        if (dragMode == 1) {
            int left = (int) Math.round(mouseX - dragOffsetX);
            int top = (int) Math.round(mouseY - dragOffsetY);
            if (!hasShiftDown()) {
                int w = selected.scaledWidth();
                int h = selected.scaledHeight();
                if (Math.abs(left) < SNAP) left = 0;
                if (Math.abs(this.width - (left + w)) < SNAP) left = this.width - w;
                if (Math.abs(top) < SNAP) top = 0;
                if (Math.abs(this.height - (top + h)) < SNAP) top = this.height - h;
            }
            selected.setAbsolute(left, top, this.width, this.height);
        } else {
            int newWidth = (int) Math.round(startWidth + (mouseX - dragStartX) / selected.scale);
            int newHeight = (int) Math.round(startHeight + (mouseY - dragStartY) / selected.scale);
            selected.width = MathHelper.clamp(newWidth, 80, 1920);
            selected.height = MathHelper.clamp(newHeight, 40, 1080);
            // keep the top-left corner pinned while the size changes
            selected.setAbsolute(startLeft, startTop, this.width, this.height);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragMode != 0) {
            dragMode = 0;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ChatWindow window = ChatRouter.get().windowAt(mouseX, mouseY);
        if (window != null && hasControlDown()) {
            window.scale = MathHelper.clamp(window.scale + (float) verticalAmount * 0.05f, 0.5f, 2.5f);
            ConfigManager.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        ConfigManager.save();
        ChatRouter.get().rebuildAll();
        if (this.client != null) this.client.setScreen(parent);
    }
}
