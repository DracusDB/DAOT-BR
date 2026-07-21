package net.dracus.daotbr.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ZoneUpdatePayload(double centerX, double centerZ, double radius) implements CustomPayload {
    public static final CustomPayload.Id<ZoneUpdatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("daotbr", "zone_update"));
    public static final PacketCodec<RegistryByteBuf, ZoneUpdatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, ZoneUpdatePayload::centerX,
            PacketCodecs.DOUBLE, ZoneUpdatePayload::centerZ,
            PacketCodecs.DOUBLE, ZoneUpdatePayload::radius,
            ZoneUpdatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}