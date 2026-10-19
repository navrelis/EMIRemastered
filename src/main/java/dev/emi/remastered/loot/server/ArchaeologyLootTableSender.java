package dev.emi.remastered.loot.server;

import dev.emi.remastered.loot.networking.ArchaeologyLootPayload;
import dev.emi.remastered.loot.networking.LootNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ArchaeologyLootTableSender implements LootSender<SimpleLootPoolBuilder> {

	private final String idToSend;
	final List<SimpleLootPoolBuilder> builderList = new LinkedList<>();
	HashMap<ItemStack, Float> floatMap = new HashMap<>();

	public ArchaeologyLootTableSender(Identifier id) {
		this.idToSend = LootSender.getIdToSend(id);
	}

	@Override
	public String getId() {
		return idToSend;
	}

	@Override
	public void send(ServerPlayerEntity player) {
		if (!LootNetworking.canSend(player, ArchaeologyLootPayload.TYPE)) return;
		PacketByteBuf buf = LootNetworking.newBuffer();
		buf.writeString(idToSend);
		buf.writeShort(floatMap.size());
		floatMap.forEach((item, floatWeight) -> {
			writeItemStack(buf, item, player.getServerWorld());
			buf.writeFloat(floatWeight);
		});
		LootNetworking.send(player, buf, ArchaeologyLootPayload::new, idToSend);
	}

	@Override
	public void addBuilder(SimpleLootPoolBuilder builder) {
		builderList.add(builder);
	}

	@Override
	public List<SimpleLootPoolBuilder> getBuilders() {
		return builderList;
	}

	@Override
	public void build() {
		builderList.forEach(builder -> {
			builder.build();
			builder.builtMap.forEach((item, weight) -> {
				if(floatMap.containsKey(item)) {
					float oldWeight = floatMap.getOrDefault(item, 0f);
					floatMap.put(item, oldWeight + weight);
				} else {
					floatMap.put(item, weight);
				}
			});
		});
	}
}