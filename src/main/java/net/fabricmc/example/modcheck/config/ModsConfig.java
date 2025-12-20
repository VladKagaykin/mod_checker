package net.fabricmc.example.modcheck.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.lang.reflect.Type;
import java.util.*;

public class ModsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("modcheck-allowed.json");
    private static final Set<String> ALLOWED_MODS = new HashSet<>();
    
    static {
        // Всегда разрешенные системные моды
        ALLOWED_MODS.add("minecraft");
        ALLOWED_MODS.add("java");
        ALLOWED_MODS.add("fabricloader");
        ALLOWED_MODS.add("fabric-api");
        ALLOWED_MODS.add("fabric");
        ALLOWED_MODS.add("modcheck");
    }
    
    public static void loadConfig() {
        File configFile = CONFIG_PATH.toFile();
        
        try {
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }
            
            try (Reader reader = new FileReader(configFile)) {
                Type type = new TypeToken<List<String>>(){}.getType();
                List<String> mods = GSON.fromJson(reader, type);
                
                if (mods != null) {
                    for (String mod : mods) {
                        if (mod != null && !mod.trim().isEmpty()) {
                            ALLOWED_MODS.add(mod.trim().toLowerCase());
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфига ModCheck: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createDefaultConfig(File configFile) throws IOException {
        List<String> defaultMods = Arrays.asList(
            "fabric-api-base",
            "fabric-api-lookup-api-v1",
            "fabric-biome-api-v1",
            "fabric-block-api-v1",
            "fabric-command-api-v2",
            "fabric-content-registries-v0",
            "fabric-convention-tags-v1",
            "fabric-crash-report-info-v1",
            "fabric-data-generation-api-v1",
            "fabric-dimensions-v1",
            "fabric-entity-events-v1",
            "fabric-events-interaction-v0",
            "fabric-game-rule-api-v1",
            "fabric-item-api-v1",
            "fabric-item-group-api-v1",
            "fabric-lifecycle-events-v1",
            "fabric-loot-api-v2",
            "fabric-message-api-v1",
            "fabric-mining-level-api-v1",
            "fabric-networking-api-v1",
            "fabric-object-builder-api-v1",
            "fabric-recipe-api-v1",
            "fabric-registry-sync-v0",
            "fabric-rendering-v1",
            "fabric-resource-loader-v0",
            "fabric-screen-api-v1",
            "fabric-screen-handler-api-v1",
            "fabric-transfer-api-v1",
            "fabric-transitive-access-wideners-v1"
        );
        
        configFile.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(defaultMods, writer);
        }
    }
    
    public static boolean isModAllowed(String modId) {
        return ALLOWED_MODS.contains(modId.toLowerCase());
    }
    
    public static Set<String> getAllowedMods() {
        return new HashSet<>(ALLOWED_MODS);
    }
}