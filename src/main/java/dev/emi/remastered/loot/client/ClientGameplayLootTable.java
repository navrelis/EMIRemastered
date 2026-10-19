package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ClientGameplayLootTable extends AbstractTextKeyParsingClientLootTable<ClientGameplayLootTable> {

    public static ClientGameplayLootTable INSTANCE = new ClientGameplayLootTable();
    private static final Identifier EMPTY = Identifier.of("blocks/empty");
    public final Identifier id;
    public final Identifier blockId;

    public ClientGameplayLootTable() {
        super();
        this.id = EMPTY;
        this.blockId = Identifier.of("air");
    }

    public ClientGameplayLootTable(Identifier id, Map<List<TextKey>, ClientRawPool> map) {
        super(map);
        this.id = id;
        String ns = id.getNamespace();
        String pth = id.getPath();
        int lastSlashIndex = pth.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            blockId = Identifier.of(ns, pth);
        } else {
            blockId = Identifier.of(ns, pth.substring(Math.min(lastSlashIndex + 1, pth.length())));
        }
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public boolean isEmpty() {
        return Objects.equals(id, EMPTY);
    }

    @Override
    void getSpecialTextKeyList(World world, Block block, List<Pair<Integer, Text>> inputList) {
    }

    @Override
    Pair<Identifier, Identifier> getBufId(PacketByteBuf buf) {
        return new Pair<>(getIdFromBuf(buf), EMPTY);
    }

    @Override
    ClientGameplayLootTable simpleTableToReturn(Pair<Identifier, Identifier> ids, PacketByteBuf buf) {
        ClientRawPool simplePool = new ClientRawPool(new HashMap<>());
        Object2FloatMap<ItemStack> simpleMap = new Object2FloatOpenHashMap<>();
        ItemStack simpleStack = new ItemStack(Registries.ITEM.getEntry(buf.readRegistryKey(RegistryKeys.ITEM)).map(RegistryEntry.Reference::value).orElse(Items.AIR));
        simpleMap.put(simpleStack, 100F);
        simplePool.map().put(new ArrayList<>(), simpleMap);
        Map<List<TextKey>, ClientRawPool> itemMap = new HashMap<>();
        itemMap.put(new ArrayList<>(), simplePool);
        return new ClientGameplayLootTable(ids.getLeft(), itemMap);
    }

    @Override
    ClientGameplayLootTable emptyTableToReturn() {
        return new ClientGameplayLootTable();
    }

    @Override
    ClientGameplayLootTable filledTableToReturn(Pair<Identifier, Identifier> ids, Map<List<TextKey>, ClientRawPool> itemMap) {
        return new ClientGameplayLootTable(ids.getLeft(), itemMap);
    }
}