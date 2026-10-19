package dev.emi.remastered.info.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.emi.remastered.info.export.ExportRows.IngredientData;
import dev.emi.remastered.info.export.ExportRows.ItemRow;
import dev.emi.remastered.info.export.ExportRows.NoSourceRow;
import dev.emi.remastered.info.export.ExportRows.RecipeRow;
import dev.emi.remastered.info.export.ExportRows.SourceData;
import dev.emi.remastered.info.export.ExportRows.SourceRow;
import dev.emi.remastered.info.export.ExportRows.StackData;
import dev.emi.remastered.info.export.ExportRows.TagRow;

class ExportRowsTest {
	private static final StackData IRON = new StackData("minecraft:iron_ingot", "", "Iron Ingot", "minecraft", "item", false);
	private static final StackData BOOK = new StackData("minecraft:enchanted_book", "{minecraft:stored_enchantments=1}",
		"Enchanted Book, \"Sharp\"", "minecraft", "item", true);

	private static IngredientData stack(String id) {
		return new IngredientData(null, List.of(id), 1, 1.0f);
	}

	@Test
	void ingredientTokens() {
		assertEquals("minecraft:stick", stack("minecraft:stick").token());
		assertEquals("3x minecraft:stick", new IngredientData(null, List.of("minecraft:stick"), 3, 1.0f).token());
		assertEquals("minecraft:oak_planks|minecraft:birch_planks",
			new IngredientData(null, List.of("minecraft:oak_planks", "minecraft:birch_planks"), 1, 1.0f).token());
		assertEquals("#minecraft:planks", new IngredientData("minecraft:planks", List.of(), 1, 1.0f).token());
		assertEquals("2x minecraft:diamond @0.5", new IngredientData(null, List.of("minecraft:diamond"), 2, 0.5f).token());
	}

	@Test
	void recipeRowCsvCells() {
		RecipeRow row = new RecipeRow("minecraft:stick", "minecraft:crafting",
			List.of(new IngredientData("minecraft:planks", List.of(), 2, 1.0f)), List.of(),
			List.of(new IngredientData(null, List.of("minecraft:stick"), 4, 1.0f)));
		assertEquals(List.of("minecraft:stick", "minecraft:crafting", "2x #minecraft:planks", "", "4x minecraft:stick"),
			row.cells());
		assertEquals(ExportKind.RECIPES.columns.size(), row.cells().size());
	}

	@Test
	void recipeRowJoinsSeveralIngredientsWithSemicolon() {
		RecipeRow row = new RecipeRow("", "emi:smithing", List.of(stack("minecraft:a"), stack("minecraft:b")), List.of(),
			List.of(stack("minecraft:c")));
		assertEquals("minecraft:a;minecraft:b", row.cells().get(2));
		assertEquals("", row.cells().get(0));
	}

	@Test
	void recipeRowJson() {
		RecipeRow row = new RecipeRow("minecraft:x", "minecraft:crafting",
			List.of(new IngredientData("minecraft:planks", List.of(), 1, 1.0f)),
			List.of(stack("minecraft:crafting_table")),
			List.of(new IngredientData(null, List.of("minecraft:stick"), 4, 0.25f)));
		JsonObject json = row.toJson();
		assertEquals("minecraft:x", json.get("id").getAsString());
		assertEquals("minecraft:crafting", json.get("category").getAsString());
		JsonObject input = json.getAsJsonArray("inputs").get(0).getAsJsonObject();
		assertEquals("minecraft:planks", input.get("tag").getAsString());
		assertFalse(input.has("stacks"));
		assertFalse(input.has("chance"));
		JsonObject output = json.getAsJsonArray("outputs").get(0).getAsJsonObject();
		assertEquals("minecraft:stick", output.getAsJsonArray("stacks").get(0).getAsString());
		assertEquals(4, output.get("amount").getAsLong());
		assertEquals(0.25f, output.get("chance").getAsFloat());
		assertEquals(1, json.getAsJsonArray("catalysts").size());
	}

	@Test
	void itemRowCellsAndJson() {
		assertEquals(List.of("minecraft:iron_ingot", "", "Iron Ingot", "minecraft", "item", "false"),
			new ItemRow(IRON).cells());
		JsonObject json = new ItemRow(BOOK).toJson();
		assertEquals("minecraft:enchanted_book", json.get("id").getAsString());
		assertEquals("{minecraft:stored_enchantments=1}", json.get("components").getAsString());
		assertTrue(json.get("hidden").getAsBoolean());
		assertFalse(new ItemRow(IRON).toJson().has("components"));
	}

	@Test
	void sourceRow() {
		SourceRow with = new SourceRow(IRON, true, List.of(
			new SourceData("emi_info:produced_by", "emi_info:recipes", "Recipes (3)", true),
			new SourceData("emi_loot:chest", "emi_loot:chest_loot", "Chests", true)));
		assertEquals(ExportKind.SOURCES.columns.size(), with.cells().size());
		assertEquals("true", with.cells().get(6));
		assertEquals("2", with.cells().get(7));
		assertEquals("emi_info:produced_by;emi_loot:chest", with.cells().get(8));
		assertEquals("Recipes (3);Chests", with.cells().get(9));
		JsonObject json = with.toJson();
		assertTrue(json.get("has_source").getAsBoolean());
		JsonArray sources = json.getAsJsonArray("sources");
		assertEquals(2, sources.size());
		assertEquals("emi_loot:chest_loot", sources.get(1).getAsJsonObject().get("provider").getAsString());

		SourceRow none = new SourceRow(IRON, false, List.of());
		assertEquals("false", none.cells().get(6));
		assertEquals("0", none.cells().get(7));
		assertEquals("", none.cells().get(8));
		assertEquals(0, none.toJson().getAsJsonArray("sources").size());
	}

	@Test
	void noSourceRowUsesTheItemColumns() {
		NoSourceRow row = new NoSourceRow(IRON);
		assertEquals(ExportKind.NOSOURCE.columns.size(), row.cells().size());
		assertEquals(new ItemRow(IRON).toJson(), row.toJson());
	}

	@Test
	void tagRow() {
		TagRow row = new TagRow("minecraft:item", "minecraft:planks", List.of("minecraft:oak_planks", "minecraft:birch_planks"));
		assertEquals(List.of("minecraft:item", "minecraft:planks", "2", "minecraft:oak_planks;minecraft:birch_planks"),
			row.cells());
		JsonObject json = row.toJson();
		assertEquals(2, json.get("count").getAsInt());
		assertEquals(2, json.getAsJsonArray("members").size());
	}

	@Test
	void tableCsvHasHeaderAndQuotesCells() {
		Table table = new Table(ExportKind.ITEMS, List.of(new ItemRow(IRON), new ItemRow(BOOK)));
		String[] lines = table.toCsv().split("\r\n");
		assertEquals(3, lines.length);
		assertEquals("id,components,name,mod,type,hidden", lines[0]);
		assertEquals("minecraft:iron_ingot,,Iron Ingot,minecraft,item,false", lines[1]);
		assertEquals("minecraft:enchanted_book,{minecraft:stored_enchantments=1},\"Enchanted Book, \"\"Sharp\"\"\",minecraft,item,true",
			lines[2]);
	}

	@Test
	void tableJsonIsAPrettyPrintedArray() {
		Table table = new Table(ExportKind.ITEMS, List.of(new ItemRow(IRON), new ItemRow(BOOK)));
		String text = table.toJson();
		assertTrue(text.contains("\n  {\n    \"id\": \"minecraft:iron_ingot\""), text);
		assertTrue(text.contains("\"Enchanted Book, \\\"Sharp\\\"\""), text);
		JsonArray parsed = JsonParser.parseString(text).getAsJsonArray();
		assertEquals(2, parsed.size());
		assertEquals("Enchanted Book, \"Sharp\"", parsed.get(1).getAsJsonObject().get("name").getAsString());
	}

	@Test
	void emptyTables() {
		Table table = new Table(ExportKind.NOSOURCE, List.of());
		assertEquals("id,components,name,mod,type,hidden\r\n", table.toCsv());
		assertEquals("[]\n", table.toJson());
		assertEquals("nosource.csv", table.fileName(ExportFormat.CSV));
		assertEquals("nosource.json", table.fileName(ExportFormat.JSON));
	}

	@Test
	void jsonKeysMatchTheCsvColumnsOfFlatKinds() {
		assertEquals(Set.copyOf(ExportKind.ITEMS.columns), new ItemRow(BOOK).toJson().keySet());
		assertEquals(Set.copyOf(ExportKind.NOSOURCE.columns), new NoSourceRow(BOOK).toJson().keySet());
	}

	@Test
	void parsesKindsAndFormats() {
		assertEquals(List.of(ExportKind.values()), ExportKind.parse("all"));
		assertEquals(List.of(ExportKind.NOSOURCE), ExportKind.parse("NoSource"));
		assertTrue(ExportKind.parse("bananas").isEmpty());
		assertEquals(Optional.of(ExportFormat.CSV), ExportFormat.parse("CSV"));
		assertEquals(Optional.of(ExportFormat.JSON), ExportFormat.parse("json"));
		assertTrue(ExportFormat.parse("xml").isEmpty());
	}

	@Test
	void langFilesHaveTheSameExportKeys() throws Exception {
		Set<String> en = exportKeys("en_us");
		Set<String> de = exportKeys("de_de");
		assertFalse(en.isEmpty());
		assertEquals(en, de);
	}

	private static Set<String> exportKeys(String lang) throws Exception {
		try (Reader reader = new InputStreamReader(ExportRowsTest.class.getResourceAsStream("/assets/emi_info/lang/" + lang + ".json"),
				StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject().keySet().stream()
				.filter(k -> k.startsWith("emi_info.export.")).collect(Collectors.toSet());
		}
	}
}
