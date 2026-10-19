package dev.emi.remastered.loot.util;

import dev.emi.remastered.loot.structure.StructureNames;
import dev.emi.remastered.loot.structure.StructureRef;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the "Found in" line of chest loot pages. Client only.
 */
public final class StructureText {

	private StructureText() {
	}

	public static String structureName(Identifier id) {
		String key = "emi_loot.structure." + id;
		return I18n.hasTranslation(key) ? I18n.translate(key) : StructureNames.prettify(id.getPath());
	}

	public static String dimensionName(Identifier id) {
		String key = "emi_loot.dimension." + id;
		return I18n.hasTranslation(key) ? I18n.translate(key) : StructureNames.prettify(id.getPath());
	}

	/** "Found in: Village (Overworld), Bastion Remnant (Nether)", or null when there is nothing to say. */
	public static Text foundIn(List<StructureRef> refs) {
		if (refs.isEmpty()) {
			return null;
		}
		List<Map.Entry<String, List<String>>> entries = new ArrayList<>();
		for (StructureRef ref : refs) {
			entries.add(Map.entry(structureName(ref.structure()), ref.dimensions().stream().map(StructureText::dimensionName).toList()));
		}
		StringBuilder list = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : StructureNames.merge(entries).entrySet()) {
			if (!list.isEmpty()) {
				list.append(", ");
			}
			list.append(entry.getKey());
			if (!entry.getValue().isEmpty()) {
				list.append(" (").append(String.join(", ", entry.getValue())).append(")");
			}
		}
		return LText.translatable("emi_loot.found_in", list.toString());
	}
}
