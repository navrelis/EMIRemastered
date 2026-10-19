package dev.emi.remastered.loot.parser;

import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.mixin.CombinedEntryAccessor;
import dev.emi.remastered.loot.mixin.ConditionalLootFunctionAccessor;
import dev.emi.remastered.loot.mixin.ItemEntryAccessor;
import dev.emi.remastered.loot.mixin.LeafEntryAccessor;
import dev.emi.remastered.loot.mixin.LootPoolAccessor;
import dev.emi.remastered.loot.mixin.LootPoolEntryAccessor;
import dev.emi.remastered.loot.mixin.LootTableAccessor;
import dev.emi.remastered.loot.mixin.LootTableEntryAccessor;
import dev.emi.remastered.loot.mixin.TagEntryAccessor;
import dev.emi.remastered.loot.networking.ClearPayload;
import dev.emi.remastered.loot.networking.LootNetworking;
import dev.emi.remastered.loot.parser.processor.NumberProcessors;
import dev.emi.remastered.loot.parser.registry.LootParserRegistry;
import dev.emi.remastered.loot.server.ArchaeologyLootTableSender;
import dev.emi.remastered.loot.server.BlockLootTableSender;
import dev.emi.remastered.loot.server.ComplexLootPoolBuilder;
import dev.emi.remastered.loot.server.NestedLootTableResults;
import dev.emi.remastered.loot.server.SimpleLootPoolBuilder;
import dev.emi.remastered.loot.server.ChestLootTableSender;
import dev.emi.remastered.loot.server.ChestStructureSync;
import dev.emi.remastered.loot.structure.StructureLookup;
import dev.emi.remastered.loot.server.GameplayLootTableSender;
import dev.emi.remastered.loot.server.LootBuilder;
import dev.emi.remastered.loot.server.LootSender;
import dev.emi.remastered.loot.server.MobLootTableSender;
import dev.emi.remastered.loot.server.ServerResourceData;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.GroupEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.loot.entry.SequenceEntry;
import net.minecraft.loot.entry.TagEntry;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LootTableParser {

    private static final Map<Identifier, ChestLootTableSender> chestSenders = new HashMap<>();
    private static final Map<Identifier, BlockLootTableSender> blockSenders = new HashMap<>();
    private static final Map<Identifier, MobLootTableSender> mobSenders = new HashMap<>();
    private static final Map<Identifier, GameplayLootTableSender> gameplaySenders = new HashMap<>();
    private static final Map<Identifier, ArchaeologyLootTableSender> archaeologySenders = new HashMap<>();
    public static final Object2BooleanMap<PostProcessor> postProcessors;
    public static String currentTable = "none";
    public static List<Identifier> parsedDirectDrops = new LinkedList<>();
    public static boolean hasParsedLootTables = false;
    public static Registry<LootTable> lootManager = null;
    public static RegistryOps<JsonElement> registryOps = null;

    private static final List<LootCondition> emptyConditions = List.of();

    static {
        Object2BooleanOpenHashMap<PostProcessor> map = new Object2BooleanOpenHashMap<>();
        for (var value : PostProcessor.values()) {
            map.put(value, false);
        }
        postProcessors = map;
    }

    private static boolean hasPostProcessed() {
        for (boolean bl: postProcessors.values()) {
            if (!bl) return false;
        }
        return true;
    }

    /** The chest loot tables of the current data. Server thread. */
    public static Collection<ChestLootTableSender> chestSenders() {
        return Collections.unmodifiableCollection(chestSenders.values());
    }

    public void registerServer(ServerPlayerEntity player) {
        if (!hasPostProcessed()) {
            EMILoot.LOGGER.warn("Post-processing not completed for some reason, completing now...");
            for (PostProcessor process: PostProcessor.values()) {
                postProcess(process);
            }
            EMILoot.LOGGER.warn("Post-processing complete!");
        }
        if (!LootNetworking.canSend(player, ClearPayload.TYPE)) return;
        ServerPlayNetworking.send(player, ClearPayload.INSTANCE);
        if (LootSettings.parseChestLoot) {
            // never waits for the structure lookup: without it the structures follow once it is done
            StructureLookup structures = ChestStructureSync.begin(player, chestSenders.values());
            chestSenders.forEach((id, chestSender) -> chestSender.send(player, structures));
        }
        if (LootSettings.parseBlockLoot)
            blockSenders.forEach((id, blockSender) -> blockSender.send(player));
        if (LootSettings.parseMobLoot)
            mobSenders.forEach((id, mobSender) -> mobSender.send(player));
        if (LootSettings.parseGameplayLoot)
            gameplaySenders.forEach((id, gameplaySender) -> gameplaySender.send(player));
        if (LootSettings.parseArchaeologyLoot)
            archaeologySenders.forEach((id, archaeologySender) -> archaeologySender.send(player));
    }

    public static void parseLootTables(Registry<LootTable> manager) {
        LootTableParser.lootManager = manager;
        parsedDirectDrops = new LinkedList<>();
        chestSenders.clear();
        blockSenders.clear();
        mobSenders.clear();
        gameplaySenders.clear();
        archaeologySenders.clear();
        try {
			long start = System.currentTimeMillis();
			EMILoot.LOGGER.info("parsing loot tables");
            manager.getEntrySet().forEach(entry -> {
                LootTable table = entry.getValue();
                if (!((LootTableAccessor) table).emi_loot$getPools().isEmpty()) {
                    parseLootTable(entry.getKey().getValue(), table);
                }
            });
            if (LootSettings.parseMobLoot) {
                Identifier chk = Identifier.ofVanilla("pig");
                Registries.ENTITY_TYPE.getEntrySet().forEach((entry) -> {
                    EntityType<?> type = entry.getValue();
                    if (type == EntityType.SHEEP) {
                        for (Identifier sheepId : ServerResourceData.SHEEP_TABLES) {
                            parseEntityType(manager, type, sheepId, chk, emptyConditions);
                        }
                    }
                    //perform shearing table checks. May have to improve for later updates.
                    Identifier shearingId = Identifier.of(entry.getKey().getValue().getNamespace(), "shearing/" + entry.getKey().getValue().getPath());
                    LootTable shearingTable = manager.get(shearingId);
                    if (shearingTable != null && shearingTable != LootTable.EMPTY) {
                        parseEntityType(manager, type, shearingId, chk, List.of(MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS)).build()));
                    }
                    parseEntityType(manager, type, type.getLootTableId().getValue(), chk, emptyConditions);
                });
            }
            Multimap<Identifier, LootTable> missedDrops = ServerResourceData.getMissedDirectDrops(parsedDirectDrops);
            for (Map.Entry<Identifier, LootTable> entry : missedDrops.entries()) {
                if (EMILoot.isDebug()) EMILoot.LOGGER.info("parsing missed direct drop table: {}", entry.getKey());
                parseLootTable(entry.getKey(), entry.getValue());
            }
			EMILoot.LOGGER.info("finished parsing loot tables in {}", System.currentTimeMillis() - start);
            hasParsedLootTables = true;
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical unhandled error encountered while parsing loot tables. Results may be incomplete.");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    private static void parseLootTable(Identifier id, LootTable lootTable) {
        if (ServerResourceData.skipTable(lootTable, id)) return;
        currentTable = id.toString();
        try {
            LootContextType type = effectiveType(id, lootTable.getType());
            if (type == LootContextTypes.CHEST && LootSettings.parseChestLoot) {
                chestSenders.put(id, parseChestLootTable(lootTable, id));
            } else if (type == LootContextTypes.BLOCK && LootSettings.parseBlockLoot) {
                blockSenders.put(id, parseBlockLootTable(lootTable, id));
            } else if ((type == LootContextTypes.FISHING || type == LootContextTypes.GIFT || type == LootContextTypes.BARTER) && LootSettings.parseGameplayLoot) {
                gameplaySenders.put(id, parseGameplayLootTable(lootTable, id));
            } else if ((type == LootContextTypes.ARCHAEOLOGY && LootSettings.parseArchaeologyLoot)) {
                archaeologySenders.put(id, parseArchaeologyTable(lootTable, id));
            }
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while parsing Loot Table {}", id);
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    /**
     * Mods that replace or build a table without a context type leave it generic or empty. Fall back on the
     * folder the table lives in so such tables are not lost.
     */
    private static LootContextType effectiveType(Identifier id, LootContextType type) {
        if (type != LootContextTypes.GENERIC && type != LootContextTypes.EMPTY) return type;
        String path = id.getPath();
        if (path.startsWith("blocks/")) return LootContextTypes.BLOCK;
        if (path.startsWith("chests/")) return LootContextTypes.CHEST;
        if (path.startsWith("archaeology/")) return LootContextTypes.ARCHAEOLOGY;
        if (path.startsWith("gameplay/fishing")) return LootContextTypes.FISHING;
        if (path.startsWith("gameplay/")) return LootContextTypes.GIFT;
        return type;
    }

    public static void postProcess(PostProcessor process) {
        if (!hasParsedLootTables) return;
        try {
            for (LootSender<?> sender : chestSenders.values()) {
                for (LootBuilder builder : sender.getBuilders()) {
                    for (LootPoolEntry entry : builder.getEntriesToPostProcess(process)) {
                        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Post-processing builder in chest sender: {}", sender.getId());
                        parseLootPoolEntry(builder, entry, process);
                    }
                }
                sender.build();
            }
            for (LootSender<?> sender : blockSenders.values()) {
                for (LootBuilder builder : sender.getBuilders()) {
                    for (LootPoolEntry entry : builder.getEntriesToPostProcess(process)) {
                        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Post-processing builder in block sender: {}", sender.getId());
                        parseLootPoolEntry(builder, entry, process);
                    }
                }
                sender.build();
            }
            for (LootSender<?> sender : mobSenders.values()) {
                for (LootBuilder builder : sender.getBuilders()) {
                    for (LootPoolEntry entry : builder.getEntriesToPostProcess(process)) {
                        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Post-processing builder in mob sender: {}", sender.getId());
                        parseLootPoolEntry(builder, entry, process);
                    }
                }
                sender.build();
            }
            for (LootSender<?> sender : gameplaySenders.values()) {
                for (LootBuilder builder : sender.getBuilders()) {
                    for (LootPoolEntry entry : builder.getEntriesToPostProcess(process)) {
                        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Post-processing builder in gameplay sender: {}", sender.getId());
                        parseLootPoolEntry(builder, entry, process);
                    }
                }
                sender.build();
            }
            for (LootSender<?> sender : archaeologySenders.values()) {
                for (LootBuilder builder : sender.getBuilders()) {
                    for (LootPoolEntry entry : builder.getEntriesToPostProcess(process)) {
                        if (EMILoot.isDebug()) EMILoot.LOGGER.info("Post-processing builder in archaeology sender: {}", sender.getId());
                        parseLootPoolEntry(builder, entry, process);
                    }
                }
                sender.build();
            }
            postProcessors.put(process, true);
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while post-processing for {}", process);
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    private static void parseEntityType(Registry<LootTable> manager, EntityType<?> type, Identifier mobTableId, Identifier fallback, List<LootCondition> addedConditions) {
        try {
            Identifier mobId = Registries.ENTITY_TYPE.getId(type);
            LootTable mobTable = manager.get(mobTableId);
			if (ServerResourceData.skipTable(mobTable, mobTableId)) return;
			if ((type == EntityType.PIG && mobId.equals(fallback) || mobTable != LootTable.EMPTY) && mobTable != null) {
                currentTable = mobTableId.toString();
                mobSenders.put(mobTableId, parseMobLootTable(mobTable, mobTableId, mobId, addedConditions));
            } else {
                if (EMILoot.isDebug()) EMILoot.LOGGER.warn("Found empty mob table at id: {}", mobTableId);
            }
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while parsing Mob Loot Table {}", mobTableId.toString());
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    private static ChestLootTableSender parseChestLootTable(LootTable lootTable, Identifier id) {
        ChestLootTableSender sender = new ChestLootTableSender(id);

        List<LootPool> pools = ((LootTableAccessor) lootTable).emi_loot$getPools();
		List<LootTable> directTables = ServerResourceData.getDirectTables(lootTable, id, EMILoot.Type.CHEST.doDirectDrops());
		if (!directTables.isEmpty()) {
            parsedDirectDrops.add(id);
            List<LootPool> allPools = new ArrayList<>(pools.size() + directTables.size());
            allPools.addAll(pools);

            for (LootTable directTable : directTables) {
                if (directTable != null) {
                    allPools.addAll(((LootTableAccessor) directTable).emi_loot$getPools());
                }
            }

            pools = allPools;
        }

        for (LootPool pool : pools) {
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float conditionalMultiplier = 1f;
            for (LootCondition condition : ((LootPoolAccessor) pool).emi_loot$getConditions()) {
                if (condition instanceof RandomChanceLootCondition randomChanceLootCondition) {
                    conditionalMultiplier *= NumberProcessors.getRollAvg(randomChanceLootCondition.chance());
                }
            }
            float rollAvg = NumberProcessors.getRollAvg(rollProvider) * conditionalMultiplier;
            SimpleLootPoolBuilder builder = new SimpleLootPoolBuilder(rollAvg);
			List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                    parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
        return sender;
    }

    private static BlockLootTableSender parseBlockLootTable(LootTable lootTable, Identifier id) {
        BlockLootTableSender sender = new BlockLootTableSender(id);
        parseBlockLootTableInternal(lootTable, sender, false);
        List<LootTable> directTables = ServerResourceData.getDirectTables(lootTable, id, EMILoot.Type.BLOCK.doDirectDrops());
        if (!directTables.isEmpty()) {
            parsedDirectDrops.add(id);
            parseBlockDirectLootTable(directTables, sender);
        }
        return sender;
    }

    private static void parseBlockDirectLootTable(Collection<LootTable> tables, BlockLootTableSender sender) {
        for (LootTable directTable : tables) {
            if (directTable != null) {
                parseBlockLootTableInternal(directTable, sender, true);
            }
        }
    }

    private static void parseBlockLootTableInternal(LootTable lootTable, BlockLootTableSender sender, boolean isDirect) {
        for (LootPool pool : ((LootTableAccessor) lootTable).emi_loot$getPools()) {
			List<LootCondition> conditions = ((LootPoolAccessor) pool).emi_loot$getConditions();
            List<LootConditionResult> parsedConditions = parseLootConditions(conditions, ItemStack.EMPTY, false);
            if (isDirect) {
                if (EMILoot.isDebug()) EMILoot.LOGGER.info("Adding direct drop block condition to {}", currentTable);
                parsedConditions.add(new LootConditionResult(TextKey.of("emi_loot.condition.direct_drop")));
            }
            List<LootFunction> functions = ((LootPoolAccessor) pool).emi_loot$getFunctions();
            List<LootFunctionResult> parsedFunctions = new LinkedList<>();
            for (LootFunction function: functions) {
                LootFunctionResult r = parseLootFunction(function);
                if (!r.skip())
                    parsedFunctions.add(r);
            }
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float rollAvg = NumberProcessors.getRollAvg(rollProvider);
            ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(rollAvg, parsedConditions, parsedFunctions);
			List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
    }

    private static MobLootTableSender parseMobLootTable(LootTable lootTable, Identifier id, Identifier mobId) {
        return parseMobLootTable(lootTable, id, mobId, emptyConditions);
    }

    private static MobLootTableSender parseMobLootTable(LootTable lootTable, Identifier id, Identifier mobId, List<LootCondition> addedConditions) {
        MobLootTableSender sender = new MobLootTableSender(id, mobId);
        parseMobLootTableInternal(lootTable, sender, false, addedConditions);
		List<LootTable> directTables = ServerResourceData.getDirectTables(lootTable, id, EMILoot.Type.MOB.doDirectDrops());
        if (!directTables.isEmpty()) {
            parsedDirectDrops.add(id);
            parseMobDirectLootTable(directTables, sender);
        }
        return sender;
    }

    private static void parseMobDirectLootTable(Collection<LootTable> tables, MobLootTableSender sender) {
        for (LootTable directTable : tables) {
            if (directTable != null) {
                parseMobLootTableInternal(directTable, sender, true, emptyConditions);
            }
        }
    }

    @SuppressWarnings("unused")
	private static void parseMobLootTableInternal(LootTable lootTable, MobLootTableSender sender, boolean isDirect, List<LootCondition> addedConditions) {
        for (LootPool pool : ((LootTableAccessor) lootTable).emi_loot$getPools()) {
            List<LootCondition> conditions = ((LootPoolAccessor) pool).emi_loot$getConditions();
            List<LootConditionResult> parsedConditions = parseLootConditions(conditions, ItemStack.EMPTY, false);
            if (isDirect) {
                if (EMILoot.isDebug()) EMILoot.LOGGER.info("Adding direct drop mob condition to {}", currentTable);
                parsedConditions.add(new LootConditionResult(TextKey.of("emi_loot.condition.direct_drop")));
            }
            List<LootFunction> functions = ((LootPoolAccessor) pool).emi_loot$getFunctions();
            List<LootFunctionResult> parsedFunctions = new LinkedList<>();
            for (LootFunction function: functions) {
                LootFunctionResult r = parseLootFunction(function);
                if (!r.skip())
                    parsedFunctions.add(r);
            }
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float rollAvg = NumberProcessors.getRollAvg(rollProvider);
            ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(rollAvg, parsedConditions, parsedFunctions);
			List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
    }

    private static GameplayLootTableSender parseGameplayLootTable(LootTable lootTable, Identifier id) {
        GameplayLootTableSender sender = new GameplayLootTableSender(id);
        parseGameplayLootTableInternal(lootTable, sender, false);
        List<LootTable> directTables = ServerResourceData.getDirectTables(lootTable, id, EMILoot.Type.GAMEPLAY.doDirectDrops());
        if (!directTables.isEmpty()) {
            parsedDirectDrops.add(id);
            parseGameplayDirectLootTable(directTables, sender);
        }
        return sender;
    }

    private static void parseGameplayDirectLootTable(Collection<LootTable> tables, GameplayLootTableSender sender) {
        for (LootTable directTable : tables) {
            if (directTable != null) {
                parseGameplayLootTableInternal(directTable, sender, true);
            }
        }
    }

    private static void parseGameplayLootTableInternal(LootTable lootTable, GameplayLootTableSender sender, boolean isDirect) {
        for (LootPool pool : ((LootTableAccessor) lootTable).emi_loot$getPools()) {
			List<LootCondition> conditions = ((LootPoolAccessor) pool).emi_loot$getConditions();
            List<LootConditionResult> parsedConditions = parseLootConditions(conditions, ItemStack.EMPTY, false);
            if (isDirect) {
                if (EMILoot.isDebug()) EMILoot.LOGGER.info("Adding direct drop gameplay condition to {}", currentTable);
                parsedConditions.add(new LootConditionResult(TextKey.of("emi_loot.condition.direct_drop")));
            }
			List<LootFunction> functions = ((LootPoolAccessor) pool).emi_loot$getFunctions();
            List<LootFunctionResult> parsedFunctions = new LinkedList<>();
            for (LootFunction function: functions) {
                LootFunctionResult r = parseLootFunction(function);
                if (!r.skip())
                    parsedFunctions.add(r);
            }
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float rollAvg = NumberProcessors.getRollAvg(rollProvider);
            ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(rollAvg, parsedConditions, parsedFunctions);
			List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
    }


    private static ArchaeologyLootTableSender parseArchaeologyTable(LootTable lootTable, Identifier id) {
        ArchaeologyLootTableSender sender = new ArchaeologyLootTableSender(id);

        List<LootPool> pools = ((LootTableAccessor) lootTable).emi_loot$getPools();
        List<LootTable> directTables = ServerResourceData.getDirectTables(lootTable, id, EMILoot.Type.ARCHAEOLOGY.doDirectDrops());

        if (!directTables.isEmpty()) {
            parsedDirectDrops.add(id);
            List<LootPool> allPools = new ArrayList<>(pools.size() + directTables.size());
            allPools.addAll(pools);

            for (LootTable directTable : directTables) {
                if (directTable != null) {
                    allPools.addAll(((LootTableAccessor) directTable).emi_loot$getPools());
                }
            }

            pools = allPools;
        }

        for (LootPool pool : pools) {
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float conditionalMultiplier = 1f;
            for (LootCondition condition : ((LootPoolAccessor) pool).emi_loot$getConditions()) {
                if (condition instanceof RandomChanceLootCondition) {
                    conditionalMultiplier *= NumberProcessors.getRollAvg(((RandomChanceLootCondition) condition).chance());
                }
            }
            float rollAvg = NumberProcessors.getRollAvg(rollProvider) * conditionalMultiplier;
            SimpleLootPoolBuilder builder = new SimpleLootPoolBuilder(rollAvg);
			List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
        return sender;
    }

    private static void parseGenericComplexLootTableInternal(LootTable lootTable, LootSender<ComplexLootPoolBuilder> sender) {
        for (LootPool pool : ((LootTableAccessor) lootTable).emi_loot$getPools()) {
            List<LootCondition> conditions = ((LootPoolAccessor) pool).emi_loot$getConditions();
            List<LootConditionResult> parsedConditions = parseLootConditions(conditions, ItemStack.EMPTY, false);
            List<LootFunction> functions = ((LootPoolAccessor) pool).emi_loot$getFunctions();
            List<LootFunctionResult> parsedFunctions = new LinkedList<>();
            for (LootFunction function: functions) {
                LootFunctionResult r = parseLootFunction(function);
                if (!r.skip())
                    parsedFunctions.add(r);
            }
            LootNumberProvider rollProvider = ((LootPoolAccessor) pool).emi_loot$getRolls();
            float rollAvg = NumberProcessors.getRollAvg(rollProvider);
            ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(rollAvg, parsedConditions, parsedFunctions);
            List<LootPoolEntry> entries = ((LootPoolAccessor) pool).emi_loot$getEntries();
            for (LootPoolEntry entry : entries) {
                parseLootPoolEntry(builder, entry);
            }
            sender.addBuilder(builder);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static void parseLootPoolEntry(LootBuilder builder, LootPoolEntry entry) {
        parseLootPoolEntry(builder, entry, null);
    }

    static void parseLootPoolEntry(LootBuilder builder, LootPoolEntry entry, @Nullable PostProcessor process) {
        if(entry instanceof TagEntry tagEntry) {
            if (process == PostProcessor.TAG) {
                List<ItemEntryResult> result = parseTagEntry(tagEntry, false);
                result.forEach(builder::addItem);
            } else {
                builder.addEntryForPostProcessing(PostProcessor.TAG, tagEntry);
            }
        } else {
            List<ItemEntryResult> result = parseLootPoolEntry(entry, false);
            result.forEach(builder::addItem);
        }
    }

    static List<ItemEntryResult> parseLootPoolEntry(LootPoolEntry entry, boolean parentIsAlternative) {
        if (entry instanceof ItemEntry itemEntry) {
            return parseItemEntry(itemEntry, parentIsAlternative);
        } else if(entry instanceof AlternativeEntry alternativeEntry) {
            return parseAlternativeEntry(alternativeEntry);
        } else if(entry instanceof GroupEntry groupEntry) {
            return parseGroupEntry(groupEntry, parentIsAlternative);
        } else if(entry instanceof SequenceEntry sequenceEntry) {
            return parseSequenceEntry(sequenceEntry, parentIsAlternative);
        } else if(entry instanceof TagEntry tagEntry) {
            return  parseTagEntry(tagEntry, parentIsAlternative);
        } else if (entry instanceof LootTableEntry lootTableEntry) {
            return parseLootTableEntry(lootTableEntry, parentIsAlternative);
        }
        return List.of();
    }

    static List<ItemEntryResult> parseItemEntry(ItemEntry entry, boolean parentIsAlternative) {
        int weight = ((LeafEntryAccessor) entry).emi_loot$getWeight();
        ItemStack item = new ItemStack(((ItemEntryAccessor) entry).emi_loot$getItem());
        List<LootFunction> functions = ((LeafEntryAccessor) entry).emi_loot$getFunctions();
        List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
        return parseItemEntry(weight, item, functions, conditions, parentIsAlternative);
    }

    static List<ItemEntryResult> parseItemEntry(int weight, ItemStack item, List<LootFunction> functions, List<LootCondition> conditions, boolean parentIsAlternative) {
        FunctionApplierResult functionApplierResult = applyLootFunctionToItem(functions, item, weight, parentIsAlternative);
        List<ItemEntryResult> conditionalEntryResults = functionApplierResult.conditionalResults;
        List<TextKey> functionTexts = functionApplierResult.functionTexts;
        item = functionApplierResult.stack;
        List<TextKey> conditionsTexts = parseLootConditionTexts(conditions, item, parentIsAlternative);
        List<ItemEntryResult> returnList = new LinkedList<>();
        returnList.add(new ItemEntryResult(item, weight, conditionsTexts, functionTexts));
        conditionalEntryResults.forEach(conditionalEntry-> {
            conditionalEntry.conditions.addAll(conditionsTexts);
            conditionalEntry.functions.addAll(functionTexts);
            returnList.add(conditionalEntry);
        });

        return returnList;
    }

    static List<ItemEntryResult> parseTagEntry(TagEntry entry, boolean parentIsAlternative) {
        TagKey<Item> items = ((TagEntryAccessor) entry).emi_loot$getName();
        if (EMILoot.isDebug()) EMILoot.LOGGER.info(">>> Parsing tag entry {}", items.id());
        Iterable<RegistryEntry<Item>> itemsItr = Registries.ITEM.iterateEntries(items);
        List<ItemEntryResult> returnList = new LinkedList<>();
        //if (EMILoot.isDebug()) EMILoot.LOGGER.info(itemsItr.toString());
        int weight = ((LeafEntryAccessor) entry).emi_loot$getWeight();
        List<LootFunction> functions = ((LeafEntryAccessor) entry).emi_loot$getFunctions();
        List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
        for (RegistryEntry<Item> item : itemsItr) {
            ItemStack stack = new ItemStack(item.value());
            //if (EMILoot.isDebug()) EMILoot.LOGGER.info("> Stack: " + stack.getName());
            returnList.addAll(parseItemEntry(weight, stack, functions, conditions, parentIsAlternative));
        }
        return returnList;

    }

    static List<ItemEntryResult> parseAlternativeEntry(AlternativeEntry entry) {
        List<LootPoolEntry> children = ((CombinedEntryAccessor)entry).emi_loot$getChildren();
        List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
        List<TextKey> conditionsTexts = parseLootConditionTexts(conditions, ItemStack.EMPTY, true);
        List<ItemEntryResult> results = new LinkedList<>();
        children.forEach((lootEntry)-> {
            List<ItemEntryResult> result = parseLootPoolEntry(lootEntry, true);
            result.forEach(resultEntry -> {
                resultEntry.conditions.addAll(conditionsTexts);
                results.add(resultEntry);
            });
        });
        return results;
    }

    static List<ItemEntryResult> parseGroupEntry(GroupEntry entry, boolean parentIsAlternative) {
        List<LootPoolEntry> children = ((CombinedEntryAccessor)entry).emi_loot$getChildren();
        List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
        List<TextKey> conditionsTexts = parseLootConditionTexts(conditions, ItemStack.EMPTY, parentIsAlternative);
        List<ItemEntryResult> results = new LinkedList<>();
        children.forEach((lootEntry)-> {
            List<ItemEntryResult> result = parseLootPoolEntry(lootEntry, parentIsAlternative);
            result.forEach(resultEntry -> {
                resultEntry.conditions.addAll(conditionsTexts);
                results.add(resultEntry);
            });
        });
        return results;
    }

    static List<ItemEntryResult> parseSequenceEntry(SequenceEntry entry, boolean parentIsAlternative) {
        List<LootPoolEntry> children = ((CombinedEntryAccessor)entry).emi_loot$getChildren();
        List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
        List<TextKey> conditionsTexts = parseLootConditionTexts(conditions, ItemStack.EMPTY, parentIsAlternative);
        List<ItemEntryResult> results = new LinkedList<>();
        TextKey sequenceCondition = TextKey.of("emi_loot.condition.sequence");
        children.forEach((lootEntry)-> {
            List<ItemEntryResult> result = parseLootPoolEntry(lootEntry, parentIsAlternative);
            result.forEach(resultEntry -> {
                resultEntry.conditions.addAll(conditionsTexts);
                resultEntry.conditions.add(sequenceCondition);
                results.add(resultEntry);
            });
        });
        return results;
    }

    static List<ItemEntryResult> parseLootTableEntry(LootTableEntry entry, boolean parentIsAlternative) {
		Either<RegistryKey<LootTable>, LootTable> value = ((LootTableEntryAccessor)entry).emi_loot$getValue();
		LootTable table = value.map(LootTableParser.lootManager::get, Function.identity());
		if (table == null) return List.of();
		Identifier id = LootTableParser.lootManager.getId(table);
		if (id == null) return List.of();
		List<LootCondition> conditions = ((LootPoolEntryAccessor) entry).emi_loot$getConditions();
		List<TextKey> conditionsTexts = parseLootConditionTexts(conditions, ItemStack.EMPTY, parentIsAlternative);
		LootSender<ComplexLootPoolBuilder> results = new NestedLootTableResults();
		parseGenericComplexLootTableInternal(table, results);
		List<ItemEntryResult> parsedList = new ArrayList<>();
		results.getBuilders().forEach(parsedBuilder ->
				parsedList.addAll(parsedBuilder.revert())
		);
		parsedList.forEach(result -> result.conditions.addAll(conditionsTexts));
		List<LootFunction> functions = ((LeafEntryAccessor) entry).emi_loot$getFunctions();
		return applyLootFunctionsToTableResults(functions, parsedList, parentIsAlternative);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    static LootFunctionResult parseLootFunction(LootFunction function) {
        return parseLootFunction(function, ItemStack.EMPTY, false);
    }

    static LootFunctionResult parseLootFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative) {
        LootFunctionType<?> type;
        try {
            type = function.getType();
        } catch (Exception e) {
			EMILoot.LOGGER.error("Failed to determine a function type for stack {} in table {}", stack.getName(), currentTable);
            EMILoot.LOGGER.error("Thrown Error:", e);
            return LootFunctionResult.EMPTY;
        }
        List<TextKey> conditionsTexts;
        if (function instanceof ConditionalLootFunction) {
            List<LootCondition> conditions = ((ConditionalLootFunctionAccessor)function).emi_loot$getConditions();
            conditionsTexts = parseLootConditionTexts(conditions, stack, parentIsAlternative);
        } else {
            conditionsTexts = new LinkedList<>();
        }
        try {
            return LootParserRegistry.parseFunction(function, stack, type, parentIsAlternative, conditionsTexts);
        } catch(Exception e) {
			EMILoot.LOGGER.error("Failed to parse LootFunction of type {} for stack {} in table {}", type, stack.getName(), currentTable);
            EMILoot.LOGGER.error("Thrown Error:", e);
            return LootFunctionResult.EMPTY;
        }
    }

    private static List<ItemEntryResult> applyLootFunctionsToTableResults(List<LootFunction> functions, List<ItemEntryResult> parsedList, boolean parentIsAlternative) {
        List<ItemEntryResult> conditionalEntryResults = new LinkedList<>();
        List<ItemEntryResult> processedEntryResults = new LinkedList<>();
        parsedList.forEach(itemEntry -> {
            FunctionApplierResult result = applyLootFunctionToItem(functions, itemEntry.item, itemEntry.weight, parentIsAlternative);
            List<TextKey> conditionTexts = itemEntry.conditions;
            List<TextKey> functionTexts = itemEntry.functions;
            functionTexts.addAll(result.functionTexts);
            processedEntryResults.add(new ItemEntryResult(result.stack, itemEntry.weight, conditionTexts, functionTexts));
            conditionalEntryResults.addAll(result.conditionalResults);
        });
        processedEntryResults.addAll(conditionalEntryResults);
        return processedEntryResults;
    }

    private static FunctionApplierResult applyLootFunctionToItem(List<LootFunction> functions, ItemStack item, int weight, boolean parentIsAlternative) {
        List<TextKey> functionTexts = new LinkedList<>();
        List<ItemEntryResult> conditionalEntryResults = new LinkedList<>();
        for (LootFunction lootFunction : functions) {
            LootFunctionResult result = parseLootFunction(lootFunction, item, parentIsAlternative);
            TextKey lootText = result.text;
            ItemStack newStack = result.stack;
            List<TextKey> resultConditions = result.conditions;

            if (!resultConditions.isEmpty()) {
                ItemStack conditionalItem;
                if (newStack != ItemStack.EMPTY) {
                    conditionalItem = newStack;
                } else {
                    conditionalItem = item;
                }
                List<TextKey> conditionalFunctionTexts = new ArrayList<>();
                conditionalFunctionTexts.add(lootText);
                conditionalEntryResults.add(new ItemEntryResult(conditionalItem, weight, resultConditions, conditionalFunctionTexts));
            } else {
                if (lootText.isNotEmpty() && !lootText.skip()) {
                    functionTexts.add(lootText);
                }
                if (newStack != ItemStack.EMPTY) {
                    item = newStack;
                }
            }
        }
        return new FunctionApplierResult(conditionalEntryResults, functionTexts, item);
    }

    ///////////////////////////////////////////////////////////////

    public static List<TextKey> parseLootConditionTexts(List<LootCondition> conditions, ItemStack item, boolean parentIsAlternative) {
        List<TextKey> conditionsTexts = new LinkedList<>();
        List<LootConditionResult> parsedConditions = parseLootConditions(conditions, item, parentIsAlternative);
        for (LootConditionResult result: parsedConditions) {
                    conditionsTexts.add(result.text);
        }
        return conditionsTexts;
    }

    public static List<LootConditionResult> parseLootConditions(List<LootCondition> conditions, ItemStack item, boolean parentIsAlternative) {
        List<LootConditionResult> parsedConditions = new LinkedList<>();
        for (LootCondition condition: conditions) {
            List<LootConditionResult> results = parseLootCondition(condition, item, parentIsAlternative);
            for (LootConditionResult result: results) {
                if (result.text.isNotEmpty()) {
                    parsedConditions.add(result);
                }
            }
        }
        return parsedConditions;
    }

    public static List<LootConditionResult> parseLootCondition(LootCondition condition, ItemStack stack) {
        return parseLootCondition(condition, stack, false);
    }

    public static List<LootConditionResult> parseLootCondition(LootCondition condition, ItemStack stack, boolean parentIsAlternative) {
        LootConditionType type;
        try {
            type = condition.getType();
        } catch (Exception e) {
			EMILoot.LOGGER.error("failed to determine a condition type for stack {} in table {}", stack.getName(), currentTable);
            return Collections.singletonList(LootConditionResult.EMPTY);
        }
        try {
            return LootParserRegistry.parseCondition(condition, type, stack, parentIsAlternative);
        } catch (Exception e) {
			EMILoot.LOGGER.error("Failed to parse LootCondition of type {} for stack {} in table {}", condition.getType(), stack.getName(), currentTable);
            EMILoot.LOGGER.error("Thrown Error:", e);
            return Collections.singletonList(LootConditionResult.EMPTY);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Text compileConditionTexts(ItemStack stack, List<LootConditionResult> results) {
        MutableText finalText = LText.empty();
        int size = results.size();
        for(int i = 0; i < size;i++) {
            LootConditionResult result = results.get(i);
            Text resultText = result.text.process(stack, null).text();
            if (i == 0) {
                finalText = resultText.copy();
            } else {
                finalText.append(resultText);
            }
            if (i<(size - 1)) {
                finalText.append(LText.translatable("emi_loot.and"));
            }
        }
        return finalText;
    }

    public enum PostProcessor {
        TAG
    }

    public record FunctionApplierResult(List<ItemEntryResult> conditionalResults, List<TextKey> functionTexts, ItemStack stack){}

    public record LootFunctionResult(
            TextKey text,
            ItemStack stack,
            List<TextKey> conditions
    ) {
        public static LootFunctionResult EMPTY = new LootFunctionResult(TextKey.empty(), net.minecraft.item.ItemStack.EMPTY, new LinkedList<>());

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
		public boolean skip() {
            return conditions.isEmpty() && text.skip();
        }
    }

    public record LootConditionResult(
            TextKey text
    ) {
        public static LootConditionResult EMPTY = new LootConditionResult(TextKey.empty());

        public TextKey getText() {
            return text;
        }
    }

    public record ItemEntryResult(
            ItemStack item,
            int weight,
            List<TextKey> conditions,
            List<TextKey> functions
    ){}
}