package dev.emi.remastered.professions;

import java.util.List;
import java.util.Locale;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

/**
 * Everything a profession page shows.
 *
 * @param profession   The profession
 * @param id           Registry id of the profession
 * @param workstations The workstation blocks that give a villager this profession
 */
public record ProfessionEntry(VillagerProfession profession, Identifier id, List<EmiStack> workstations) {

	/**
	 * Name of the profession as vanilla shows it on a villager. Modded professions that ship no translation get a
	 * name derived from their id.
	 */
	public Text displayName() {
		String prefix = "entity.minecraft.villager.";
		String namespaced = prefix + id.getNamespace() + "." + id.getPath();
		if (I18n.hasTranslation(namespaced)) {
			return Text.translatable(namespaced);
		}
		String plain = prefix + id.getPath();
		if (I18n.hasTranslation(plain)) {
			return Text.translatable(plain);
		}
		StringBuilder name = new StringBuilder();
		for (String part : id.getPath().split("[_/]")) {
			if (part.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			name.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
		}
		return Text.literal(name.toString());
	}
}
