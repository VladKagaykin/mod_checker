package net.fabricmc.AVCD.modcheck;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.AVCD.modcheck.server.ModListServer;
import net.fabricmc.AVCD.modcheck.config.ModsConfig;
import net.fabricmc.AVCD.modcheck.network.ModListPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModCheck implements ModInitializer {
    public static final String MOD_ID = "modcheck";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ModCheck initialized!");
        
        // Register network packet
        ModListPacket.registerServerReceiver();
        
        // Initialize timeout check system
        ModListServer.initialize();
        
        // Load configuration on server start
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ModsConfig.loadConfig();
            LOGGER.info("ModCheck configuration loaded");
        });
        
        // Start timer when player connects
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                ModListServer.scheduleTimeoutCheck(handler.player);
            });
        });
    }
}