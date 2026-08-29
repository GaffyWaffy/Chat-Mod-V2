package dev.chatwindows.gui;

import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.config.ChatWindowsConfig;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.render.ChatWindowRenderer;
import dev.chatwindows.util.Keys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

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
    private boolean prevLeft, prevRight, prevMiddle;

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
        handleMouse(mouseX, mouseY);

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
                Text.literal("Hold SHIFT to disable edge snapping  -  middle-click to hide a window"),
                this.width / 2, 30, 0xFF777777);
    }

    private void drawOutline(DrawContext context, int left, int top, int right, int bottom, int color) {
        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }

    // ---------------------------------------------------------------- input
    //
    // Mouse handling is polled from GLFW instead of overriding mouseClicked /
    // mouseDragged / mouseReleased, because those now take a Click record whose
    // shape varies between versions. render() hands us the cursor position in
    // GUI coordinates, which is all we need. The bottom button row still uses
    // the normal widget path.

    private void handleMouse(int mouseX, int mouseY) {
        boolean left = Keys.mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean right = Keys.mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        boolean middle = Keys.mouseDown(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

        // don't fight with the button row along the bottom
        boolean overButtons = mouseY >= this.height - 34;

        if (!overButtons) {
            if (right && !prevRight) {
                ChatWindow window = windowAt(mouseX, mouseY);
                if (window != null) {
                    selected = window;
                    if (this.client != null) this.client.setScreen(new WindowSettingsScreen(this, window));
                    prevLeft = left;
                    prevRight = right;
                    prevMiddle = middle;
                    return;
                }
            }

            if (middle && !prevMiddle) {
                ChatWindow window = windowAt(mouseX, mouseY);
                if (window != null) {
                    window.visible = !window.visible;
                    ConfigManager.save();
                }
            }

            if (left && !prevLeft) {
                beginDrag(mouseX, mouseY);
            }
        }

        if (left && prevLeft && dragMode != 0) {
            updateDrag(mouseX, mouseY);
        } else if (!left && prevLeft && dragMode != 0) {
            dragMode = 0;
            ConfigManager.save();
        }

        prevLeft = left;
        prevRight = right;
        prevMiddle = middle;
    }

    private ChatWindow windowAt(int mouseX, int mouseY) {
        ChatWindowsConfig cfg = ConfigManager.get();
        for (int i = cfg.windows.size() - 1; i >= 0; i--) {
            ChatWindow window = cfg.windows.get(i);
            int left = window.resolveLeft(this.width);
            int top = window.resolveTop(this.height);
            if (mouseX >= left && mouseX < left + window.scaledWidth()
                    && mouseY >= top && mouseY < top + window.scaledHeight()) {
                return window;
            }
        }
        return null;
    }

    private void beginDrag(int mouseX, int mouseY) {
        ChatWindow hit = windowAt(mouseX, mouseY);
        if (hit == null) return;

        selected = hit;
        // bring to front
        ChatWindowsConfig cfg = ConfigManager.get();
        cfg.windows.remove(hit);
        cfg.windows.add(hit);

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
    }

    private void updateDrag(int mouseX, int mouseY) {
        if (selected == null) return;

        if (dragMode == 1) {
            int left = (int) Math.round(mouseX - dragOffsetX);
            int top = (int) Math.round(mouseY - dragOffsetY);
            if (!Keys.shift()) {
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
    }

    @Override
    public void close() {
        ConfigManager.save();
        ChatRouter.get().rebuildAll();
        if (this.client != null) this.client.setScreen(parent);
    }
}
