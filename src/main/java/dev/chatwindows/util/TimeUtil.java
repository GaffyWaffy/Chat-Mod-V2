package dev.chatwindows.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class TimeUtil {

    public static final String DEFAULT_PATTERN = "HH:mm";

    private static final Map<String, DateTimeFormatter> CACHE = new HashMap<>();

    private TimeUtil() {}

    /** Formats a wall-clock timestamp. Falls back to HH:mm if the pattern is invalid. */
    public static String format(String pattern, long epochMillis) {
        DateTimeFormatter formatter = formatter(pattern);
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        try {
            return time.format(formatter);
        } catch (Exception e) {
            return time.format(formatter(DEFAULT_PATTERN));
        }
    }

    public static boolean isValidPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        try {
            DateTimeFormatter.ofPattern(pattern);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static DateTimeFormatter formatter(String pattern) {
        String key = pattern == null || pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
        DateTimeFormatter cached = CACHE.get(key);
        if (cached != null) return cached;
        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(key);
        } catch (Exception e) {
            formatter = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
        }
        if (CACHE.size() > 32) CACHE.clear();
        CACHE.put(key, formatter);
        return formatter;
    }
}
