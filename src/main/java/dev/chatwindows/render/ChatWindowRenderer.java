package dev.chatwindows.render;

import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a {@link ChatWindow}. All drawing happens in window-local coordinates
 * (0,0 = top-left of the window) after a translate + scale, and is clipped to
 * the window rectangle with a scissor.
 */
public final class ChatWindowRenderer {

    public static final int TAB_HEIGHT = 12;
    public static final int PADDING = 2;
    public static final int RESIZE_HANDLE = 8;

    private ChatWindowRenderer() {}

    /** One row that is actually on screen. */
    public record Line(OrderedText text, int highlight, int tick, int localY, String source) {}

    // ------------------------------------------------------------------ layout

    public static int usableTextWidth(ChatWindow window) {
        return Math.max(24, window.width - PADDING * 2 - 4);
    }

    public static int visibleLineCount(ChatWindow window) {
        int contentTop = window.showTabBar ? TAB_HEIGHT : 0;
        int usable = window.height - contentTop - PADDING * 2;
        return Math.max(1, usable / Math.max(1, window.lineSpacing));
    }

    /**
     * Works out which wrapped lines are visible and where they sit. Used both by
     * the renderer and by hit-testing, so clicking always lines up with drawing.
     */
    public static List<Line> layout(ChatWindow window, ChatTab tab, TextRenderer textRenderer) {
        List<Line> out = new ArrayList<>();
        if (tab == null) return out;

        tab.ensureLines(textRenderer, usableTextWidth(window), window);

        List<ChatTab.VisualLine> all = tab.lines;
        int perPage = visibleLineCount(window);
        int maxScroll = Math.max(0, all.size() - perPage);
        tab.scroll = MathHelper.clamp(tab.scroll, 0, maxScroll);

        int end = all.size() - tab.scroll;
        int start = Math.max(0, end - perPage);
        int count = end - start;
        int y = window.height - PADDING - count * window.lineSpacing;

        for (int i = start; i < end; i++) {
            ChatTab.VisualLine vl = all.get(i);
            out.add(new Line(vl.text(), vl.highlight(), vl.tick(), y, vl.source()));
            y += window.lineSpacing;
        }
        return out;
    }

    // ------------------------------------------------------------- copy button

    public static int copySize(ChatWindow window) {
        return Math.max(6, Math.min(10, window.lineSpacing));
    }

    /** Left edge of the copy button, kept clear of the scrollbar at the far right. */
    public static int copyButtonX(ChatWindow window) {
        return window.width - copySize(window) - 5;
    }

    private static boolean overCopyButton(ChatWindow window, double localX) {
        int bx = copyButtonX(window);
        return localX >= bx && localX < bx + copySize(window);
    }

    private static void drawCopyButton(DrawContext context, ChatWindow window, int localY, boolean hovered) {
        int size = copySize(window);
        int x = copyButtonX(window);
        int y = localY - 1;

        context.fill(x, y, x + size, y + size, ColorUtil.argb(hovered ? 230 : 150, hovered ? 0x4A90D9 : 0x2A2A2A));
        // two offset sheets, suggesting "copy"
        context.fill(x + 2, y + 2, x + size - 3, y + size - 3, ColorUtil.argb(255, 0xE0E0E0));
        context.fill(x + 3, y + 3, x + size - 2, y + size - 2, ColorUtil.argb(255, 0xFFFFFF));
    }

    /**
     * Plain text of the message whose copy button sits under the cursor, or null.
     * Coordinates are window-local and unscaled.
     */
    public static String copyTargetAt(ChatWindow window, double localX, double localY, TextRenderer textRenderer) {
        if (!window.showCopyButton) return null;
        ChatTab tab = window.activeTab();
        if (tab == null) return null;
        if (!overCopyButton(window, localX)) return null;

        for (Line line : layout(window, tab, textRenderer)) {
            if (localY >= line.localY() - 1 && localY < line.localY() + window.lineSpacing - 1) {
                return line.source();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing

    public static void render(DrawContext context, ChatWindow window, boolean focused, boolean editorMode,
                              int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int left = window.resolveLeft(screenW);
        int top = window.resolveTop(screenH);
        int drawW = window.scaledWidth();
        int drawH = window.scaledHeight();

        boolean bright = focused || editorMode;

        // Mouse in window-local, unscaled space. Off-screen when chat is closed.
        double localMouseX = (mouseX - left) / window.scale;
        double localMouseY = (mouseY - top) / window.scale;
        boolean mouseInside = localMouseX >= 0 && localMouseX < window.width
                && localMouseY >= 0 && localMouseY < window.height;

        context.enableScissor(left, top, left + drawW, top + drawH);

        // NOTE: 1.21.6+ uses the 2D Matrix3x2fStack API below.
        // On 1.21.5 and older replace with:
        //   context.getMatrices().push();
        //   context.getMatrices().translate(left, top, 0.0f);
        //   context.getMatrices().scale(window.scale, window.scale, 1.0f);
        //   ... context.getMatrices().pop();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) left, (float) top);
        context.getMatrices().scale(window.scale, window.scale);

        int bgAlpha = bright ? window.backgroundAlphaFocused : window.backgroundAlpha;
        if (bgAlpha > 0) {
            context.fill(0, 0, window.width, window.height,
                    ColorUtil.argb(bgAlpha, ColorUtil.parseHex(window.backgroundColor, 0x000000)));
        }

        if (window.showTabBar) {
            renderTabBar(context, textRenderer, window, bright);
        }

        ChatTab tab = window.activeTab();
        if (tab != null) {
            List<Line> lines = layout(window, tab, textRenderer);
            for (Line line : lines) {
                int opacity = lineOpacity(window, line.tick(), bright);
                if (opacity <= 4) continue;

                if (ColorUtil.alphaOf(line.highlight()) > 0) {
                    // Background bar only - the text keeps its own colours.
                    context.fill(0, line.localY() - 1, window.width, line.localY() + window.lineSpacing - 1,
                            ColorUtil.scaleAlpha(line.highlight(), opacity));
                }

                boolean hovered = focused && window.showCopyButton && mouseInside
                        && localMouseY >= line.localY() - 1
                        && localMouseY < line.localY() + window.lineSpacing - 1;

                if (hovered) {
                    context.fill(0, line.localY() - 1, window.width, line.localY() + window.lineSpacing - 1,
                            ColorUtil.argb(35, 0xFFFFFF));
                }

                context.drawText(textRenderer, line.text(), PADDING + 1, line.localY(),
                        ColorUtil.argb(opacity, 0xFFFFFF), window.textShadow);

                if (hovered) {
                    drawCopyButton(context, window, line.localY(), overCopyButton(window, localMouseX));
                }
            }

            renderScrollbar(context, window, tab, bright);

            if (focused && ChatRouter.get().copyFlash(window)) {
                context.drawText(textRenderer, "Copied to clipboard", PADDING + 1,
                        window.showTabBar ? TAB_HEIGHT + 2 : 2, ColorUtil.argb(230, 0x55FF55), true);
            }
        }

        context.getMatrices().popMatrix();
        context.disableScissor();
    }

    private static void renderTabBar(DrawContext context, TextRenderer textRenderer, ChatWindow window, boolean bright) {
        context.fill(0, 0, window.width, TAB_HEIGHT, ColorUtil.argb(bright ? 150 : 70, 0x101010));
        int x = 0;
        for (int i = 0; i < window.tabs.size(); i++) {
            ChatTab tab = window.tabs.get(i);
            int tabWidth = tabWidth(textRenderer, tab);
            if (x > window.width) break;

            boolean active = i == window.activeTab;
            context.fill(x, 0, x + tabWidth, TAB_HEIGHT, ColorUtil.argb(active ? (bright ? 190 : 110) : (bright ? 90 : 40), active ? 0x3A3A3A : 0x000000));
            if (active) {
                context.fill(x, TAB_HEIGHT - 1, x + tabWidth, TAB_HEIGHT, ColorUtil.argb(255, 0xFFFFFF));
            }

            int textColor = active ? 0xFFFFFF : (tab.unread ? 0xFFFF55 : 0xA0A0A0);
            context.drawText(textRenderer, tab.name, x + 4, 2, ColorUtil.argb(bright ? 255 : 190, textColor), false);

            if (tab.unread && !active) {
                context.fill(x + tabWidth - 3, 2, x + tabWidth - 1, 4, ColorUtil.argb(255, 0xFF5555));
            }
            x += tabWidth;
        }
    }

    public static int tabWidth(TextRenderer textRenderer, ChatTab tab) {
        return textRenderer.getWidth(tab.name) + 10;
    }

    private static void renderScrollbar(DrawContext context, ChatWindow window, ChatTab tab, boolean bright) {
        if (!bright) return; // only draw the scrollbar while the chat box is open

        int perPage = visibleLineCount(window);
        int total = tab.lines.size();
        if (total <= perPage) return;

        int contentTop = window.showTabBar ? TAB_HEIGHT : 0;
        int trackTop = contentTop + 1;
        int trackBottom = window.height - 1;
        int trackHeight = trackBottom - trackTop;
        if (trackHeight <= 4) return;

        int barHeight = Math.max(8, (int) (trackHeight * (perPage / (float) total)));
        float progress = 1.0f - (tab.scroll / (float) Math.max(1, total - perPage));
        int barTop = trackTop + (int) ((trackHeight - barHeight) * progress);

        context.fill(window.width - 3, trackTop, window.width - 1, trackBottom, ColorUtil.argb(bright ? 70 : 30, 0xFFFFFF));
        context.fill(window.width - 3, barTop, window.width - 1, barTop + barHeight, ColorUtil.argb(bright ? 190 : 90, 0xFFFFFF));
    }

    /** Vanilla-style fade out: full opacity, then a quick taper near the end. */
    public static int lineOpacity(ChatWindow window, int addedTick, boolean bright) {
        if (bright || window.fadeOutSeconds <= 0) return 255;

        int age = ChatRouter.get().ticks() - addedTick;
        int lifetime = window.fadeOutSeconds * 20;
        if (age >= lifetime) return 0;

        double remaining = 1.0 - (double) age / lifetime;
        remaining = MathHelper.clamp(remaining * 10.0, 0.0, 1.0);
        remaining = remaining * remaining;
        return (int) (255.0 * remaining);
    }

    // ------------------------------------------------------------------ hit testing

    /** True if the given screen coordinate falls inside the window rectangle. */
    public static boolean contains(ChatWindow window, double mouseX, double mouseY, int screenW, int screenH) {
        int left = window.resolveLeft(screenW);
        int top = window.resolveTop(screenH);
        return mouseX >= left && mouseX < left + window.scaledWidth()
                && mouseY >= top && mouseY < top + window.scaledHeight();
    }

    /** Converts a screen coordinate into window-local (unscaled) space. */
    public static double localX(ChatWindow window, double mouseX, int screenW) {
        return (mouseX - window.resolveLeft(screenW)) / window.scale;
    }

    public static double localY(ChatWindow window, double mouseY, int screenH) {
        return (mouseY - window.resolveTop(screenH)) / window.scale;
    }

    /** Index of the tab under the cursor, or -1. */
    public static int tabAt(ChatWindow window, double localX, double localY, TextRenderer textRenderer) {
        if (!window.showTabBar || localY < 0 || localY >= TAB_HEIGHT) return -1;
        int x = 0;
        for (int i = 0; i < window.tabs.size(); i++) {
            int w = tabWidth(textRenderer, window.tabs.get(i));
            if (localX >= x && localX < x + w) return i;
            x += w;
        }
        return -1;
    }

}
