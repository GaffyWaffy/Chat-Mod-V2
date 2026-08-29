package dev.chatwindows.mixin;

import dev.chatwindows.chat.ChatRouter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes mouse input to the chat windows while the chat box is open, so you can
 * scroll each window independently and click links inside them.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chatwindows$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Style style = ChatRouter.get().styleAt(mouseX, mouseY);
        if (style != null && style.getClickEvent() != null && this.handleTextClick(style)) {
            cir.setReturnValue(true);
            return;
        }
        if (ChatRouter.get().mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void chatwindows$mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                           double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (ChatRouter.get().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

}
