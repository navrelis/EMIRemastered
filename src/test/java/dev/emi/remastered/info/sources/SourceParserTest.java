package dev.emi.remastered.info.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SourceParserTest {
	private static final String FILE = "pack:emi_sources/heads/test.json";

	private final List<String> problems = new ArrayList<>();

	private List<SourceEntry> parse(String json) {
		return parse(json, Set.of("botania"));
	}

	private List<SourceEntry> parse(String json, Set<String> mods) {
		return SourceParser.parse(FILE, json, mods::contains, (file, index, reason) -> problems.add(file + "#" + index + ": " + reason));
	}

	@Test
	void validEntryWithAllFields() {
		List<SourceEntry> entries = parse("""
			{"sources": [{
				"id": "pack:piglin_head_charged_creeper",
				"output": "minecraft:piglin_head",
				"count": 2,
				"from": ["minecraft:piglin_spawn_egg", "#minecraft:logs"],
				"text": "emi_sources.pack.piglin",
				"chance": 0.25,
				"requires_mod": "botania"
			}]}""");
		assertTrue(problems.isEmpty(), problems.toString());
		assertEquals(1, entries.size());
		SourceEntry e = entries.get(0);
		assertEquals("pack:piglin_head_charged_creeper", e.id());
		assertEquals("minecraft:piglin_head", e.output());
		assertEquals(2, e.count());
		assertEquals(List.of("minecraft:piglin_spawn_egg", "#minecraft:logs"), e.from());
		assertEquals("emi_sources.pack.piglin", e.text());
		assertEquals(0.25, e.chance());
		assertEquals("botania", e.requiresMod());
		assertEquals("/other_sources/pack/piglin_head_charged_creeper", e.recipePath());
	}

	@Test
	void defaultsForOptionalFields() {
		List<SourceEntry> entries = parse("""
			{"sources": [{"output": "minecraft:stick", "text": "A stick."}, {"output": "minecraft:dirt", "text": "Dirt."}]}""");
		assertTrue(problems.isEmpty());
		assertEquals(2, entries.size());
		SourceEntry first = entries.get(0);
		assertEquals("pack:heads/test/0", first.id());
		assertEquals(1, first.count());
		assertTrue(first.from().isEmpty());
		assertNull(first.chance());
		assertNull(first.requiresMod());
		assertEquals("pack:heads/test/1", entries.get(1).id());
		assertEquals("/other_sources/pack/heads/test/0", first.recipePath());
	}

	@Test
	void idWithoutNamespaceUsesTheFileNamespace() {
		List<SourceEntry> entries = parse("""
			{"sources": [{"id": "my_head", "output": "minecraft:stick", "text": "x"}]}""");
		assertEquals("pack:my_head", entries.get(0).id());
	}

	@Test
	void missingOutputSkipsOnlyThatEntry() {
		List<SourceEntry> entries = parse("""
			{"sources": [{"text": "no output"}, {"output": "minecraft:stick", "text": "ok"}]}""");
		assertEquals(1, entries.size());
		assertEquals(1, entries.get(0).index());
		assertEquals(1, problems.size());
		assertTrue(problems.get(0).contains(FILE + "#0"), problems.get(0));
		assertTrue(problems.get(0).contains("missing \"output\""), problems.get(0));
	}

	@Test
	void missingTextIsInvalid() {
		assertTrue(parse("""
			{"sources": [{"output": "minecraft:stick"}]}""").isEmpty());
		assertTrue(problems.get(0).contains("missing \"text\""), problems.get(0));
	}

	@Test
	void badChanceIsRejected() {
		for (String chance : List.of("1.5", "-0.1", "\"half\"", "true")) {
			problems.clear();
			assertTrue(parse("{\"sources\": [{\"output\": \"minecraft:stick\", \"text\": \"x\", \"chance\": " + chance + "}]}").isEmpty(), chance);
			assertEquals(1, problems.size(), chance);
			assertTrue(problems.get(0).contains("chance"), problems.get(0));
		}
	}

	@Test
	void chanceBoundsAreInclusive() {
		assertEquals(2, parse("""
			{"sources": [{"output": "minecraft:stick", "text": "x", "chance": 0}, {"output": "minecraft:dirt", "text": "x", "chance": 1}]}""").size());
		assertTrue(problems.isEmpty());
	}

	@Test
	void badCountIsRejected() {
		for (String count : List.of("0", "65", "1.5", "\"two\"")) {
			problems.clear();
			assertTrue(parse("{\"sources\": [{\"output\": \"minecraft:stick\", \"text\": \"x\", \"count\": " + count + "}]}").isEmpty(), count);
			assertEquals(1, problems.size(), count);
		}
	}

	@Test
	void badIdsAreRejected() {
		assertTrue(parse("""
			{"sources": [{"output": "Minecraft:Stick", "text": "x"}]}""").isEmpty());
		assertTrue(parse("""
			{"sources": [{"output": "minecraft:stick", "text": "x", "from": ["#"]}]}""").isEmpty());
		assertTrue(parse("""
			{"sources": [{"output": "minecraft:stick", "text": "x", "from": [1]}]}""").isEmpty());
		assertTrue(parse("""
			{"sources": [{"output": "minecraft:stick", "text": "x", "from": "minecraft:dirt"}]}""").isEmpty());
		assertTrue(parse("""
			{"sources": [{"output": "minecraft:stick", "text": "x", "id": "bad id"}]}""").isEmpty());
		assertEquals(5, problems.size());
	}

	@Test
	void requiresModFiltersThroughThePredicate() {
		String json = """
			{"sources": [{"output": "minecraft:stick", "text": "x", "requires_mod": "botania"}]}""";
		assertEquals(1, parse(json, Set.of("botania")).size());
		assertTrue(parse(json, Set.of("other")).isEmpty());
		assertTrue(problems.isEmpty(), "a missing mod is not a problem");
	}

	@Test
	void unknownKeysAreIgnored() {
		List<SourceEntry> entries = parse("""
			{"replace": false, "comment": 1, "sources": [{"output": "minecraft:stick", "text": "x", "note": [1, 2], "extra": {"a": 1}}]}""");
		assertEquals(1, entries.size());
		assertTrue(problems.isEmpty());
	}

	@Test
	void brokenFilesNeverThrow() {
		for (String json : List.of("", "{", "[]", "42", "null", "{}", "{\"sources\": 5}", "{\"sources\": [1, null, \"x\", []]}")) {
			problems.clear();
			assertTrue(parse(json).isEmpty(), json);
			assertFalse(problems.isEmpty(), json);
		}
	}

	@Test
	void duplicateIdInOneFileIsReported() {
		List<SourceEntry> entries = parse("""
			{"sources": [{"id": "a", "output": "minecraft:stick", "text": "x"}, {"id": "pack:a", "output": "minecraft:dirt", "text": "y"}]}""");
		assertEquals(1, entries.size());
		assertEquals(1, problems.size());
		assertTrue(problems.get(0).contains("duplicate"), problems.get(0));
	}

	@Test
	void defaultIdStripsFolderAndExtension() {
		assertEquals("emi_info:vanilla/3", SourceParser.defaultId("emi_info:emi_sources/vanilla.json", 3));
		assertEquals("a:b/c/0", SourceParser.defaultId("a:emi_sources/b/c.json", 0));
	}

	@Test
	void percentText() {
		assertEquals("25%", SourceText.percent(0.25));
		assertEquals("100%", SourceText.percent(1));
		assertEquals("2.5%", SourceText.percent(0.025));
		assertEquals("33.3%", SourceText.percent(1.0 / 3));
		assertEquals("0%", SourceText.percent(0));
	}
}
