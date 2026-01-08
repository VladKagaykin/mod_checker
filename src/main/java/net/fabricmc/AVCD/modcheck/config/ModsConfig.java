package net.fabricmc.AVCD.modcheck.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.lang.reflect.Type;
import java.util.*;

public class ModsConfig {
    public enum Mode {
        WHITELIST("Whitelist"),
        BANLIST("Banlist");
        
        private final String displayName;
        
        Mode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("modcheck.json");
    
    private static Mode currentMode = Mode.WHITELIST;
    private static final Set<String> CONFIG_MODS = new HashSet<>();
    
    // Always allowed system mods (even in banlist mode)
    private static final Set<String> SYSTEM_MODS = new HashSet<>();
    
    static {
        // System mods that are always allowed
        SYSTEM_MODS.add("minecraft");
        SYSTEM_MODS.add("java");
        SYSTEM_MODS.add("fabricloader");
        SYSTEM_MODS.add("fabric-api");
        SYSTEM_MODS.add("fabric");
        SYSTEM_MODS.add("modcheck");
    }
    
    public static void loadConfig() {
        File configFile = CONFIG_PATH.toFile();
        
        try {
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }
            
            try (Reader reader = new FileReader(configFile)) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                
                // Load mode
                if (config.has("mode")) {
                    String modeStr = config.get("mode").getAsString().toUpperCase();
                    try {
                        currentMode = Mode.valueOf(modeStr);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid mode in config: " + modeStr + ". Using WHITELIST.");
                        currentMode = Mode.WHITELIST;
                    }
                }
                
                // Load mods list
                CONFIG_MODS.clear();
                if (config.has("mods")) {
                    Type type = new TypeToken<List<String>>(){}.getType();
                    List<String> mods = GSON.fromJson(config.get("mods"), type);
                    
                    if (mods != null) {
                        for (String mod : mods) {
                            if (mod != null && !mod.trim().isEmpty()) {
                                CONFIG_MODS.add(mod.trim().toLowerCase());
                            }
                        }
                    }
                }
                
                System.out.println("[ModCheck] Config loaded - Mode: " + currentMode.getDisplayName() + 
                                 ", Mods: " + CONFIG_MODS.size());
            }
            
        } catch (IOException e) {
            System.err.println("Error loading ModCheck config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createDefaultConfig(File configFile) throws IOException {
        JsonObject config = new JsonObject();
        config.addProperty("mode", "whitelist");
        
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
        
        config.add("mods", GSON.toJsonTree(defaultMods));
        
        configFile.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        }
    }
    
    public static boolean isModAllowed(String modId) {
        modId = modId.toLowerCase();
        
        // System mods are always allowed
        if (SYSTEM_MODS.contains(modId)) {
            return true;
        }
        
        boolean isInConfig = CONFIG_MODS.contains(modId);
        
        if (currentMode == Mode.WHITELIST) {
            // In whitelist mode, only mods in config are allowed
            return isInConfig;
        } else {
            // In banlist mode, mods in config are NOT allowed
            return !isInConfig;
        }
    }
    
    public static Mode getCurrentMode() {
        return currentMode;
    }
    
    public static Set<String> getConfigMods() {
        return new HashSet<>(CONFIG_MODS);
    }
}