package dev.emi.remastered.loot.server;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.networking.ChestLootPayload;
import dev.emi.remastered.loot.networking.LootNetworking;
import dev.emi.remastered.loot.structure.StructureLookup;
import dev.emi.remastered.loot.structure.StructureRef;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class ChestLootTableSender implements LootSender<SimpleLootPoolBuilder> {

    public ChestLootTableSender(Identifier id) {
        this.id = id;
        this.idToSend = LootSender.getIdToSend(id);
    }

    private final Identifier id;
    private final String idToSend;
    final List<SimpleLootPoolBuilder> builderList = new LinkedList<>();
    HashMap<ItemStack, Float> floatMap = new HashMap<>();

    @Override
    public void build() {
        builderList.forEach((builder) -> {
            builder.build();
            builder.builtMap.forEach((item, weight)-> {
                if (floatMap.containsKey(item)) {
                    float oldWeight = floatMap.getOrDefault(item, 0f);
                    floatMap.put(item, oldWeight + weight);
                } else {
                    floatMap.put(item, weight);
                }
            });
        });
    }

    @Override
    public String getId() {
        return idToSend;
    }

    /** The loot table id. */
    public Identifier getTableId() {
        return id;
    }

    @Override
    public void send(ServerPlayerEntity player) {
        send(player, LootSettings.chestStructures ? StructureLookup.ready(player.server) : null);
    }

    /** Sends the table with its structures from {@code structures}, or without them when it is null (not done yet). */
    public void send(ServerPlayerEntity player, @Nullable StructureLookup structures) {
        if (!LootNetworking.canSend(player, ChestLootPayload.TYPE)) return;
        PacketByteBuf buf = LootNetworking.newBuffer();
        buf.writeString(idToSend);
        buf.writeShort(floatMap.size());
        floatMap.forEach((item, floatWeight) -> {
            writeItemStack(buf, item, player.getServerWorld());
            buf.writeFloat(floatWeight);
        });
        if (LootSettings.chestStructures && structures != null) {
            // trailing block, clients of older versions ignore it
            StructureRef.write(buf, structures.structuresOf(id));
        }
        LootNetworking.send(player, buf, ChestLootPayload::new, idToSend);

    }

    @Override
    public void addBuilder(SimpleLootPoolBuilder builder) {
        builderList.add(builder);
    }

    @Override
    public List<SimpleLootPoolBuilder> getBuilders() {
        return builderList;
    }
}
