package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * One block loot table, in the same binary layout the upstream mod used: see the matching table sender and client table.
 */
public record BlockLootPayload(byte[] data) implements CustomPayload {
	public static final Id<BlockLootPayload> TYPE = new Id<>(EMILoot.identity("block"));
	public static final PacketCodec<PacketByteBuf, BlockLootPayload> CODEC = PacketCodec.of(BlockLootPayload::write, BlockLootPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBytes(data);
	}

	private static BlockLootPayload read(PacketByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new BlockLootPayload(data);
	}

	@Override
	public Id<BlockLootPayload> getId() {
		return TYPE;
	}
}
