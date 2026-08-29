package dev.chatwindows.chat;

import net.minecraft.text.Text;

/**
 * One incoming chat line, stored once and shared by every tab that accepted it.
 *
 * @param text  the original formatted component (styles, click/hover events intact)
 * @param plain the same line with all formatting stripped - this is what filters match against
 * @param tick  client tick at which it arrived, used for the fade-out timer
 */
public record ReceivedMessage(Text text, String plain, int tick) {
}
