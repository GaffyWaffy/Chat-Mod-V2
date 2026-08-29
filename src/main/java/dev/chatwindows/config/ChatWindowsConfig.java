package dev.chatwindows.config;

import dev.chatwindows.util.MatchType;

import java.util.ArrayList;
import java.util.List;

public class ChatWindowsConfig {

    public int configVersion = 1;

    /** Master switch. When off the mod does nothing and vanilla chat behaves normally. */
    public boolean enabled = true;
    /** Stop messages from reaching the vanilla chat HUD (leave on unless you want both). */
    public boolean hideVanillaChat = true;
    /** How many raw messages are kept so filters can be re-applied live while you edit them. */
    public int historySize = 300;

    public List<ChatWindow> windows = new ArrayList<>();

    public ChatWindowsConfig() {}

    public static ChatWindowsConfig createDefault() {
        ChatWindowsConfig cfg = new ChatWindowsConfig();

        ChatWindow main = new ChatWindow("Main");
        main.anchor = ChatWindow.Anchor.BOTTOM_LEFT;
        main.x = 4;
        main.y = 40;
        main.width = 320;
        main.height = 180;

        ChatTab all = ChatWindow.defaultTab();
        HighlightRule mention = new HighlightRule("", "#FFAA00");
        mention.enabled = false; // put your own username in and enable it
        mention.match = MatchType.CONTAINS;
        all.highlights.add(mention);

        ChatTab whispers = new ChatTab("Whispers");
        whispers.filters.add(new FilterRule("whispers to you", MatchType.CONTAINS, FilterRule.Mode.SHOW_ONLY));
        whispers.filters.add(new FilterRule("You whisper to", MatchType.CONTAINS, FilterRule.Mode.SHOW_ONLY));
        whispers.filters.add(new FilterRule("msg", MatchType.STARTS_WITH, FilterRule.Mode.SHOW_ONLY));
        HighlightRule whisperHl = new HighlightRule("whispers to you", "#AA00AA");
        whisperHl.alpha = 80;
        whispers.highlights.add(whisperHl);
        whispers.sendPrefix = "";

        main.tabs.clear();
        main.tabs.add(all);
        main.tabs.add(whispers);

        cfg.windows.add(main);
        return cfg;
    }

    public void validate() {
        if (windows == null) windows = new ArrayList<>();
        windows.removeIf(java.util.Objects::isNull);
        if (windows.isEmpty()) windows.add(new ChatWindow("Main"));
        for (ChatWindow w : windows) w.validate();
        historySize = Math.max(50, Math.min(historySize, 2000));
    }
}
