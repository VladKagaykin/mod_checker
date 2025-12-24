package net.fabricmc.example.modcheck.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.fabricmc.example.modcheck.network.ModListPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModCheckClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModCheckClient");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("ModCheck Client инициализирован!");
        ClientPlayConnectionEvents.JOIN.register(this::onJoinServer);
    }
    
    private void onJoinServer(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        // Собираем список всех модов клиента
        String[] modList = FabricLoader.getInstance().getAllMods().stream()
            .map(modContainer -> modContainer.getMetadata().getId())
            .toArray(String[]::new);
        
        LOGGER.info("Отправляю список модов на сервер ({} модов)", modList.length);
        
        // Отправляем пакет на сервер
        sender.sendPacket(ModListPacket.MOD_LIST_PACKET, ModListPacket.createPacket(modList));
    }
}