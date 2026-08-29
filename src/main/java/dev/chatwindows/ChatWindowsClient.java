package dev.chatwindows;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.chatwindows.chat.ChatRouter;
import dev.chatwindows.config.ConfigManager;
import dev.chatwindows.gui.ChatLayoutScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatWindowsClient implements ClientModInitializer {

    public static final String MOD_ID = "chatwindows";
    public static final Logger LOGGER = LoggerFactory.getLogger("ChatWindows");

    public static KeyBinding openLayoutKey;
    public static KeyBinding nextTabKey;
    public static KeyBinding prevTabKey;
    public static KeyBinding openChatKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // NOTE: if your Minecraft build uses the newer KeyBinding.Category object
        // instead of a String category, swap the last argument for the category instance.
        openLayoutKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindows.layout", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.chatwindows"));
        nextTabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindows.next_tab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories.chatwindows"));
        prevTabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindows.prev_tab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories.chatwindows"));
        openChatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindows.open_chat", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.chatwindows"));

        // Intercept system / server messages (join messages, /msg output, plugin text...)
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true; // action bar, not chat
            return !ChatRouter.get().dispatch(message);
        });

        // Intercept signed player chat
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                !ChatRouter.get().dispatch(message));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatRouter.get().tick();
            while (openLayoutKey.wasPressed()) {
                client.setScreen(new ChatLayoutScreen(null));
            }
            while (nextTabKey.wasPressed()) ChatRouter.get().cycleTab(1);
            while (prevTabKey.wasPressed()) ChatRouter.get().cycleTab(-1);
            while (openChatKey.wasPressed()) {
                // Opens the chat box pre-filled with the active tab's prefix (e.g. "/pc ").
                client.setScreen(new net.minecraft.client.gui.screen.ChatScreen(ChatRouter.get().activeSendPrefix()));
            }
        });

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

    private static void openLayoutLater() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new ChatLayoutScreen(null)));
    }
}
