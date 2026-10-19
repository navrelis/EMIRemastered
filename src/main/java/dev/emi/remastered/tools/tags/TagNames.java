package dev.emi.remastered.tools.tags;

import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Display names of tags and tag members built from ids. Pure logic, no Minecraft classes: the caller looks the
 * translation keys up and falls back to {@link #readable}.
 */
public final class TagNames {
	/** Translation key prefix of the members of registries that have names: registry id to key prefix. */
	private static final Map<String, String> MEMBER_PREFIX = Map.of(
		"minecraft:worldgen/biome", "biome",
		"minecraft:enchantment", "enchantment",
		"minecraft:entity_type", "entity",
		"minecraft:mob_effect", "effect");

	private TagNames() {
	}

	/** {@code tag.<registry path>.<namespace>.<tag path>}, slashes become dots: {@code tag.worldgen.biome.minecraft.is_forest}. */
	public static String tagKey(String registry, String tagId) {
		return "tag." + dotted(path(registry)) + "." + namespace(tagId) + "." + dotted(path(tagId));
	}

	/** Translation key of a member (a biome is {@code biome.<namespace>.<path>}), or null when the registry has none. */
	public static @Nullable String memberKey(String registry, String memberId) {
		String prefix = MEMBER_PREFIX.get(registry);
		if (prefix == null) {
			return null;
		}
		return prefix + "." + namespace(memberId) + "." + dotted(path(memberId));
	}

	/** A name made of the id: the last path segment, underscores as spaces, every word capitalized. */
	public static String readable(String id) {
		String path = path(id);
		String last = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ').trim();
		StringBuilder out = new StringBuilder(last.length());
		boolean wordStart = true;
		for (int i = 0; i < last.length(); i++) {
			char c = last.charAt(i);
			out.append(wordStart ? Character.toUpperCase(c) : c);
			wordStart = c == ' ';
		}
		return out.length() == 0 ? id : out.toString();
	}

	private static String namespace(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? "minecraft" : id.substring(0, colon);
	}

	private static String path(String id) {
		return id.substring(id.indexOf(':') + 1);
	}

	private static String dotted(String path) {
		return path.replace('/', '.').toLowerCase(Locale.ROOT);
	}
}
