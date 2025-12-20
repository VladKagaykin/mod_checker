package net.fabricmc.example.modcheck.server;

import net.fabricmc.example.modcheck.config.ModsConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

public class ModListServer {
    private static final Map<UUID, Long> pendingChecks = new HashMap<>();
    private static final Map<UUID, Boolean> receivedPackets = new HashMap<>();
    private static final long TIMEOUT_TICKS = 100L; // 5 секунд (20 тиков/сек)
    
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
                    player.networkHandler.disconnect(Text.of(
                        "§c[ModCheck] Не получена проверка модов!\n" +
                        "§7Убедитесь, что у вас установлен мод ModChecker"
                    ));
                }
                toRemove.add(playerId);
            }
        }
        
        for (UUID playerId : toRemove) {
            pendingChecks.remove(playerId);
            receivedPackets.remove(playerId);
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
            player.networkHandler.disconnect(Text.of(
                "§c[ModCheck] Требуется мод ModChecker!\n" +
                "§7Скачайте мод для входа на сервер\n" +
                "§7ID мода: §fmodcheck"
            ));
            return;
        }
        
        // Проверяем белый список
        List<String> forbiddenMods = new ArrayList<>();
        for (String modId : clientMods) {
            // Игнорируем системные моды
            if (modId.equals("java") || modId.equals("minecraft") || 
                modId.equals("fabricloader") || modId.equals("fabric-api-base") ||
                modId.equals("fabric")) {
                continue;
            }
            
            if (!ModsConfig.isModAllowed(modId)) {
                forbiddenMods.add(modId);
            }
        }
        
        if (!forbiddenMods.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("§c[ModCheck] Запрещенные моды обнаружены:\n\n§7");
            
            for (String modId : forbiddenMods) {
                message.append("• ").append(modId).append("\n");
            }
            
            message.append("\n§fУдалите эти моды для входа на сервер");
            player.networkHandler.disconnect(Text.of(message.toString()));
            return;
        }
        
        // Все проверки пройдены
        player.sendMessage(Text.of("§a[ModCheck] ✓ Все моды проверены"), false);
    }
    
    public static void scheduleTimeoutCheck(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        pendingChecks.put(playerId, player.getServer().getOverworld().getTime());
        receivedPackets.put(playerId, false);
    }
}