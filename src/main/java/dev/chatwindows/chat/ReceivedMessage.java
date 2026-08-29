package dev.chatwindows.chat;

import net.minecraft.text.Text;

/**
 * One incoming chat line, stored once and shared by every tab that accepted it.
 *
 * @param text        the original formatted component (styles intact)
 * @param plain       the same line with all formatting stripped - what filters match
 *                    against, and what the copy button puts on the clipboard
 * @param tick        client tick at which it arrived, used for the fade-out timer
 * @param epochMillis wall-clock arrival time, used for timestamps
 */
public record ReceivedMessage(Text text, String plain, int tick, long epochMillis) {
}
