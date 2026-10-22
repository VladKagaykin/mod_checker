package net.fabricmc.AVCD.modcheck.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.fabricmc.AVCD.modcheck.server.ModListServer;

import java.util.Arrays;
import java.util.List;

public record ModListPacket(List<String> mods) implements CustomPayload {
    public static final Identifier MOD_LIST_PACKET = Identifier.of("modcheck", "mod_list");
    public static final CustomPayload.Id<ModListPacket> ID = new CustomPayload.Id<>(MOD_LIST_PACKET);
    public static final PacketCodec<PacketByteBuf, ModListPacket> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING.collect(PacketCodecs.toList()),
        ModListPacket::mods,
        ModListPacket::new
    );

    public ModListPacket(String[] mods) {
        this(Arrays.asList(mods));
    }

    public static ModListPacket createPacket(String[] mods) {
        return new ModListPacket(mods);
    }

    public static void registerServerReceiver() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                ModListServer.processClientMods(context.player(), payload.mods().toArray(new String[0]));
            });
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
