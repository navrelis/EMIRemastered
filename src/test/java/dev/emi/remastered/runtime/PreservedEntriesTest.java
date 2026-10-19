package dev.emi.remastered.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PreservedEntriesTest {

	/** Mimics the EMI loaders: readable entries go into memory, the others are kept with their anchor. */
	private static List<JsonElement> load(JsonArray file, Predicate<JsonElement> readable, PreservedEntries kept) {
		List<JsonElement> memory = new ArrayList<>();
		JsonElement previous = null;
		int index = 0;
		for (JsonElement el : file) {
			if (readable.test(el)) {
				memory.add(el.deepCopy());
			} else {
				kept.keep(el, previous, index);
			}
			previous = el;
			index++;
		}
		return memory;
	}

	private static JsonArray save(List<JsonElement> memory, PreservedEntries kept) {
		JsonArray arr = new JsonArray();
		memory.forEach(arr::add);
		return kept.mergeInto(arr);
	}

	private static JsonArray array(String json) {
		return JsonParser.parseString(json).getAsJsonArray();
	}

	private static boolean known(JsonElement el) {
		String s = el.isJsonObject() ? el.getAsJsonObject().get("stack").getAsString() : el.getAsString();
		return s.startsWith("item:minecraft:") && !s.contains("{");
	}

	@Test
	void unchangedRoundTripKeepsFileIdentical() {
		JsonArray file = array("""
				["item:nonexistent_mod:thing", "item:minecraft:stone", "item:minecraft:dirt{bad nbt",
				 "item:betternether:ruby", "item:betternether:ruby_fire", "item:minecraft:dirt", "biome:minecraft:plains"]""");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		assertEquals(2, memory.size());
		assertEquals(5, kept.size());
		assertEquals(file, save(memory, kept));
	}

	@Test
	void objectsWithRecipeSurviveUnchanged() {
		JsonArray file = array("""
				[{"stack": "item:minecraft:stone", "recipe": "minecraft:stone"},
				 {"stack": "item:gone:thing", "recipe": "gone:thing"},
				 {"stack": "item:minecraft:dirt"}]""");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		assertEquals(file, save(memory, kept));
	}

	@Test
	void keptEntryFollowsItsAnchorWhenNewEntriesArePrepended() {
		JsonArray file = array("""
				["item:minecraft:stone", "item:gone:a", "item:minecraft:dirt"]""");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		memory.add(0, JsonParser.parseString("\"item:minecraft:apple\""));
		memory.add(0, JsonParser.parseString("\"item:minecraft:bread\""));
		assertEquals(array("""
				["item:minecraft:bread", "item:minecraft:apple", "item:minecraft:stone", "item:gone:a", "item:minecraft:dirt"]"""),
				save(memory, kept));
	}

	@Test
	void keptEntryFallsBackToItsIndexWhenTheAnchorWasRemoved() {
		JsonArray file = array("""
				["item:minecraft:a", "item:minecraft:b", "item:gone:x", "item:minecraft:c", "item:minecraft:d"]""");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		memory.remove(JsonParser.parseString("\"item:minecraft:b\""));
		assertEquals(array("""
				["item:minecraft:a", "item:minecraft:c", "item:gone:x", "item:minecraft:d"]"""), save(memory, kept));
		memory.clear();
		assertEquals(array("[\"item:gone:x\"]"), save(memory, kept));
	}

	@Test
	void consecutiveKeptEntriesStayInOrderAndFirstStaysFirst() {
		JsonArray file = array("""
				["item:gone:1", "item:gone:2", "item:minecraft:a", "item:gone:3", "item:gone:4"]""");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		assertEquals(file, save(memory, kept));
	}

	@Test
	void entryThatLoadsAgainIsNotDuplicated() {
		JsonArray file = array("[\"item:gone:x\", \"item:minecraft:a\"]");
		PreservedEntries kept = new PreservedEntries();
		List<JsonElement> memory = load(file, PreservedEntriesTest::known, kept);
		memory.add(JsonParser.parseString("\"item:gone:x\""));
		assertEquals(array("[\"item:minecraft:a\", \"item:gone:x\"]"), save(memory, kept));
	}

	@Test
	void emptyKeepsReturnsTheSameArrayAndClearForgets() {
		PreservedEntries kept = new PreservedEntries();
		JsonArray arr = array("[1, 2]");
		assertSame(arr, kept.mergeInto(arr));
		kept.keep(JsonParser.parseString("3"), null, 0);
		kept.clear();
		assertSame(arr, kept.mergeInto(arr));
	}

	@Test
	void keptElementIsCopiedNotShared() {
		PreservedEntries kept = new PreservedEntries();
		JsonObject raw = JsonParser.parseString("{\"stack\": \"item:gone:x\"}").getAsJsonObject();
		kept.keep(raw, null, 0);
		JsonArray out = kept.mergeInto(new JsonArray());
		out.get(0).getAsJsonObject().addProperty("stack", "changed");
		assertEquals("item:gone:x", kept.mergeInto(new JsonArray()).get(0).getAsJsonObject().get("stack").getAsString());
	}

	@Test
	void mergeObjectAddsMissingKeysAndMissingArrayElements() {
		JsonObject current = JsonParser.parseString("""
				{"minecraft:a": ["item:minecraft:a"], "minecraft:b": ["item:minecraft:b"]}""").getAsJsonObject();
		JsonObject unread = new JsonObject();
		PreservedEntries.keepInObject(unread, "minecraft:a", JsonParser.parseString("\"item:gone:x\""));
		PreservedEntries.keepInObject(unread, "minecraft:a", JsonParser.parseString("\"item:minecraft:a\""));
		unread.add("gone:recipe", array("[\"item:gone:y\"]"));
		PreservedEntries.mergeObject(current, unread);
		assertEquals(JsonParser.parseString("""
				{"minecraft:a": ["item:minecraft:a", "item:gone:x"], "minecraft:b": ["item:minecraft:b"],
				 "gone:recipe": ["item:gone:y"]}"""), current);
	}

	@Test
	void mergeObjectKeepsCurrentValueForScalarKeys() {
		JsonObject current = JsonParser.parseString("{\"#minecraft:logs\": \"item:minecraft:oak_log\"}").getAsJsonObject();
		JsonObject unread = JsonParser.parseString("""
				{"#minecraft:logs": "item:gone:log", "#gone:tag": "item:gone:thing"}""").getAsJsonObject();
		PreservedEntries.mergeObject(current, unread);
		assertEquals(JsonParser.parseString("""
				{"#minecraft:logs": "item:minecraft:oak_log", "#gone:tag": "item:gone:thing"}"""), current);
	}
}
