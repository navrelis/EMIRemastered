/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.display;

import dev.emi.remastered.ores.sync.OresSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Identifier;

/**
 * The compact biome list of an ore page: per dimension either "all biomes", "all biomes except ..." or the biomes
 * themselves, and a way to cut long lists down to a few icons plus a count. No Minecraft client types.
 */
public final class BiomeSummary {
	private BiomeSummary() {
	}

	public enum Kind {
		/** The feature is in every biome of the dimension. */
		ALL,
		/** In all biomes of the dimension but the listed ones. */
		ALL_EXCEPT,
		/** In the listed biomes only. */
		LIST
	}

	/**
	 * @param dimension null for biomes that no known dimension can generate
	 * @param total     number of biomes of the dimension the feature is in (all of them for {@link Kind#ALL})
	 * @param listed    the excluded biomes for {@link Kind#ALL_EXCEPT}, the biomes of the feature for {@link Kind#LIST},
	 *                  empty for {@link Kind#ALL}; sorted
	 */
	public record Section(Identifier dimension, Kind kind, int total, List<Identifier> listed) {
	}

	/** A list cut down to what is shown and what is hidden behind "+N more". */
	public record Split(List<Identifier> shown, List<Identifier> hidden) {
	}

	/**
	 * @param iconLimit the number of icons shown per list; a dimension is only summed up as "all except" when the
	 *                  feature is in more biomes of it than fit into the icons
	 */
	public static List<Section> summarize(Collection<Identifier> featureBiomes, Collection<OresSnapshot.Dimension> dimensions, int iconLimit) {
		Set<Identifier> feature = new LinkedHashSet<>(featureBiomes);
		Set<Identifier> placed = new LinkedHashSet<>();
		List<Section> sections = new ArrayList<>();
		for (OresSnapshot.Dimension dimension : dimensions) {
			List<Identifier> present = new ArrayList<>();
			List<Identifier> missing = new ArrayList<>();
			for (Identifier biome : dimension.biomes()) {
				(feature.contains(biome) ? present : missing).add(biome);
			}
			if (present.isEmpty()) {
				continue;
			}
			placed.addAll(present);
			if (missing.isEmpty()) {
				sections.add(new Section(dimension.id(), Kind.ALL, present.size(), List.of()));
			} else if (present.size() > iconLimit && missing.size() < present.size()) {
				sections.add(new Section(dimension.id(), Kind.ALL_EXCEPT, present.size(), sorted(missing)));
			} else {
				sections.add(new Section(dimension.id(), Kind.LIST, present.size(), sorted(present)));
			}
		}
		List<Identifier> other = new ArrayList<>();
		for (Identifier biome : feature) {
			if (!placed.contains(biome)) {
				other.add(biome);
			}
		}
		if (!other.isEmpty()) {
			sections.add(new Section(null, Kind.LIST, other.size(), sorted(other)));
		}
		return sections;
	}

	/** Minecraft's own biomes first, then by namespace and path. */
	public static List<Identifier> sorted(Collection<Identifier> biomes) {
		List<Identifier> list = new ArrayList<>(biomes);
		list.sort(Comparator.comparing((Identifier id) -> !id.getNamespace().equals("minecraft"))
			.thenComparing(Identifier::getNamespace)
			.thenComparing(Identifier::getPath));
		return list;
	}

	/** Shows the first {@code iconLimit} entries, unless hiding two or fewer would not save any room. */
	public static Split split(List<Identifier> list, int iconLimit) {
		int limit = Math.max(1, iconLimit);
		if (list.size() <= limit + 2) {
			return new Split(list, List.of());
		}
		return new Split(list.subList(0, limit), list.subList(limit, list.size()));
	}

	/** Cells (icon wide) the "+N more" label takes. */
	public static final int MORE_CELLS = 3;

	/** Rows of icons, with the "+N more" label behind the last icon, for rows of {@code perRow} cells. */
	public static int rows(int icons, boolean more, int perRow) {
		if (icons <= 0 && !more) {
			return 0;
		}
		int cells = icons + (more ? MORE_CELLS : 0);
		return Math.max(1, (cells + perRow - 1) / perRow);
	}
}
