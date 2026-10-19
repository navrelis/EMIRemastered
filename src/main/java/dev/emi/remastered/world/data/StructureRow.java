package dev.emi.remastered.world.data;

import java.util.List;

/**
 * One structure as the server sends it. Plain strings and ints so that the logic around it is testable without
 * Minecraft.
 *
 * @param id          structure id
 * @param step        generation step name ({@code surface_structures}, ...)
 * @param dimensions  dimensions whose biome source can produce one of the valid biomes
 * @param biomes      valid biomes (tags already expanded), sorted
 * @param biomeTag    the tag the valid biomes come from, empty when the structure lists its biomes directly
 * @param set         id of the structure set the structure is in, empty when it is in none
 * @param weight      weight of the structure inside its set
 * @param totalWeight sum of the weights of the set
 * @param placement   where the set places its structures
 * @param lootTables  chest loot tables found in the structure (from the loot module's structure lookup), sorted
 */
public record StructureRow(String id, String step, List<String> dimensions, List<String> biomes, String biomeTag, String set,
		int weight, int totalWeight, Placement placement, List<String> lootTables) {

	public StructureRow {
		dimensions = List.copyOf(dimensions);
		biomes = List.copyOf(biomes);
		lootTables = List.copyOf(lootTables);
	}

	/** The same structure with other chest loot tables. */
	public StructureRow withLootTables(List<String> tables) {
		return new StructureRow(id, step, dimensions, biomes, biomeTag, set, weight, totalWeight, placement, tables);
	}

	/** Where structures of a set generate. The meaning of {@code a}, {@code b} and {@code c} depends on the kind. */
	public record Placement(Kind kind, int a, int b, int c) {
		public static final Placement NONE = new Placement(Kind.NONE, 0, 0, 0);

		/** Random spread: spacing and separation in chunks, spread type 0 linear and 1 triangular. */
		public static Placement randomSpread(int spacing, int separation, int spreadType) {
			return new Placement(Kind.RANDOM_SPREAD, spacing, separation, spreadType);
		}

		/** Concentric rings: distance, spread and count. */
		public static Placement rings(int distance, int spread, int count) {
			return new Placement(Kind.CONCENTRIC_RINGS, distance, spread, count);
		}

		public int spacing() {
			return a;
		}

		public int separation() {
			return b;
		}

		public boolean triangular() {
			return c == 1;
		}

		public int ringDistance() {
			return a;
		}

		public int ringSpread() {
			return b;
		}

		public int ringCount() {
			return c;
		}
	}

	public enum Kind {
		/** The structure is in no structure set (placed by something else). */
		NONE,
		RANDOM_SPREAD,
		CONCENTRIC_RINGS,
		/** A placement type of another mod. */
		OTHER
	}
}
