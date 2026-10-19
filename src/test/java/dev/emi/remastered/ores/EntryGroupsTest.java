package dev.emi.remastered.ores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.ores.display.BiomeSummary;
import dev.emi.remastered.ores.display.ChanceFormat;
import dev.emi.remastered.ores.display.EntryGroups;
import dev.emi.remastered.ores.display.HeightModel.Resolved;
import dev.emi.remastered.ores.display.HeightModel.Shape;
import net.minecraft.util.Identifier;

class EntryGroupsTest {
	@Test
	void groupsEqualKeysInOrderOfFirstAppearance() {
		List<String> items = List.of("a1", "b1", "a2", "c1", "b2", "a3");
		List<List<String>> groups = EntryGroups.group(items, s -> s.charAt(0));
		assertEquals(List.of(List.of("a1", "a2", "a3"), List.of("b1", "b2"), List.of("c1")), groups);
	}

	@Test
	void noItemsNoGroups() {
		assertTrue(EntryGroups.group(List.<String>of(), s -> s).isEmpty());
	}

	@Test
	void identicalHeightRowsMergeAndDistinctOnesStay() {
		// three placed features of diamond ore: the same row three times, plus one with another range
		Resolved best = new Resolved(Shape.TRIANGULAR, -64, 16, -64, -64, 0, 31);
		Resolved sameText = new Resolved(Shape.TRIANGULAR, -64, 16, -64, -64, 2, 29);
		Resolved other = new Resolved(Shape.UNIFORM, -64, -4, -64, -4, 0, 31);
		assertEquals(EntryGroups.heightKey(best), EntryGroups.heightKey(sameText));
		assertNotEquals(EntryGroups.heightKey(best), EntryGroups.heightKey(other));
		List<Resolved> rows = List.of(best, sameText, other, best);
		List<List<Resolved>> groups = EntryGroups.group(rows, EntryGroups::heightKey);
		assertEquals(2, groups.size());
		assertEquals(3, groups.get(0).size());
		assertEquals(1, groups.get(1).size());
	}

	@Test
	void aMissingHeightHasAKeyOfItsOwn() {
		assertEquals(List.of(), EntryGroups.heightKey(null));
	}

	@Test
	void sortsBiomesByTheirNameThenById() {
		Identifier a = Identifier.of("minecraft", "savanna");
		Identifier b = Identifier.of("minecraft", "badlands");
		Identifier c = Identifier.of("mod", "badlands");
		List<Identifier> sorted = BiomeSummary.sortedByName(List.of(a, c, b), id -> id.getPath().equals("savanna") ? "Savanna" : "badlands");
		assertEquals(List.of(b, c, a), sorted);
		assertEquals(sorted, BiomeSummary.sortedByName(List.of(b, a, c), id -> id.getPath().equals("savanna") ? "Savanna" : "badlands"));
	}

	@Test
	void formatsChancesAsPercent() {
		assertEquals("50", ChanceFormat.percent(0.5f));
		assertEquals("12.5", ChanceFormat.percent(0.125f));
		assertEquals("100", ChanceFormat.percent(1.0f));
		assertEquals("70", ChanceFormat.percent(0.7f));
		assertEquals("0", ChanceFormat.percent(0f));
	}
}
