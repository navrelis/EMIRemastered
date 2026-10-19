package dev.emi.remastered.loot.structure;

import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.structure.StructureType;

import java.util.List;
import java.util.Map;

/**
 * Vanilla chests that are not part of any structure template: the structure pieces place them in code (or, for ruined
 * portals, the piece loads a template that is not reachable through a jigsaw start pool). Keyed by structure type so
 * every structure registered with that type is covered, including variants and structures of data packs.
 * Taken from the 1.21.1 structure generator classes ({@code net.minecraft.structure.*Generator}).
 */
final class CodePlacedLoot {
	static final Map<StructureType<?>, List<RegistryKey<LootTable>>> BY_TYPE = Map.ofEntries(
		Map.entry(StructureType.DESERT_PYRAMID, List.of(LootTables.DESERT_PYRAMID_CHEST)),
		Map.entry(StructureType.JUNGLE_TEMPLE, List.of(LootTables.JUNGLE_TEMPLE_CHEST, LootTables.JUNGLE_TEMPLE_DISPENSER_CHEST)),
		Map.entry(StructureType.IGLOO, List.of(LootTables.IGLOO_CHEST_CHEST)),
		Map.entry(StructureType.MINESHAFT, List.of(LootTables.ABANDONED_MINESHAFT_CHEST)),
		Map.entry(StructureType.FORTRESS, List.of(LootTables.NETHER_BRIDGE_CHEST)),
		Map.entry(StructureType.STRONGHOLD, List.of(LootTables.STRONGHOLD_CORRIDOR_CHEST, LootTables.STRONGHOLD_CROSSING_CHEST, LootTables.STRONGHOLD_LIBRARY_CHEST)),
		Map.entry(StructureType.END_CITY, List.of(LootTables.END_CITY_TREASURE_CHEST)),
		Map.entry(StructureType.BURIED_TREASURE, List.of(LootTables.BURIED_TREASURE_CHEST)),
		Map.entry(StructureType.SHIPWRECK, List.of(LootTables.SHIPWRECK_MAP_CHEST, LootTables.SHIPWRECK_SUPPLY_CHEST, LootTables.SHIPWRECK_TREASURE_CHEST)),
		Map.entry(StructureType.OCEAN_RUIN, List.of(LootTables.UNDERWATER_RUIN_SMALL_CHEST, LootTables.UNDERWATER_RUIN_BIG_CHEST)),
		Map.entry(StructureType.WOODLAND_MANSION, List.of(LootTables.WOODLAND_MANSION_CHEST)),
		Map.entry(StructureType.RUINED_PORTAL, List.of(LootTables.RUINED_PORTAL_CHEST))
	);

	private CodePlacedLoot() {
	}

	static List<RegistryKey<LootTable>> forType(StructureType<?> type) {
		return BY_TYPE.getOrDefault(type, List.of());
	}
}
