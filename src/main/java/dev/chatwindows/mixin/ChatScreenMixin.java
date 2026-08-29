package dev.chatwindows.mixin;

import dev.chatwindows.chat.ChatRouter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes mouse input to the chat windows while the chat box is open.
 *
 * Clicks are detected from the render hook (which hands us mouse coordinates)
 * rather than from mouseClicked, because that method now takes a Click record
 * whose shape varies between versions.
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"))
    private void chatwindows$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChatRouter.get().pollChatScreenMouse(mouseX, mouseY);
    }

    // require = 0: if this signature moves, per-window scrolling quietly stops
    // working instead of crashing the game at startup.
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void chatwindows$mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                           double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (ChatRouter.get().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
