package net.fabricmc.example.modcheck;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.example.modcheck.server.ModListServer;
import net.fabricmc.example.modcheck.config.ModsConfig;
import net.fabricmc.example.modcheck.network.ModListPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModCheck implements ModInitializer {
    public static final String MOD_ID = "modcheck";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ModCheck инициализирован!");
        
        // Регистрируем сетевой пакет
        ModListPacket.registerServerReceiver();
        
        // Инициализируем систему проверки таймаутов
        ModListServer.initialize();
        
        // Загружаем конфигурацию при запуске сервера
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ModsConfig.loadConfig();
            LOGGER.info("Конфигурация ModCheck загружена");
        });
        
        // Запускаем таймер проверки при подключении игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                ModListServer.scheduleTimeoutCheck(handler.player);
            });
        });
    }
}