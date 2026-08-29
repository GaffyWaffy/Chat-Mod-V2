package dev.chatwindows.util;

import java.util.Locale;

public final class ColorUtil {
    private ColorUtil() {}

    /** Parses "#RRGGBB", "RRGGBB" or "#RGB" into a 0xRRGGBB int. Returns fallback on bad input. */
    public static int parseHex(String hex, int fallback) {
        if (hex == null) return fallback;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) return fallback;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean isValidHex(String hex) {
        if (hex == null) return false;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 3 && s.length() != 6) return false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.digit(s.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static int alphaOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /** Multiplies the alpha channel of an ARGB colour by a 0..255 opacity. */
    public static int scaleAlpha(int argb, int opacity255) {
        int a = (int) (alphaOf(argb) * (opacity255 / 255.0f));
        return argb(a, argb & 0xFFFFFF);
    }
}
