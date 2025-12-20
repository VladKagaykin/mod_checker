package net.fabricmc.example.modcheck.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.example.modcheck.server.ModListServer;

public class ModListPacket {
    public static final Identifier MOD_LIST_PACKET = new Identifier("modcheck", "mod_list");
    
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(MOD_LIST_PACKET, ModListPacket::receive);
    }
    
    private static void receive(MinecraftServer server, ServerPlayerEntity player, 
                               ServerPlayNetworkHandler handler, PacketByteBuf buf, 
                               PacketSender responseSender) {
        int modCount = buf.readInt();
        String[] clientMods = new String[modCount];
        
        for (int i = 0; i < modCount; i++) {
            clientMods[i] = buf.readString();
        }
        
        server.execute(() -> {
            ModListServer.processClientMods(player, clientMods);
        });
    }
    
    public static PacketByteBuf createPacket(String[] mods) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(mods.length);
        for (String mod : mods) {
            buf.writeString(mod);
        }
        return buf;
    }
}