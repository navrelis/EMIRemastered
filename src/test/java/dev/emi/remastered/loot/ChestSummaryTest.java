package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.loot.structure.ChestSummary;
import dev.emi.remastered.loot.structure.StructureRef;
import net.minecraft.util.Identifier;

class ChestSummaryTest {
	@Test
	void countsTheDeliveredPagesNotThePathTables() {
		Identifier a = Identifier.of("m", "chests/a");
		Identifier b = Identifier.of("m", "chests/b");
		Identifier reached = Identifier.of("m", "rewards/c");
		StructureRef ref = new StructureRef(Identifier.of("m", "s"), List.of());
		Map<Identifier, List<StructureRef>> structures = Map.of(a, List.of(ref), reached, List.of(ref));
		ChestSummary summary = ChestSummary.of(List.of(a, b, reached), List.of(a, b), t -> structures.getOrDefault(t, List.of()));
		assertEquals(new ChestSummary(2, 3, 1, 2), summary);
		assertEquals("Chest loot pages with a known structure: 2 of 3 (chest loot tables by path: 1 of 2)", summary.describe());
	}
}
