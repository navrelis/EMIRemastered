package dev.emi.remastered.tools.tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;

/**
 * All tags of the client world's registries, built once per tag load. Pure data, no Minecraft classes.
 */
public final class TagCatalog {
	/** Registries listed first in the registry filter, in this order; all others follow alphabetically. */
	private static final List<String> PRIORITY = List.of("minecraft:item", "minecraft:block", "minecraft:fluid",
		"minecraft:entity_type");

	private final List<TagEntry> entries;
	private final List<String> registries;
	private final Map<String, Integer> counts = new HashMap<>();
	private volatile Map<String, Map<String, List<TagEntry>>> reverse;

	public TagCatalog(List<TagEntry> tags) {
		List<TagEntry> sorted = new ArrayList<>(tags);
		sorted.sort(Comparator.comparingInt((TagEntry e) -> rank(e.registry())).thenComparing(TagEntry::registry)
			.thenComparing(TagEntry::id));
		this.entries = List.copyOf(sorted);
		TreeSet<String> names = new TreeSet<>(Comparator.comparingInt(TagCatalog::rank).thenComparing(s -> s));
		for (TagEntry entry : entries) {
			names.add(entry.registry());
			counts.merge(entry.registry(), 1, Integer::sum);
		}
		this.registries = List.copyOf(names);
	}

	private static int rank(String registry) {
		int index = PRIORITY.indexOf(registry);
		return index < 0 ? PRIORITY.size() : index;
	}

	/** Every tag, sorted by registry (items, blocks, fluids, entity types, then the others) and id. */
	public List<TagEntry> entries() {
		return entries;
	}

	/** Registries that have at least one tag. */
	public List<String> registries() {
		return registries;
	}

	public int count(@Nullable String registry) {
		return registry == null ? entries.size() : counts.getOrDefault(registry, 0);
	}

	public @Nullable TagEntry find(@Nullable String registry, String id) {
		for (TagEntry entry : entries) {
			if (entry.id().equals(id) && (registry == null || entry.registry().equals(registry))) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * Reverse lookup: the tags that contain a member.
	 *
	 * @param registry registry of the tags, or null for all registries
	 */
	public List<TagEntry> tagsContaining(@Nullable String registry, String memberId) {
		Map<String, Map<String, List<TagEntry>>> index = reverse;
		if (index == null) {
			index = new HashMap<>();
			for (TagEntry entry : entries) {
				Map<String, List<TagEntry>> byMember = index.computeIfAbsent(entry.registry(), k -> new HashMap<>());
				for (String member : entry.members()) {
					byMember.computeIfAbsent(member, k -> new ArrayList<>()).add(entry);
				}
			}
			reverse = index;
		}
		if (registry != null) {
			return index.getOrDefault(registry, Map.of()).getOrDefault(memberId, List.of());
		}
		List<TagEntry> result = new ArrayList<>();
		for (String name : registries) {
			result.addAll(index.getOrDefault(name, Map.of()).getOrDefault(memberId, List.of()));
		}
		return result;
	}
}
