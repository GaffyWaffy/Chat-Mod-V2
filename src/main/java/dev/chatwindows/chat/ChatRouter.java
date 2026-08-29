package dev.chatwindows.chat;

import dev.chatwindows.config.ChatTab;
import dev.chatwindows.config.ChatWindow;
import dev.chatwindows.config.ChatWindowsConfig;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.gui.ChatLayoutScreen;
import dev.chatwindows.render.ChatWindowRenderer;
import net.minecraft.client.MinecraftClient;
import dev.chatwindows.util.Keys;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * Receives every incoming chat line, decides which tabs get it, and owns
 * rendering / mouse handling for all windows.
 */
public final class ChatRouter {

    private static final ChatRouter INSTANCE = new ChatRouter();
    private static final Pattern LEGACY_CODES = Pattern.compile("(?i)\u00a7[0-9A-FK-ORX]");

    private final Deque<ReceivedMessage> history = new ArrayDeque<>();
    private int ticks;
    private boolean prevLeftDown;

    private ChatRouter() {}

    public static ChatRouter get() {
        return INSTANCE;
    }

    public int ticks() {
        return ticks;
    }

    public void tick() {
        ticks++;
    }

    // ------------------------------------------------------------------ routing

    /**
     * @return true if the vanilla chat HUD should be skipped for this message.
     */
    public boolean dispatch(Text text) {
        ChatWindowsConfig cfg = ConfigManager.get();
        if (!cfg.enabled) return false;

        String plain = strip(text.getString());
        ReceivedMessage message = new ReceivedMessage(text, plain, ticks);

        history.addLast(message);
        while (history.size() > cfg.historySize) history.removeFirst();

        route(message, true);
        return cfg.hideVanillaChat;
    }

    private void route(ReceivedMessage message, boolean allowSound) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatWindowsConfig cfg = ConfigManager.get();

        for (ChatWindow window : cfg.windows) {
            for (int i = 0; i < window.tabs.size(); i++) {
                ChatTab tab = window.tabs.get(i);
                if (!tab.accepts(message.plain())) continue;

                tab.addMessage(message, tab.highlightFor(message.plain()), window.maxMessages, client.textRenderer);

                if (i != window.activeTab) tab.unread = true;
            }
        }
    }

    /** Re-applies every filter/highlight to the stored backlog. Called after settings change. */
    public void rebuildAll() {
        ChatWindowsConfig cfg = ConfigManager.get();
        for (ChatWindow window : cfg.windows) {
            for (ChatTab tab : window.tabs) tab.clear();
        }
        for (ReceivedMessage message : history) {
            route(message, false);
        }
    }

    public void clearAll() {
        history.clear();
        for (ChatWindow window : ConfigManager.get().windows) {
            for (ChatTab tab : window.tabs) tab.clear();
        }
    }

    private static String strip(String raw) {
        return LEGACY_CODES.matcher(raw).replaceAll("");
    }

    // ------------------------------------------------------------------ rendering

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.options.hudHidden) return;
        // The layout editor draws the windows itself with extra chrome.
        if (client.currentScreen instanceof ChatLayoutScreen) return;

        ChatWindowsConfig cfg = ConfigManager.get();
        if (!cfg.enabled) return;

        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        for (ChatWindow window : cfg.windows) {
            if (!window.visible) continue;
            ChatWindowRenderer.render(context, window, chatOpen, false);
        }
    }

    // ------------------------------------------------------------------ input

    public ChatWindow windowAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        ChatWindowsConfig cfg = ConfigManager.get();
        // iterate backwards so the "topmost" window wins
        for (int i = cfg.windows.size() - 1; i >= 0; i--) {
            ChatWindow window = cfg.windows.get(i);
            if (!window.visible) continue;
            if (ChatWindowRenderer.contains(window, mouseX, mouseY, sw, sh)) return window;
        }
        return null;
    }

    /** Handles tab switching. Returns true if the click was consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!ConfigManager.get().enabled) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        ChatWindow window = windowAt(mouseX, mouseY);
        if (window == null) return false;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        double lx = ChatWindowRenderer.localX(window, mouseX, sw);
        double ly = ChatWindowRenderer.localY(window, mouseY, sh);

        int tabIndex = ChatWindowRenderer.tabAt(window, lx, ly, client.textRenderer);
        if (tabIndex >= 0) {
            window.setActiveTab(tabIndex);
            ConfigManager.save();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!ConfigManager.get().enabled) return false;
        ChatWindow window = windowAt(mouseX, mouseY);
        if (window == null) return false;
        ChatTab tab = window.activeTab();
        if (tab == null) return false;

        int step = Keys.ctrl() ? 1 : 3;
        int perPage = ChatWindowRenderer.visibleLineCount(window);
        int maxScroll = Math.max(0, tab.lines.size() - perPage);
        tab.scroll = (int) Math.max(0, Math.min(maxScroll, tab.scroll + amount * step));
        return true;
    }

    /**
     * Left-click detection while the chat box is open, driven from a HEAD inject
     * on ChatScreen#render so we get mouse coordinates without depending on the
     * Click API. Used for switching tabs by clicking the tab strip.
     */
    public void pollChatScreenMouse(int mouseX, int mouseY) {
        boolean down = Keys.mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (down && !prevLeftDown) {
            mouseClicked(mouseX, mouseY, 0);
        }
        prevLeftDown = down;
    }

    /** Cycles the active tab of the first visible window. */
    public void cycleTab(int direction) {
        for (ChatWindow window : ConfigManager.get().windows) {
            if (!window.visible || window.tabs.size() < 2) continue;
            window.setActiveTab(window.activeTab + direction);
            ConfigManager.save();
            return;
        }
    }

    /** Prefix configured on the active tab of the first visible window. */
    public String activeSendPrefix() {
        for (ChatWindow window : ConfigManager.get().windows) {
            if (!window.visible) continue;
            ChatTab tab = window.activeTab();
            if (tab != null && tab.sendPrefix != null && !tab.sendPrefix.isEmpty()) return tab.sendPrefix;
            return "";
        }
        return "";
    }
}
