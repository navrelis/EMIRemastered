package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * One mob loot table, in the same binary layout the upstream mod used: see the matching table sender and client table.
 */
public record MobLootPayload(byte[] data) implements CustomPayload {
	public static final Id<MobLootPayload> TYPE = new Id<>(EMILoot.identity("mob"));
	public static final PacketCodec<PacketByteBuf, MobLootPayload> CODEC = PacketCodec.of(MobLootPayload::write, MobLootPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static MobLootPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new MobLootPayload(data);
	}

	@Override
	public Id<MobLootPayload> getId() {
		return TYPE;
	}
}
