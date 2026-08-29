package dev.chatwindows.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.chatwindows.ChatWindowsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static ChatWindowsConfig config;

    private ConfigManager() {}

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("chatwindows.json");
    }

    public static ChatWindowsConfig get() {
        if (config == null) load();
        return config;
    }

    public static void load() {
        Path path = file();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ChatWindowsConfig.class);
            } catch (Exception e) {
                ChatWindowsClient.LOGGER.error("[ChatWindows] Failed to read config, using defaults", e);
                config = null;
            }
        }
        if (config == null) {
            config = ChatWindowsConfig.createDefault();
            save();
        }
        config.validate();
    }

    public static void save() {
        if (config == null) return;
        config.validate();
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            ChatWindowsClient.LOGGER.error("[ChatWindows] Failed to write config", e);
        }
    }

    public static void reset() {
        config = ChatWindowsConfig.createDefault();
        save();
    }
}
