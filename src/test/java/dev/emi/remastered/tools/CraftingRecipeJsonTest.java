package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.emi.remastered.tools.recipe.CraftingRecipeJson;
import dev.emi.remastered.tools.recipe.CraftingRecipeJson.Cell;
import dev.emi.remastered.tools.recipe.CraftingRecipeJson.Pattern;

class CraftingRecipeJsonTest {
	private static final JsonElement IRON = item("minecraft:iron_ingot");
	private static final JsonElement STICK = item("minecraft:stick");
	private static final JsonElement INGOT_TAG = JsonParser.parseString("{\"tag\":\"c:ingots/iron\"}");

	private static JsonElement item(String id) {
		return JsonParser.parseString("{\"item\":\"" + id + "\"}");
	}

	private static Cell cell(JsonElement ingredient) {
		return new Cell(ingredient);
	}

	/** A grid from rows where I is iron, S is stick, T is a tag and a space is empty. */
	private static List<Cell> grid(String... rows) {
		List<Cell> cells = new ArrayList<>();
		for (String row : rows) {
			for (char c : row.toCharArray()) {
				cells.add(switch (c) {
					case 'I' -> cell(IRON);
					case 'S' -> cell(STICK);
					case 'T' -> cell(INGOT_TAG);
					default -> Cell.EMPTY;
				});
			}
		}
		return cells;
	}

	@Test
	void pickaxePatternUsesTheItemInitials() {
		Pattern pattern = CraftingRecipeJson.pattern(3, 3, grid("III", " S ", " S "));
		assertNotNull(pattern);
		assertEquals(List.of("III", " S ", " S "), pattern.rows());
		assertEquals(List.of('I', 'S'), List.copyOf(pattern.key().keySet()));
		assertEquals(IRON, pattern.key().get('I'));
		assertEquals(STICK, pattern.key().get('S'));
	}

	@Test
	void sameIngredientsShareOneKeyAndEqualJsonIsCompared() {
		// two separately built but equal ingredient objects
		List<Cell> cells = List.of(cell(item("minecraft:iron_ingot")), cell(item("minecraft:iron_ingot")), cell(STICK));
		Pattern pattern = CraftingRecipeJson.pattern(3, 1, cells);
		assertEquals(List.of("IIS"), pattern.rows());
		assertEquals(2, pattern.key().size());
	}

	@Test
	void collidingInitialsUseTheNextLetterOfTheName() {
		List<Cell> cells = List.of(cell(item("minecraft:stone")), cell(item("minecraft:stick")), cell(item("minecraft:sand")));
		Pattern pattern = CraftingRecipeJson.pattern(3, 1, cells);
		// stone takes S, stick takes its next free letter T, sand takes A
		assertEquals(List.of("STA"), pattern.rows());
		assertEquals(3, pattern.key().size());
	}

	@Test
	void keysFallBackToTheAlphabetWhenTheNameHasNoFreeLetter() {
		List<Cell> cells = List.of(cell(item("minecraft:a")), cell(item("x:a")), cell(item("x:b")));
		Pattern pattern = CraftingRecipeJson.pattern(3, 1, cells);
		// both names are "a": the second one gets the first free letter of the alphabet, B, so the third takes C
		assertEquals(List.of("ABC"), pattern.rows());
		assertEquals(3, pattern.key().size());
	}

	@Test
	void emptyOuterRowsAndColumnsAreTrimmed() {
		Pattern pattern = CraftingRecipeJson.pattern(3, 3, grid("   ", " I ", " S "));
		assertEquals(List.of("I", "S"), pattern.rows());
	}

	@Test
	void emptyCellsInsideStayAsSpaces() {
		Pattern pattern = CraftingRecipeJson.pattern(3, 2, grid("I I", " S "));
		assertEquals(List.of("I I", " S "), pattern.rows());
	}

	@Test
	void anEmptyGridOrAWrongSizeHasNoPattern() {
		assertNull(CraftingRecipeJson.pattern(2, 2, grid("  ", "  ")));
		assertNull(CraftingRecipeJson.pattern(2, 2, grid("II")));
		assertNull(CraftingRecipeJson.pattern(0, 0, List.of()));
	}

	@Test
	void shapedRecipeJsonHasTheDataPackFields() {
		JsonElement result = JsonParser.parseString("{\"id\":\"minecraft:iron_pickaxe\"}");
		JsonObject json = CraftingRecipeJson.shaped("equipment", "", 3, 3, grid("III", " S ", " S "), result, true);
		assertEquals(List.of("type", "category", "pattern", "key", "result"), List.copyOf(json.keySet()));
		assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
		assertEquals("equipment", json.get("category").getAsString());
		assertEquals("[\"III\",\" S \",\" S \"]", json.get("pattern").toString());
		assertEquals("{\"I\":{\"item\":\"minecraft:iron_ingot\"},\"S\":{\"item\":\"minecraft:stick\"}}", json.get("key").toString());
		assertEquals(result, json.get("result"));
	}

	@Test
	void shapedRecipeJsonKeepsGroupAndHiddenNotification() {
		JsonObject json = CraftingRecipeJson.shaped("misc", "wooden_door", 1, 2, grid("I", "I"), item("x:y"), false);
		assertEquals("wooden_door", json.get("group").getAsString());
		assertFalse(json.get("show_notification").getAsBoolean());
		assertEquals(List.of("type", "category", "group", "pattern", "key", "result", "show_notification"),
			List.copyOf(json.keySet()));
	}

	@Test
	void shapedRecipeWithoutIngredientsIsNull() {
		assertNull(CraftingRecipeJson.shaped("misc", "", 1, 1, grid(" "), item("x:y"), true));
	}

	@Test
	void shapelessRecipeJsonListsTheIngredients() {
		JsonElement result = JsonParser.parseString("{\"id\":\"minecraft:flint_and_steel\"}");
		JsonObject json = CraftingRecipeJson.shapeless("equipment", "", List.of(IRON, item("minecraft:flint")), result);
		assertEquals(List.of("type", "category", "ingredients", "result"), List.copyOf(json.keySet()));
		assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());
		assertEquals(2, json.getAsJsonArray("ingredients").size());
		assertEquals(result, json.get("result"));
	}

	@Test
	void tagIngredientsGetAKeyNamedAfterTheTag() {
		Pattern pattern = CraftingRecipeJson.pattern(2, 1, grid("TS"));
		// the hint of {"tag":"c:ingots/iron"} is "iron"
		assertEquals(INGOT_TAG, pattern.key().get('I'));
	}

	@Test
	void equalTagContentPrefersTheVanillaTagThenTheAlphabet() {
		assertEquals("minecraft:planks", CraftingRecipeJson.chooseTag(List.of("c:planks", "minecraft:planks")));
		assertEquals("a:x", CraftingRecipeJson.chooseTag(List.of("b:x", "a:x")));
		assertNull(CraftingRecipeJson.chooseTag(List.of()));
	}
}
