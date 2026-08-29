package dev.chatwindows.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** How a filter/highlight pattern is compared against a chat line. */
public enum MatchType {
    CONTAINS("Contains"),
    STARTS_WITH("Starts with"),
    ENDS_WITH("Ends with"),
    EXACT("Exactly equals"),
    REGEX("Regex");

    private static final Map<String, Pattern> REGEX_CACHE = new HashMap<>();

    private final String display;

    MatchType(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public MatchType next() {
        MatchType[] v = values();
        return v[(this.ordinal() + 1) % v.length];
    }

    public boolean test(String input, String pattern, boolean caseSensitive) {
        if (input == null || pattern == null || pattern.isEmpty()) return false;

        if (this == REGEX) {
            Pattern p = compile(pattern, caseSensitive);
            return p != null && p.matcher(input).find();
        }

        String a = caseSensitive ? input : input.toLowerCase(Locale.ROOT);
        String b = caseSensitive ? pattern : pattern.toLowerCase(Locale.ROOT);
        return switch (this) {
            case CONTAINS -> a.contains(b);
            case STARTS_WITH -> a.startsWith(b);
            case ENDS_WITH -> a.endsWith(b);
            case EXACT -> a.equals(b);
            default -> false;
        };
    }

    private static Pattern compile(String pattern, boolean caseSensitive) {
        String key = (caseSensitive ? "s:" : "i:") + pattern;
        Pattern cached = REGEX_CACHE.get(key);
        if (cached != null) return cached;
        try {
            Pattern p = Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            if (REGEX_CACHE.size() > 256) REGEX_CACHE.clear();
            REGEX_CACHE.put(key, p);
            return p;
        } catch (PatternSyntaxException e) {
            return null;
        }
    }
}
