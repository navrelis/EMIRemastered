package dev.emi.remastered.world.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.emi.remastered.mobs.client.ClientSpawnData;
import dev.emi.remastered.mobs.data.SpawnRow;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.ores.client.OresFeatures;
import dev.emi.remastered.ores.sync.OresSnapshot;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * What the mobs and the ores module know per biome. Both read their data from the client; this class only inverts it
 * (mobs and ores by biome) and caches the result until the data changes. Every read is guarded by the module being on
 * and its data being there.
 */
public final class CrossData {
	private static final Object LOCK = new Object();
	private static Map<String, List<Mob>> mobs = Map.of();
	private static int mobsVersion = -1;
	private static OresSnapshot<PlacedFeature> oresSource;
	private static Map<String, List<Block>> ores = Map.of();

	private CrossData() {
	}

	/** A mob that spawns in a biome: its entity type and spawn entry. */
	public record Mob(EntityType<?> type, String group, int weight, int minGroup, int maxGroup) {
		/** The spawn egg item, null for entity types that have none. */
		public SpawnEggItem egg() {
			return SpawnEggItem.forEntity(type);
		}
	}

	/** Whether the mobs module is on and the server's spawn data arrived. */
	public static boolean hasMobData() {
		return Modules.MOBS.isEnabled() && ClientSpawnData.isComplete();
	}

	/** Whether the ores module is on and the server's ore sync arrived. */
	public static boolean hasOreData() {
		return Modules.ORES.isEnabled() && OresFeatures.snapshot().syncId() != OresSnapshot.EMPTY_ID;
	}

	/** Mobs that spawn naturally in the biome, ordered by spawn group, then by weight. Empty without mobs data. */
	public static List<Mob> mobsOf(String biome) {
		if (!hasMobData()) {
			return List.of();
		}
		synchronized (LOCK) {
			int version = ClientWorldData.version();
			if (mobsVersion != version) {
				mobs = buildMobs();
				mobsVersion = version;
			}
			return mobs.getOrDefault(biome, List.of());
		}
	}

	private static Map<String, List<Mob>> buildMobs() {
		Map<String, List<Mob>> result = new HashMap<>();
		for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
			List<SpawnRow> rows = ClientSpawnData.rowsOf(id.toString());
			if (rows.isEmpty()) {
				continue;
			}
			EntityType<?> type = Registries.ENTITY_TYPE.get(id);
			for (SpawnRow row : rows) {
				result.computeIfAbsent(row.biome(), k -> new ArrayList<>()).add(new Mob(type, row.group(), row.weight(), row.minGroup(), row.maxGroup()));
			}
		}
		Comparator<Mob> order = Comparator.comparingInt((Mob mob) -> groupOrder(mob.group()))
			.thenComparing(Comparator.comparingInt(Mob::weight).reversed())
			.thenComparing(mob -> Registries.ENTITY_TYPE.getId(mob.type()).toString());
		result.values().forEach(list -> list.sort(order));
		return result;
	}

	private static int groupOrder(String group) {
		return switch (group) {
			case "creature" -> 0;
			case "monster" -> 1;
			case "ambient" -> 2;
			case "water_creature" -> 3;
			case "water_ambient" -> 4;
			case "underground_water_creature" -> 5;
			case "axolotls" -> 6;
			default -> 7;
		};
	}

	/** Blocks of the ore and geode features that generate in the biome, in registry order, each once. Empty without ore data. */
	public static List<Block> oresOf(String biome) {
		if (!hasOreData()) {
			return List.of();
		}
		OresSnapshot<PlacedFeature> snapshot = OresFeatures.snapshot();
		synchronized (LOCK) {
			if (oresSource != snapshot) {
				ores = buildOres(snapshot);
				oresSource = snapshot;
			}
			return ores.getOrDefault(biome, List.of());
		}
	}

	private static Map<String, List<Block>> buildOres(OresSnapshot<PlacedFeature> snapshot) {
		Map<String, Set<Block>> sets = new HashMap<>();
		snapshot.features().forEach((id, placed) -> {
			List<Block> blocks = new ArrayList<>();
			try {
				Object config = placed.feature().value().config();
				if (config instanceof OreFeatureConfig ore) {
					for (OreFeatureConfig.Target target : ore.targets) {
						blocks.add(target.state.getBlock());
					}
				} else if (config instanceof GeodeFeatureConfig geode && !geode.layerConfig.innerBlocks.isEmpty()) {
					blocks.add(geode.layerConfig.innerBlocks.get(0).getBlock());
				}
			} catch (RuntimeException e) {
				return;
			}
			for (Identifier biome : snapshot.biomesOf(id)) {
				Set<Block> set = sets.computeIfAbsent(biome.toString(), k -> new HashSet<>());
				set.addAll(blocks);
			}
		});
		Comparator<Block> order = Comparator.comparing(block -> Registries.BLOCK.getId(block).toString());
		Map<String, List<Block>> result = new LinkedHashMap<>();
		sets.forEach((biome, set) -> {
			set.remove(Blocks.AIR);
			List<Block> list = new ArrayList<>(set);
			list.sort(order);
			result.put(biome, Collections.unmodifiableList(list));
		});
		return result;
	}
}
