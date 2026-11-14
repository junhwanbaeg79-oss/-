package com.example.swordforge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record HammerStrikePayload(BlockPos anvilPos) implements CustomPayload {
    public static final CustomPayload.Id<HammerStrikePayload> ID = new CustomPayload.Id<>(Identifier.of("swordforge", "hammer_strike"));
    public static final PacketCodec<RegistryByteBuf, HammerStrikePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, HammerStrikePayload::anvilPos,
            HammerStrikePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
