package dev.emi.remastered.loot.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Name based fallback that links a chest loot table to the structures it belongs to, used when the structure
 * pieces do not reveal their loot tables (hard coded structures such as strongholds or shipwrecks, and tables that no
 * piece references). Works on plain strings ("namespace:path") so it does not need Minecraft.
 */
public final class StructureMatcher {

	/** Vanilla loot table name families that do not share the name of their structure. Prefix to structure name. */
	private static final Map<String, String> FAMILIES = Map.ofEntries(
		Map.entry("abandoned_mineshaft", "mineshaft"),
		Map.entry("jungle_temple", "jungle_pyramid"),
		Map.entry("woodland_mansion", "mansion"),
		Map.entry("nether_bridge", "fortress"),
		Map.entry("underwater_ruin", "ocean_ruin"),
		Map.entry("igloo_chest", "igloo"),
		Map.entry("bastion", "bastion_remnant"),
		Map.entry("shipwreck", "shipwreck"),
		Map.entry("stronghold", "stronghold"),
		Map.entry("ancient_city", "ancient_city"),
		Map.entry("end_city", "end_city"),
		Map.entry("village", "village"),
		Map.entry("ruined_portal", "ruined_portal"),
		Map.entry("trial_chambers", "trial_chambers"),
		Map.entry("pillager_outpost", "pillager_outpost"),
		Map.entry("desert_pyramid", "desert_pyramid"),
		Map.entry("buried_treasure", "buried_treasure")
	);

	private StructureMatcher() {
	}

	/**
	 * Structure ids ("namespace:path") that the chest loot table most likely belongs to, sorted. Empty when the table
	 * is not a chest table or nothing matches (e.g. {@code minecraft:chests/simple_dungeon}).
	 */
	public static List<String> match(String tableId, Collection<String> structureIds) {
		int colon = tableId.indexOf(':');
		String tableNamespace = colon < 0 ? "minecraft" : tableId.substring(0, colon);
		String tablePath = colon < 0 ? tableId : tableId.substring(colon + 1);
		String key = key(tablePath);
		if (key == null) {
			return List.of();
		}
		boolean vanilla = tableNamespace.equals("minecraft");
		if (vanilla) {
			key = family(key);
		}
		List<String> result = new ArrayList<>();
		for (String structureId : structureIds) {
			int sColon = structureId.indexOf(':');
			String structureNamespace = sColon < 0 ? "minecraft" : structureId.substring(0, sColon);
			if (!structureNamespace.equals(tableNamespace)) {
				continue;
			}
			String structurePath = sColon < 0 ? structureId : structureId.substring(sColon + 1);
			if (matches(key, structurePath)) {
				result.add(structureId);
			}
		}
		result.sort(null);
		return result;
	}

	/** First path segment below "chests/" (or "chest/"), or null if this is not a chest table path. */
	static String key(String path) {
		int slash = path.indexOf('/');
		if (slash < 0) {
			return null;
		}
		String root = path.substring(0, slash);
		if (!root.equals("chests") && !root.equals("chest")) {
			return null;
		}
		String rest = path.substring(slash + 1);
		int next = rest.indexOf('/');
		String first = next < 0 ? rest : rest.substring(0, next);
		return first.isEmpty() ? null : first;
	}

	static String family(String key) {
		for (Map.Entry<String, String> entry : FAMILIES.entrySet()) {
			String prefix = entry.getKey();
			if (key.equals(prefix) || key.startsWith(prefix + "_")) {
				return entry.getValue();
			}
		}
		return key;
	}

	private static boolean matches(String key, String structurePath) {
		return structurePath.equals(key) || structurePath.startsWith(key + "_") || key.startsWith(structurePath + "_");
	}
}
