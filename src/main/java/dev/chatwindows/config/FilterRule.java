package dev.chatwindows.config;

import dev.chatwindows.util.MatchType;

/** A single show-only / hide rule belonging to one tab. */
public class FilterRule {

    public enum Mode {
        SHOW_ONLY("Show only"),
        HIDE("Hide");

        private final String display;
        Mode(String d) { this.display = d; }
        public String display() { return display; }
        public Mode next() { return this == SHOW_ONLY ? HIDE : SHOW_ONLY; }
    }

    public String pattern = "";
    public MatchType match = MatchType.CONTAINS;
    public Mode mode = Mode.SHOW_ONLY;
    public boolean caseSensitive = false;
    public boolean enabled = true;

    public FilterRule() {}

    public FilterRule(String pattern, MatchType match, Mode mode) {
        this.pattern = pattern;
        this.match = match;
        this.mode = mode;
    }

    public boolean matches(String plainLine) {
        return match.test(plainLine, pattern, caseSensitive);
    }
}
