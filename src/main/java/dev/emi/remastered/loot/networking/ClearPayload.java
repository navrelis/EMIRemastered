package dev.emi.remastered.loot.networking;

import dev.emi.remastered.loot.EMILoot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Tells the client to drop all received loot tables. Sent before the tables of a (re)sync.
 */
public class ClearPayload implements CustomPayload {
	public static final ClearPayload INSTANCE = new ClearPayload();
	public static final Id<ClearPayload> TYPE = new CustomPayload.Id<>(EMILoot.identity("clear_loot"));
	public static final PacketCodec<ByteBuf, ClearPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<ClearPayload> getId() {
		return TYPE;
	}
}
