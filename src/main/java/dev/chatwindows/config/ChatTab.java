package dev.chatwindows.config;

import dev.chatwindows.chat.ReceivedMessage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.List;

/**
 * A tab inside a window. Owns its own filters, highlights, scroll position and
 * message backlog, so tabs never affect each other.
 */
public class ChatTab {

    // ---- persisted settings -------------------------------------------------
    public String name = "New Tab";
    /** When true this tab ignores its filters and receives every message. */
    public boolean receiveAll = false;
    /** Text automatically prepended to anything you send while this tab is active (e.g. "/pc "). */
    public String sendPrefix = "";
    public List<FilterRule> filters = new ArrayList<>();
    public List<HighlightRule> highlights = new ArrayList<>();

    // ---- runtime ------------------------------------------------------------
    public transient List<TabMessage> messages = new ArrayList<>();
    public transient List<VisualLine> lines = new ArrayList<>();
    public transient int scroll = 0;
    public transient boolean unread = false;
    private transient int cachedWidth = -1;

    public ChatTab() {}

    public ChatTab(String name) {
        this.name = name;
    }

    public static class TabMessage {
        public final ReceivedMessage message;
        public int highlight;
        public int lineCount;

        public TabMessage(ReceivedMessage message, int highlight) {
            this.message = message;
            this.highlight = highlight;
        }
    }

    /** A single wrapped, ready-to-draw row. */
    public record VisualLine(OrderedText text, int highlight, int tick) {}

    // ---- filtering ----------------------------------------------------------

    /**
     * SHOW_ONLY rules act as a whitelist: if the tab has at least one enabled
     * SHOW_ONLY rule, a line must match one of them. HIDE rules are a blacklist
     * and always win.
     */
    public boolean accepts(String plainLine) {
        if (receiveAll) return true;
        if (filters == null) return true;

        boolean hasWhitelist = false;
        boolean matchedWhitelist = false;

        for (FilterRule rule : filters) {
            if (rule == null || !rule.enabled || rule.pattern.isEmpty()) continue;
            if (rule.mode == FilterRule.Mode.HIDE) {
                if (rule.matches(plainLine)) return false;
            } else {
                hasWhitelist = true;
                if (rule.matches(plainLine)) matchedWhitelist = true;
            }
        }
        return !hasWhitelist || matchedWhitelist;
    }

    /** First matching highlight wins. Returns a packed ARGB, or 0 for "no highlight". */
    public int highlightFor(String plainLine) {
        if (highlights == null) return 0;
        for (HighlightRule rule : highlights) {
            if (rule != null && rule.matches(plainLine)) return rule.argb();
        }
        return 0;
    }

    public HighlightRule matchingHighlight(String plainLine) {
        if (highlights == null) return null;
        for (HighlightRule rule : highlights) {
            if (rule != null && rule.matches(plainLine)) return rule;
        }
        return null;
    }

    // ---- message storage ----------------------------------------------------

    public void addMessage(ReceivedMessage message, int highlight, int maxMessages, TextRenderer textRenderer) {
        TabMessage tm = new TabMessage(message, highlight);
        messages.add(tm);

        if (cachedWidth > 0 && textRenderer != null) {
            tm.lineCount = appendLines(textRenderer, tm);
            // keep the viewport anchored while the user is scrolled up
            if (scroll > 0) scroll += tm.lineCount;
        }

        while (messages.size() > Math.max(20, maxMessages)) {
            TabMessage dropped = messages.remove(0);
            for (int i = 0; i < dropped.lineCount && !lines.isEmpty(); i++) {
                lines.remove(0);
            }
            if (scroll > 0) scroll = Math.max(0, scroll - dropped.lineCount);
        }
    }

    /** Rewraps everything if the usable width changed. */
    public void ensureLines(TextRenderer textRenderer, int width) {
        width = Math.max(24, width);
        if (width == cachedWidth) return;
        cachedWidth = width;
        lines.clear();
        for (TabMessage tm : messages) {
            tm.lineCount = appendLines(textRenderer, tm);
        }
    }

    private int appendLines(TextRenderer textRenderer, TabMessage tm) {
        List<OrderedText> wrapped = ChatMessages.breakRenderedChatMessageLines(tm.message.text(), cachedWidth, textRenderer);
        if (wrapped.isEmpty()) return 0;
        for (OrderedText ordered : wrapped) {
            lines.add(new VisualLine(ordered, tm.highlight, tm.message.tick()));
        }
        return wrapped.size();
    }

    /** Recomputes highlight colours after the rules were edited. */
    public void recomputeHighlights() {
        for (TabMessage tm : messages) {
            tm.highlight = highlightFor(tm.message.plain());
        }
        cachedWidth = -1; // force a rewrap so the new colours are picked up
    }

    public void clear() {
        messages.clear();
        lines.clear();
        scroll = 0;
        unread = false;
        cachedWidth = -1;
    }

    public void validate() {
        if (name == null) name = "Tab";
        if (sendPrefix == null) sendPrefix = "";
        if (filters == null) filters = new ArrayList<>();
        if (highlights == null) highlights = new ArrayList<>();
        if (messages == null) messages = new ArrayList<>();
        if (lines == null) lines = new ArrayList<>();
        filters.removeIf(java.util.Objects::isNull);
        highlights.removeIf(java.util.Objects::isNull);
    }
}
