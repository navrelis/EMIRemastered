package dev.emi.remastered.world.emi;

import java.util.Collection;

import dev.emi.remastered.world.data.WorldFormat;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Names of dimensions, structures, biomes and generation steps. Client only. */
public final class WorldTexts {
	private WorldTexts() {
	}

	/** The module's own translation, then the one of the dimension id (mods may add {@code dimension.<ns>.<path>}), else a readable form of the id. */
	public static Text dimensionName(String id) {
		Identifier ident = Identifier.tryParse(id);
		if (ident == null) {
			return Text.literal(id);
		}
		String own = "emi_world.dimension." + ident.getNamespace() + "." + ident.getPath();
		if (I18n.hasTranslation(own)) {
			return Text.translatable(own);
		}
		String key = ident.toTranslationKey("dimension");
		return I18n.hasTranslation(key) ? Text.translatable(key) : Text.literal(WorldFormat.prettify(ident.getPath()));
	}

	public static Text dimensionNames(Collection<String> ids) {
		MutableText result = Text.empty();
		boolean first = true;
		for (String id : ids) {
			if (!first) {
				result.append(", ");
			}
			result.append(dimensionName(id));
			first = false;
		}
		return result;
	}

	/** The module's own translation, then the loot module's structure name, else a readable form of the id. */
	public static Text structureName(String id) {
		Identifier ident = Identifier.tryParse(id);
		if (ident == null) {
			return Text.literal(id);
		}
		String own = "emi_world.structure." + ident.getNamespace() + "." + ident.getPath();
		if (I18n.hasTranslation(own)) {
			return Text.translatable(own);
		}
		String loot = "emi_loot.structure." + ident;
		if (I18n.hasTranslation(loot)) {
			return Text.translatable(loot);
		}
		return Text.literal(WorldFormat.prettify(ident.getPath()));
	}

	public static Text biomeName(String id) {
		Identifier ident = Identifier.tryParse(id);
		return ident == null ? Text.literal(id) : Text.translatable(ident.toTranslationKey("biome"));
	}

	public static Text stepName(String step) {
		String key = "emi_world.step." + step;
		return I18n.hasTranslation(key) ? Text.translatable(key) : Text.literal(WorldFormat.prettify(step));
	}

	/**
	 * Readable name of a chest loot table: {@code minecraft:chests/village/village_weaponsmith} in a village becomes
	 * {@code Weaponsmith} (the structure's own name at the start of the table's name is left out).
	 */
	public static Text lootTableName(String table, String structureId) {
		Identifier ident = Identifier.tryParse(table);
		Identifier structure = Identifier.tryParse(structureId);
		if (ident == null) {
			return Text.literal(table);
		}
		String path = ident.getPath();
		String name = path.substring(path.lastIndexOf('/') + 1);
		if (structure != null) {
			String stripped = WorldFormat.withoutPrefix(name, structure.getPath());
			name = WorldFormat.withoutPrefix(stripped, structure.getPath().split("_")[0]);
		}
		String readable = WorldFormat.prettify(name);
		return Text.literal(ident.getNamespace().equals("minecraft") ? readable : readable + " (" + ident.getNamespace() + ")");
	}
}
