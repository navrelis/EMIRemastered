package dev.emi.remastered.loot.structure;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * How many chest loot tables got a structure. {@code pages} are the chest loot pages that are sent to clients (one per
 * chest loot sender, and exactly the ones that have a structure get an entry in the structure payloads);
 * {@code byPath} are the loot tables whose id path starts with {@code chests/}, which is a different set: the parser
 * also classifies tables by their context type, and propagation reaches tables of any name.
 */
public record ChestSummary(int pagesWithStructure, int pages, int byPathWithStructure, int byPath) {
	public static ChestSummary of(Collection<Identifier> pageTables, Collection<Identifier> pathTables, Function<Identifier, List<StructureRef>> structuresOf) {
		return new ChestSummary(countKnown(pageTables, structuresOf), pageTables.size(), countKnown(pathTables, structuresOf), pathTables.size());
	}

	/** The number of tables that have at least one structure. */
	public static int countKnown(Collection<Identifier> tables, Function<Identifier, List<StructureRef>> structuresOf) {
		int known = 0;
		for (Identifier table : tables) {
			if (!structuresOf.apply(table).isEmpty()) {
				known++;
			}
		}
		return known;
	}

	public String describe() {
		return "Chest loot pages with a known structure: " + pagesWithStructure + " of " + pages
			+ " (chest loot tables by path: " + byPathWithStructure + " of " + byPath + ")";
	}
}
