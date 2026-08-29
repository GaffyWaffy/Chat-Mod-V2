package dev.chatwindows.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One floating chat window. Geometry + appearance live here; everything about
 * message selection lives on the individual {@link ChatTab}s.
 */
public class ChatWindow {

    public enum Anchor {
        TOP_LEFT("Top left"),
        TOP_RIGHT("Top right"),
        BOTTOM_LEFT("Bottom left"),
        BOTTOM_RIGHT("Bottom right");

        private final String display;
        Anchor(String d) { this.display = d; }
        public String display() { return display; }
        public boolean right() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }
        public boolean bottom() { return this == BOTTOM_LEFT || this == BOTTOM_RIGHT; }
    }

    public String id = UUID.randomUUID().toString();
    public String name = "Chat";

    /** Offset from the anchored corner, in GUI pixels. */
    public int x = 4;
    public int y = 40;
    public Anchor anchor = Anchor.BOTTOM_LEFT;

    /** Unscaled size. The drawn size is width*scale x height*scale. */
    public int width = 320;
    public int height = 180;
    public float scale = 1.0f;

    public boolean visible = true;
    public boolean showTabBar = true;
    public boolean textShadow = true;

    public String backgroundColor = "#000000";
    public int backgroundAlpha = 80;         // when chat is closed
    public int backgroundAlphaFocused = 160; // when chat is open

    /** Prefix each message with a wall-clock timestamp. */
    public boolean showTimestamps = false;
    /** java.time pattern, e.g. "HH:mm", "HH:mm:ss", "h:mm a". */
    public String timestampFormat = "HH:mm";
    public String timestampColor = "#808080";

    /** Show a copy-to-clipboard button on the hovered message (chat open only). */
    public boolean showCopyButton = true;

    public int lineSpacing = 9;
    /** Seconds before a line fades out while chat is closed. 0 = never fade. */
    public int fadeOutSeconds = 10;
    public int maxMessages = 250;

    public List<ChatTab> tabs = new ArrayList<>();
    public int activeTab = 0;

    public ChatWindow() {}

    public ChatWindow(String name) {
        this.name = name;
        this.tabs.add(defaultTab());
    }

    public static ChatTab defaultTab() {
        ChatTab tab = new ChatTab("All");
        tab.receiveAll = true;
        return tab;
    }

    public ChatTab activeTab() {
        if (tabs.isEmpty()) return null;
        if (activeTab < 0 || activeTab >= tabs.size()) activeTab = 0;
        return tabs.get(activeTab);
    }

    public void setActiveTab(int index) {
        if (tabs.isEmpty()) return;
        activeTab = Math.floorMod(index, tabs.size());
        ChatTab tab = tabs.get(activeTab);
        tab.unread = false;
    }

    public int scaledWidth() { return Math.round(width * scale); }
    public int scaledHeight() { return Math.round(height * scale); }

    /** Absolute left edge in GUI pixels for the given screen size. */
    public int resolveLeft(int screenWidth) {
        int w = scaledWidth();
        int left = anchor.right() ? screenWidth - x - w : x;
        return Math.max(-w + 20, Math.min(left, screenWidth - 20));
    }

    /** Absolute top edge in GUI pixels for the given screen size. */
    public int resolveTop(int screenHeight) {
        int h = scaledHeight();
        int top = anchor.bottom() ? screenHeight - y - h : y;
        return Math.max(-h + 20, Math.min(top, screenHeight - 20));
    }

    /** Stores an absolute position back into anchor-relative coordinates. */
    public void setAbsolute(int left, int top, int screenWidth, int screenHeight) {
        x = anchor.right() ? screenWidth - left - scaledWidth() : left;
        y = anchor.bottom() ? screenHeight - top - scaledHeight() : top;
    }

    public void validate() {
        if (id == null || id.isEmpty()) id = UUID.randomUUID().toString();
        if (name == null) name = "Chat";
        if (anchor == null) anchor = Anchor.BOTTOM_LEFT;
        if (backgroundColor == null) backgroundColor = "#000000";
        if (timestampColor == null) timestampColor = "#808080";
        if (timestampFormat == null || timestampFormat.isEmpty()) timestampFormat = dev.chatwindows.util.TimeUtil.DEFAULT_PATTERN;
        if (tabs == null) tabs = new ArrayList<>();
        tabs.removeIf(java.util.Objects::isNull);
        if (tabs.isEmpty()) tabs.add(defaultTab());
        for (ChatTab tab : tabs) tab.validate();
        width = Math.max(80, Math.min(width, 1920));
        height = Math.max(40, Math.min(height, 1080));
        scale = Math.max(0.5f, Math.min(scale, 2.5f));
        lineSpacing = Math.max(7, Math.min(lineSpacing, 20));
        backgroundAlpha = Math.max(0, Math.min(backgroundAlpha, 255));
        backgroundAlphaFocused = Math.max(0, Math.min(backgroundAlphaFocused, 255));
        fadeOutSeconds = Math.max(0, Math.min(fadeOutSeconds, 300));
        maxMessages = Math.max(20, Math.min(maxMessages, 2000));
        if (activeTab < 0 || activeTab >= tabs.size()) activeTab = 0;
    }
}
