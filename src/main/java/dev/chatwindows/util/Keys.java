package dev.chatwindows.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Raw GLFW input helpers.
 *
 * Minecraft's own Screen.hasShiftDown()/hasControlDown() and the KeyBinding
 * category API move around between versions, so this mod talks to GLFW
 * directly. Nothing in here depends on Minecraft's mappings beyond fetching
 * the window handle.
 */
public final class Keys {

    private Keys() {}

    private static long handle() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    /** GLFW key codes; anything <= 0 means "unbound" and is always false. */
    public static boolean isDown(int keyCode) {
        if (keyCode <= 0) return false;
        return GLFW.glfwGetKey(handle(), keyCode) == GLFW.GLFW_PRESS;
    }

    public static boolean shift() {
        return isDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean ctrl() {
        return isDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean mouseDown(int button) {
        return GLFW.glfwGetMouseButton(handle(), button) == GLFW.GLFW_PRESS;
    }

    /** Human-readable name for a GLFW key code, for the settings UI. */
    public static String describe(int keyCode) {
        if (keyCode <= 0) return "Unbound";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase(java.util.Locale.ROOT);
        return "Key " + keyCode;
    }
}
