package net.fabricmc.AVCD.modcheck.server;

import net.fabricmc.AVCD.modcheck.config.ModsConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.network.ClientConnection;
import java.lang.reflect.Field;
import java.util.*;

public class ModListServer {
    private static final Map<UUID, Long> pendingChecks = new HashMap<>();
    private static final Map<UUID, Boolean> receivedPackets = new HashMap<>();
    private static final Map<UUID, String> playerLocales = new HashMap<>();
    private static final long TIMEOUT_TICKS = 100L; // 5 секунд (20 тиков/сек)
    
    // Кэш для reflection чтобы не делать его каждый раз
    private static Field connectionField = null;
    private static Field localeField = null;
    private static boolean reflectionInitialized = false;
    
    // Английские сообщения по умолчанию
    private static final Map<String, String> ENGLISH_MESSAGES = new HashMap<>();
    private static final Map<String, String> RUSSIAN_MESSAGES = new HashMap<>();
    
    static {
        initializeMessages();
        initializeReflection();
    }
    
    private static void initializeMessages() {
        // English messages
        ENGLISH_MESSAGES.put("error.no_mod_list", 
            "§c[ModCheck] Mod list not received!\n" +
            "§7Make sure you have ModChecker mod installed");
        
        ENGLISH_MESSAGES.put("error.no_mod_checker", 
            "§c[ModCheck] ModChecker mod required!\n" +
            "§7Download the mod to join the server\n" +
            "§7Mod ID: §fmodcheck");
        
        ENGLISH_MESSAGES.put("error.unauthorized_mods", 
            "§c[ModCheck] Unauthorized mods detected:\n\n§7");
        
        ENGLISH_MESSAGES.put("error.whitelist_mode", 
            "Server is running in §eWHITELIST§7 mode.\n" +
            "Only mods from the configuration are allowed.\n\n");
        
        ENGLISH_MESSAGES.put("error.banlist_mode", 
            "Server is running in §eBANLIST§7 mode.\n" +
            "These mods are not allowed on the server.\n\n");
        
        ENGLISH_MESSAGES.put("error.remove_mods", 
            "\n§fRemove these mods to join the server");
        
        ENGLISH_MESSAGES.put("success.all_mods_checked", 
            "§a[ModCheck] ✓ All mods checked");
        
        // Russian messages
        RUSSIAN_MESSAGES.put("error.no_mod_list", 
            "§c[ModCheck] Не получена проверка модов!\n" +
            "§7Убедитесь, что у вас установлен мод ModChecker");
        
        RUSSIAN_MESSAGES.put("error.no_mod_checker", 
            "§c[ModCheck] Требуется мод ModChecker!\n" +
            "§7Скачайте мод для входа на сервер\n" +
            "§7ID мода: §fmodcheck");
        
        RUSSIAN_MESSAGES.put("error.unauthorized_mods", 
            "§c[ModCheck] Неавторизованные моды обнаружены:\n\n§7");
        
        RUSSIAN_MESSAGES.put("error.whitelist_mode", 
            "Сервер работает в режиме §eБЕЛОГО СПИСКА§7.\n" +
            "Только моды из конфигурации разрешены.\n\n");
        
        RUSSIAN_MESSAGES.put("error.banlist_mode", 
            "Сервер работает в режиме §eЧЕРНОГО СПИСКА§7.\n" +
            "Эти моды не разрешены на сервере.\n\n");
        
        RUSSIAN_MESSAGES.put("error.remove_mods", 
            "\n§fУдалите эти моды для входа на сервер");
        
        RUSSIAN_MESSAGES.put("success.all_mods_checked", 
            "§a[ModCheck] ✓ Все моды проверены");
    }
    
    private static void initializeReflection() {
        if (reflectionInitialized) return;
        
        try {
            // Инициализируем reflection поля один раз
            connectionField = Class.forName("net.minecraft.server.network.ServerPlayNetworkHandler")
                .getDeclaredField("connection");
            connectionField.setAccessible(true);
            
            localeField = Class.forName("net.minecraft.network.ClientConnection")
                .getDeclaredField("locale");
            localeField.setAccessible(true);
            
            reflectionInitialized = true;
            System.out.println("[ModCheck] Reflection initialized successfully");
            
        } catch (Exception e) {
            System.err.println("[ModCheck] Failed to initialize reflection: " + e.getMessage());
            reflectionInitialized = false;
        }
    }
    
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ModListServer::onServerTick);
    }
    
    private static void onServerTick(MinecraftServer server) {
        long currentTime = server.getOverworld().getTime();
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, Long> entry : pendingChecks.entrySet()) {
            UUID playerId = entry.getKey();
            Long startTime = entry.getValue();
            
            if (startTime != null && currentTime - startTime > TIMEOUT_TICKS) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player != null && !receivedPackets.getOrDefault(playerId, false)) {
                    String message = getMessage(player, "error.no_mod_list");
                    player.networkHandler.disconnect(Text.of(message));
                }
                toRemove.add(playerId);
            }
        }
        
        for (UUID playerId : toRemove) {
            pendingChecks.remove(playerId);
            receivedPackets.remove(playerId);
            playerLocales.remove(playerId);
        }
    }
    
    public static void processClientMods(ServerPlayerEntity player, String[] clientMods) {
        UUID playerId = player.getUuid();
        receivedPackets.put(playerId, true);
        pendingChecks.remove(playerId);
        
        // Проверяем, установлен ли наш мод
        boolean hasModChecker = false;
        for (String modId : clientMods) {
            if (modId.equals("modcheck")) {
                hasModChecker = true;
                break;
            }
        }
        
        if (!hasModChecker) {
            String message = getMessage(player, "error.no_mod_checker");
            player.networkHandler.disconnect(Text.of(message));
            return;
        }
        
        // Проверяем моды по текущему режиму
        List<String> forbiddenMods = new ArrayList<>();
        for (String modId : clientMods) {
            if (!ModsConfig.isModAllowed(modId)) {
                forbiddenMods.add(modId);
            }
        }
        
        if (!forbiddenMods.isEmpty()) {
            StringBuilder message = new StringBuilder();
            
            // Основное сообщение
            message.append(getMessage(player, "error.unauthorized_mods"));
            
            // Режим работы сервера
            if (ModsConfig.getCurrentMode() == ModsConfig.Mode.WHITELIST) {
                message.append(getMessage(player, "error.whitelist_mode"));
            } else {
                message.append(getMessage(player, "error.banlist_mode"));
            }
            
            // Список запрещенных модов
            for (String modId : forbiddenMods) {
                message.append("• ").append(modId).append("\n");
            }
            
            // Заключительное сообщение
            message.append(getMessage(player, "error.remove_mods"));
            
            player.networkHandler.disconnect(Text.of(message.toString()));
            return;
        }
        
        // Все проверки пройдены
        String successMessage = getMessage(player, "success.all_mods_checked");
        player.sendMessage(Text.of(successMessage), false);
    }
    
    private static String getPlayerLocale(ServerPlayerEntity player) {
        // Если уже определили локаль для этого игрока, возвращаем ее
        UUID playerId = player.getUuid();
        if (playerLocales.containsKey(playerId)) {
            return playerLocales.get(playerId);
        }
        
        String locale = "en_us"; // Значение по умолчанию
        
        try {
            // Используем reflection для получения локали из ClientConnection
            if (reflectionInitialized && connectionField != null && localeField != null) {
                Object connection = connectionField.get(player.networkHandler);
                if (connection != null) {
                    Object localeObj = localeField.get(connection);
                    if (localeObj instanceof String) {
                        locale = ((String) localeObj).toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ModCheck] Error getting locale via reflection: " + e.getMessage());
        }
        
        // Сохраняем локаль в кэш
        playerLocales.put(playerId, locale);
        return locale;
    }
    
    private static String getMessage(ServerPlayerEntity player, String key) {
        // Получаем локаль игрока
        String playerLocale = getPlayerLocale(player);
        
        // Определяем язык (проверяем русские локали)
        boolean isRussian = playerLocale.startsWith("ru") || 
                           playerLocale.contains("russian");
        
        if (isRussian) {
            String russianMessage = RUSSIAN_MESSAGES.get(key);
            if (russianMessage != null) {
                return russianMessage;
            }
        }
        
        // По умолчанию английский
        return ENGLISH_MESSAGES.getOrDefault(key, key);
    }
    
    public static void scheduleTimeoutCheck(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        pendingChecks.put(playerId, player.getServer().getOverworld().getTime());
        receivedPackets.put(playerId, false);
        
        // Предварительно определяем локаль
        getPlayerLocale(player);
    }
}