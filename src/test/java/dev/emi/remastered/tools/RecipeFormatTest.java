package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.emi.remastered.tools.recipe.RecipeJsonFormatter;
import dev.emi.remastered.tools.recipe.RecipeSnapshot;
import dev.emi.remastered.tools.recipe.RecipeSnapshot.Slot;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter.Labels;

class RecipeFormatTest {
	private static final Slot RAW_IRON = Slot.stack("item", "minecraft:raw_iron", "Raw Iron", 1, 1, null);
	private static final Slot INGOT = Slot.stack("item", "minecraft:iron_ingot", "Iron Ingot", 1, 1, null);
	private static final Slot FURNACE = Slot.stack("item", "minecraft:furnace", "Furnace", 1, 1, null);

	private static final RecipeSnapshot SMELTING = new RecipeSnapshot("minecraft:iron_ingot_from_smelting_raw_iron",
		"emi:smelting", "Smelting", List.of(RAW_IRON), List.of(INGOT), List.of(FURNACE));

	@Test
	void textOfASimpleRecipe() {
		assertEquals("""
			Smelting (emi:smelting)
			Recipe id: minecraft:iron_ingot_from_smelting_raw_iron
			Inputs:
			  - 1x Raw Iron [minecraft:raw_iron]
			Outputs:
			  - 1x Iron Ingot [minecraft:iron_ingot]
			Catalysts:
			  - 1x Furnace [minecraft:furnace]""", RecipeTextFormatter.format(SMELTING, Labels.ENGLISH));
	}

	@Test
	void textShowsTagsAlternativesChancesFluidsAndEmptySlots() {
		Slot planks = Slot.tag("item", "minecraft:planks", "Planks", 12, 2, 1);
		Slot anyOf = Slot.anyOf(List.of(
			Slot.stack("item", "minecraft:a", null, 1, 1, null), Slot.stack("item", "minecraft:b", "B", 1, 1, null),
			Slot.stack("item", "minecraft:c", null, 1, 1, null), Slot.stack("item", "minecraft:d", null, 1, 1, null),
			Slot.stack("item", "minecraft:e", null, 1, 1, null), Slot.stack("item", "minecraft:f", null, 1, 1, null),
			Slot.stack("item", "minecraft:g", null, 1, 1, null), Slot.stack("item", "minecraft:h", null, 1, 1, null)), 3, 1);
		Slot water = Slot.stack("fluid", "minecraft:water", "Water", 81000, 1, null);
		Slot bonus = Slot.stack("item", "minecraft:gold_nugget", "Gold Nugget", 2, 0.25f, null);
		RecipeSnapshot recipe = new RecipeSnapshot(null, "mod:machine", "mod:machine", List.of(planks, Slot.empty(), anyOf, water),
			List.of(bonus), List.of());
		assertEquals("""
			mod:machine
			Inputs:
			  - 2x Planks [#minecraft:planks] (12 entries)
			  - 3x any of minecraft:a, B [minecraft:b], minecraft:c, minecraft:d, minecraft:e, minecraft:f, and 2 more
			  - 1000 mB Water [minecraft:water]
			  (1 empty slots left out)
			Outputs:
			  - 2x Gold Nugget [minecraft:gold_nugget], 25% chance""", RecipeTextFormatter.format(recipe, Labels.ENGLISH));
	}

	@Test
	void fluidAmountsInMillibuckets() {
		assertEquals("250 mB", RecipeTextFormatter.amount(Slot.stack("fluid", "minecraft:lava", null, 20250, 1, null)));
		assertEquals("0.5 mB", RecipeTextFormatter.amount(Slot.stack("fluid", "minecraft:lava", null, 40, 1, null)));
		assertEquals("4x", RecipeTextFormatter.amount(Slot.stack("item", "minecraft:stick", null, 4, 1, null)));
	}

	@Test
	void textUsesTheGivenLabels() {
		Labels german = new Labels("Rezept-ID: %s", "Zutaten:", "Ergebnisse:", "Katalysatoren:", "eines von %s", "und %s weitere",
			"%s%% Chance", "%s Einträge", "%s leere Felder ausgelassen");
		String text = RecipeTextFormatter.format(SMELTING, german);
		assertTrue(text.contains("Rezept-ID: minecraft:iron_ingot_from_smelting_raw_iron"));
		assertTrue(text.contains("Zutaten:"));
		assertTrue(text.contains("Katalysatoren:"));
	}

	@Test
	void genericJson() {
		Slot anyOf = Slot.anyOf(List.of(Slot.stack("item", "minecraft:a", "A", 1, 1, null),
			Slot.stack("item", "minecraft:b", "B", 1, 1, null)), 2, 1);
		RecipeSnapshot recipe = new RecipeSnapshot("mod:/thing", "mod:machine", "Machine",
			List.of(Slot.tag("item", "minecraft:planks", "Planks", 5, 1, 1), Slot.empty(), anyOf,
				Slot.stack("fluid", "minecraft:water", "Water", 81000, 1, null)),
			List.of(Slot.stack("item", "minecraft:stick", "Stick", 4, 0.5f, "{\"minecraft:damage\":3}")), List.of());
		JsonObject json = RecipeJsonFormatter.toJson(recipe);
		assertEquals(JsonParser.parseString("""
			{
			  "id": "mod:/thing",
			  "category": "mod:machine",
			  "inputs": [
			    {"type": "item", "tag": "minecraft:planks", "count": 1},
			    null,
			    {"any_of": [{"type": "item", "id": "minecraft:a"}, {"type": "item", "id": "minecraft:b"}], "count": 2},
			    {"type": "fluid", "id": "minecraft:water", "amount": 81000}
			  ],
			  "outputs": [
			    {"type": "item", "id": "minecraft:stick", "count": 4, "components": "{\\"minecraft:damage\\":3}", "chance": 0.5}
			  ]
			}"""), json);
		JsonArray inputs = json.getAsJsonArray("inputs");
		assertTrue(inputs.get(1).isJsonNull());
		assertTrue(!json.has("catalysts"));
	}

	@Test
	void prettyJsonRoundTrips() {
		String pretty = RecipeJsonFormatter.pretty(RecipeJsonFormatter.toJson(SMELTING));
		assertTrue(pretty.contains("\n  \"category\": \"emi:smelting\""), pretty);
		assertEquals(RecipeJsonFormatter.toJson(SMELTING), JsonParser.parseString(pretty));
	}
}
