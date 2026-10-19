package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.structure.StructureRef;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.List;

public class ClientChestLootTable implements LootReceiver {

    public static ClientChestLootTable INSTANCE = new ClientChestLootTable();
    public final Identifier id;
    public final Object2FloatMap<ItemStack> items;
    /** Structures the chest is found in, empty when the server did not say (yet: they can follow, see {@link #setStructures}). */
    private volatile List<StructureRef> structures;

    public ClientChestLootTable() {
        this.id = Identifier.of("empty");
        this.items = new Object2FloatOpenHashMap<>();
        this.structures = List.of();
    }

    public ClientChestLootTable(Identifier id, Object2FloatMap<ItemStack> map) {
        this(id, map, List.of());
    }

    public ClientChestLootTable(Identifier id, Object2FloatMap<ItemStack> map, List<StructureRef> structures) {
        this.id = id;
        this.items = map;
        this.structures = structures;
    }

    /** Structures the chest is found in; read when a page or the Item Info is built, so a late update shows there. */
    public List<StructureRef> getStructures() {
        return structures;
    }

    /** The structures the server sent after the table ({@code ChestStructuresPayload}). Client thread. */
    public void setStructures(List<StructureRef> structures) {
        this.structures = List.copyOf(structures);
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public LootReceiver fromBuf(PacketByteBuf buf) {
        Identifier id = AbstractTextKeyParsingClientLootTable.getIdFromBuf(buf);
        int mapCount = buf.readShort();
        Object2FloatMap<ItemStack> itemMap = new Object2FloatOpenHashMap<>();
        for (int i = 0; i < mapCount; i++) {
            ItemStack item = readItemStack(buf);
            float itemWeight = buf.readFloat();
            if (item.isOf(Items.AIR)) continue;
            itemMap.put(item, itemWeight);
        }
        List<StructureRef> structures = List.of();
        try {
            structures = StructureRef.read(buf);
        } catch (RuntimeException ignored) {
            // the structure block is optional extra information, never lose the items because of it
        }
        return new ClientChestLootTable(id, itemMap, structures);
    }
}
