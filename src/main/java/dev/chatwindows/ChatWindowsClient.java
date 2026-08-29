package dev.chatwindows;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ChatWindowsConfig;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.gui.ChatLayoutScreen;
import dev.chatwindows.util.Keys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatWindowsClient implements ClientModInitializer {

    public static final String MOD_ID = "chatwindows";
    public static final Logger LOGGER = LoggerFactory.getLogger("ChatWindows");

    // Edge-detection state for the polled hotkeys.
    private boolean prevLayout;
    private boolean prevNext;
    private boolean prevPrev;
    private boolean prevOpenChat;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // Intercept system / server messages (join messages, /msg output, plugin text...)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true; // action bar, not chat
            return !ChatRouter.get().dispatch(message);
        });

        // Intercept signed player chat
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                !ChatRouter.get().dispatch(message));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("chatwindows")
                        .executes(ctx -> {
                            openLayoutLater();
                            return 1;
                        })
                        .then(ClientCommandManager.literal("edit").executes(ctx -> {
                            openLayoutLater();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("clear").executes(ctx -> {
                            ChatRouter.get().clearAll();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(ctx -> {
                            ConfigManager.load();
                            ChatRouter.get().rebuildAll();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("test")
                                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            // Injects a fake line so you can test filters offline.
                                            ChatRouter.get().dispatch(Text.literal(StringArgumentType.getString(ctx, "text")));
                                            return 1;
                                        })))));
    }

    /**
     * Hotkeys are polled from GLFW rather than registered as KeyBindings, because
     * the KeyBinding category API changed shape in recent versions. The key codes
     * live in chatwindows.json (GLFW codes) so they are still user-changeable.
     * They only fire when no screen is open, so they can't trigger while typing.
     */
    private void onTick(MinecraftClient client) {
        ChatRouter.get().tick();

        if (client.currentScreen != null) {
            prevLayout = prevNext = prevPrev = prevOpenChat = false;
            return;
        }

        ChatWindowsConfig cfg = ConfigManager.get();

        boolean layout = Keys.isDown(cfg.keyOpenLayout);
        if (layout && !prevLayout) client.setScreen(new ChatLayoutScreen(null));
        prevLayout = layout;

        boolean next = Keys.isDown(cfg.keyNextTab);
        if (next && !prevNext) ChatRouter.get().cycleTab(1);
        prevNext = next;

        boolean prev = Keys.isDown(cfg.keyPrevTab);
        if (prev && !prevPrev) ChatRouter.get().cycleTab(-1);
        prevPrev = prev;

        boolean openChat = Keys.isDown(cfg.keyOpenChat);
        if (openChat && !prevOpenChat) {
            client.setScreen(new ChatScreen(ChatRouter.get().activeSendPrefix(), false));
        }
        prevOpenChat = openChat;
    }

    private static void openLayoutLater() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new ChatLayoutScreen(null)));
    }
}
