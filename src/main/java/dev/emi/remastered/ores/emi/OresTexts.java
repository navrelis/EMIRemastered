/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi;

import dev.emi.remastered.ores.display.DimensionNames;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Names of dimensions and biomes. Client only. */
public final class OresTexts {
	private OresTexts() {
	}

	/** {@code dimension.<namespace>.<path>} if a language file has it (mods may add it), else a readable form of the id. */
	public static Text dimensionName(Identifier id) {
		String key = id.toTranslationKey("dimension");
		return I18n.hasTranslation(key) ? Text.translatable(key) : Text.literal(DimensionNames.readable(id));
	}

	public static Text biomeName(Identifier id) {
		return Text.translatable(id.toTranslationKey("biome"));
	}

	public static Text dimensionNames(Collection<Identifier> ids) {
		MutableText result = Text.empty();
		boolean first = true;
		for (Identifier id : ids) {
			if (!first) {
				result.append(", ");
			}
			result.append(dimensionName(id));
			first = false;
		}
		return result;
	}

	public static List<String> sortedNames(Collection<Identifier> biomes) {
		return biomes.stream().map(id -> biomeName(id).getString()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
	}
}
