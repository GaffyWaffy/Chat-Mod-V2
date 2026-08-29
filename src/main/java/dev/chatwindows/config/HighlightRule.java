package dev.chatwindows.config;

import dev.chatwindows.util.ColorUtil;
import dev.chatwindows.util.MatchType;

/**
 * Highlights a matching line by painting the message background.
 * The text colour itself is never touched.
 */
public class HighlightRule {

    public String pattern = "";
    public MatchType match = MatchType.CONTAINS;
    public boolean caseSensitive = false;
    public boolean enabled = true;

    /** Background colour as a hex code, e.g. "#FFAA00". */
    public String color = "#FFAA00";
    /** 0-255 opacity of the highlight bar. */
    public int alpha = 90;
    /** Play a ping when a line matches. */
    public boolean playSound = false;

    public HighlightRule() {}

    public HighlightRule(String pattern, String color) {
        this.pattern = pattern;
        this.color = color;
    }

    public boolean matches(String plainLine) {
        return enabled && match.test(plainLine, pattern, caseSensitive);
    }

    /** Packed ARGB used to fill the line background. 0 means "no highlight". */
    public int argb() {
        return ColorUtil.argb(alpha, ColorUtil.parseHex(color, 0xFFAA00));
    }
}
