package dev.emi.remastered.effects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/** Pure logic of the effects module: formatting, source table parser, pagination. */
class EffectsLogicTest {

	@Test
	void romanNumerals() {
		assertEquals("I", EffectFormat.level(0));
		assertEquals("II", EffectFormat.level(1));
		assertEquals("IV", EffectFormat.roman(4));
		assertEquals("X", EffectFormat.roman(10));
		assertEquals("XLIX", EffectFormat.roman(49));
		assertEquals("0", EffectFormat.roman(0));
		assertEquals("4000", EffectFormat.roman(4000));
	}

	@Test
	void percentages() {
		assertEquals("100%", EffectFormat.percent(1f));
		assertEquals("30%", EffectFormat.percent(0.3f));
		assertEquals("7.5%", EffectFormat.percent(0.075f));
		assertEquals("0%", EffectFormat.percent(0f));
	}

	@Test
	void durations() {
		assertEquals("0:45", EffectFormat.duration(900));
		assertEquals("0:01", EffectFormat.duration(1));
		assertEquals("3:00", EffectFormat.duration(3600));
		assertEquals("100:00", EffectFormat.duration(120000));
		assertEquals("0:00", EffectFormat.duration(0));
		assertEquals("∞", EffectFormat.duration(-1));
	}

	@Test
	void sourceLabels() {
		assertEquals("II · 0:45", EffectFormat.label(1, 900, 1f));
		assertEquals("I · 0:08 · 30%", EffectFormat.label(0, 160, 0.3f));
		assertEquals("0:08", EffectFormat.label(-1, 160, 1f));
		assertEquals("II", EffectFormat.label(1, -1, 1f));
		assertEquals("", EffectFormat.label(-1, -1, 1f));
	}

	@Test
	void modifierValues() {
		assertEquals("20", EffectFormat.modifierValue(0.2, 2));
		assertEquals("15", EffectFormat.modifierValue(-0.15, 2));
		assertEquals("3", EffectFormat.modifierValue(3.0, 0));
		assertEquals("4", EffectFormat.modifierValue(-4.0, 0));
		assertEquals("2.5", EffectFormat.modifierValue(2.5, 0));
		assertEquals("12.5", EffectFormat.modifierValue(0.125, 1));
		assertEquals("0.25", EffectFormat.modifierValue(0.25, 0));
	}

	@Test
	void groupsAreOrderedByLevelDurationAndChance() {
		record Source(String name, int amplifier, int duration, float probability) {
		}
		List<Source> sources = List.of(new Source("c", 1, 100, 1f), new Source("a", 0, 900, 1f), new Source("b", 0, 900, 1f),
			new Source("d", 0, 100, 0.5f), new Source("e", 0, 100, 1f));
		var groups = SourceGroups.group(sources, s -> new SourceGroups.Key(s.amplifier(), s.duration(), s.probability()));
		assertEquals(4, groups.size());
		List<String> order = new ArrayList<>();
		groups.values().forEach(g -> g.forEach(s -> order.add(s.name())));
		assertEquals(List.of("e", "d", "a", "b", "c"), order);
	}

	@Test
	void slotRows() {
		assertEquals(0, SourceGroups.rows(0, 8));
		assertEquals(1, SourceGroups.rows(1, 8));
		assertEquals(1, SourceGroups.rows(8, 8));
		assertEquals(2, SourceGroups.rows(9, 8));
	}

	@Test
	void extraStacksFormat() {
		var def = EffectSourceDef.parseExtra(JsonParser.parseString(
			"{\"id\":\"minecraft:wither\",\"stacks\":[\"item:minecraft:wither_spawn_egg\",\"item:minecraft:wither_rose\"]}").getAsJsonObject());
		assertEquals(List.of("minecraft:wither"), def.effects());
		assertEquals(2, def.stacks().size());
		assertEquals("", def.text());
		assertFalse(def.cure());
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> EffectSourceDef.parseExtra(JsonParser.parseString("{\"id\":\"a:b\"}").getAsJsonObject()));
	}

	@Test
	void stackStringSpellings() {
		assertEquals("minecraft:speed", EffectFormat.parseStackString("emiffect:effect:minecraft:speed"));
		assertEquals("minecraft:speed", EffectFormat.parseStackString("mob_effect:minecraft:speed"));
		assertEquals("minecraft:speed", EffectFormat.parseStackString("emiffect:minecraft:speed"));
		assertEquals("mod:effect", EffectFormat.parseStackString("emiffect:mod:effect"));
		assertEquals("minecraft:speed", EffectFormat.parseStackString("mob_effect:speed"));
		assertNull(EffectFormat.parseStackString("speed"));
		assertNull(EffectFormat.parseStackString(null));
	}

	@Test
	void sourceTableParses() {
		List<String> errors = new ArrayList<>();
		var json = JsonParser.parseString("""
			{"sources": [
			  {"effect": "minecraft:poison", "entities": ["minecraft:cave_spider", "minecraft:bee"],
			   "text": "emiffect.source.x", "amplifier": 1, "duration": 150},
			  {"kind": "cure", "effects": ["*"], "stacks": ["item:minecraft:milk_bucket"]},
			  {"effects": ["minecraft:wither"], "stacks": "item:minecraft:wither_rose", "text": "t"},
			  {"effect": "minecraft:speed"},
			  {"effects": [], "text": "t"},
			  {"effect": "minecraft:speed", "text": "t", "kind": "weird"},
			  "nonsense"
			]}""");
		List<EffectSourceDef> defs = EffectSourceDef.parse(json, errors::add);
		assertEquals(3, defs.size());
		assertEquals(4, errors.size());

		EffectSourceDef poison = defs.get(0);
		assertFalse(poison.cure());
		assertEquals(List.of("minecraft:poison"), poison.effects());
		assertEquals(2, poison.entities().size());
		assertEquals(1, poison.amplifier());
		assertEquals(150, poison.duration());
		assertTrue(poison.hasReferences());

		EffectSourceDef milk = defs.get(1);
		assertTrue(milk.cure());
		assertEquals(List.of(EffectSourceDef.ALL_EFFECTS), milk.effects());
		assertEquals(-1, milk.amplifier());
		assertEquals(-1, milk.duration());
		assertEquals("", milk.text());

		assertEquals(1, defs.get(2).stacks().size());
	}

	@Test
	void sourceTableAcceptsPlainArrayAndRejectsGarbage() {
		List<String> errors = new ArrayList<>();
		assertEquals(1, EffectSourceDef.parse(JsonParser.parseString("[{\"effect\":\"a:b\",\"text\":\"k\"}]"), errors::add).size());
		assertTrue(errors.isEmpty());
		assertTrue(EffectSourceDef.parse(JsonParser.parseString("{\"x\":1}"), errors::add).isEmpty());
		assertEquals(1, errors.size());
	}

	@Test
	void singlePageWhenItFits() {
		assertArrayEquals(new int[] {0}, PagePlanner.plan(new int[] {10, 10, 10}, new boolean[3], 30, 14, 12));
	}

	@Test
	void pagesAreFilledAndNavigationHeightReserved() {
		int[] heights = {20, 20, 20, 20, 20};
		// capacity 60 but the content is 100 -> nav (14) reserved: 46 per page -> 2 blocks per page
		assertArrayEquals(new int[] {0, 2, 4}, PagePlanner.plan(heights, new boolean[5], 60, 14, 0));
	}

	@Test
	void continuationHeaderShrinksLaterPages() {
		int[] heights = {30, 20, 20, 20};
		// cap 45: page 0 = 30 (+20 does not fit); later pages have 12 used for the header -> 33 left -> 1 block
		assertArrayEquals(new int[] {0, 1, 2, 3}, PagePlanner.plan(heights, new boolean[4], 55, 10, 12));
	}

	@Test
	void headingsStayWithTheirFirstBlock() {
		int[] heights = {20, 10, 20, 20};
		boolean[] keep = {false, true, false, false};
		// cap 36 (50-14): block 1 is a heading, it must move to the next page together with block 2
		assertArrayEquals(new int[] {0, 1, 3}, PagePlanner.plan(heights, keep, 50, 14, 0));
	}

	@Test
	void oversizedBlockStillGetsAPage() {
		int[] pages = PagePlanner.plan(new int[] {100, 10}, new boolean[2], 50, 10, 0);
		assertArrayEquals(new int[] {0, 1}, pages);
	}
}
