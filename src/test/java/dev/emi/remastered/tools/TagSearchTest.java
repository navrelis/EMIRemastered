package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.tools.tags.TagCatalog;
import dev.emi.remastered.tools.tags.TagEntry;
import dev.emi.remastered.tools.tags.TagFilter;
import dev.emi.remastered.tools.tags.TagFilter.MemberRef;
import dev.emi.remastered.tools.tags.TagSearch;

class TagSearchTest {
	private static final TagEntry LOGS = new TagEntry("minecraft:item", "minecraft:logs",
		List.of("minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log"));
	private static final TagEntry LOGS_BURN = new TagEntry("minecraft:item", "minecraft:logs_that_burn",
		List.of("minecraft:oak_log", "minecraft:birch_log"));
	private static final TagEntry PLANKS = new TagEntry("minecraft:item", "minecraft:planks",
		List.of("minecraft:oak_planks", "minecraft:birch_planks"));
	private static final TagEntry OAK_LOGS = new TagEntry("minecraft:item", "minecraft:oak_logs", List.of("minecraft:oak_log"));
	private static final TagEntry BLOCK_LOGS = new TagEntry("minecraft:block", "minecraft:logs",
		List.of("minecraft:oak_log", "minecraft:stripped_oak_log"));
	private static final TagEntry C_ORES = new TagEntry("minecraft:item", "c:ores", List.of("minecraft:iron_ore"));
	private static final TagEntry BIOME = new TagEntry("minecraft:worldgen/biome", "minecraft:is_forest",
		List.of("minecraft:forest", "minecraft:birch_forest"));
	private static final TagEntry ZOMBIES = new TagEntry("minecraft:entity_type", "minecraft:zombies",
		List.of("minecraft:zombie", "minecraft:husk"));

	private static final TagCatalog CATALOG = new TagCatalog(
		List.of(PLANKS, BIOME, OAK_LOGS, ZOMBIES, BLOCK_LOGS, LOGS_BURN, C_ORES, LOGS));

	private static List<String> ids(List<TagEntry> list) {
		return list.stream().map(e -> e.registry().replace("minecraft:", "") + "/" + e.id()).toList();
	}

	private static List<TagEntry> search(String query) {
		return TagSearch.filter(CATALOG.entries(), new TagFilter(query, null, null));
	}

	@Test
	void catalogSortsRegistriesItemsBlocksFluidsEntitiesThenAlphabetical() {
		assertEquals(List.of("minecraft:item", "minecraft:block", "minecraft:entity_type", "minecraft:worldgen/biome"),
			CATALOG.registries());
		assertEquals(List.of("item/c:ores", "item/minecraft:logs", "item/minecraft:logs_that_burn", "item/minecraft:oak_logs",
			"item/minecraft:planks", "block/minecraft:logs", "entity_type/minecraft:zombies", "worldgen/biome/minecraft:is_forest"),
			ids(CATALOG.entries()));
		assertEquals(5, CATALOG.count("minecraft:item"));
		assertEquals(8, CATALOG.count(null));
	}

	@Test
	void emptyQueryKeepsCatalogOrder() {
		assertEquals(ids(CATALOG.entries()), ids(search("")));
		assertEquals(ids(CATALOG.entries()), ids(search("   ")));
	}

	@Test
	void queryMatchesTagIdAndMemberId() {
		// by tag id: logs, logs_that_burn, oak_logs (items) and the block tag
		assertEquals(List.of("item/minecraft:logs", "block/minecraft:logs", "item/minecraft:logs_that_burn", "item/minecraft:oak_logs"),
			ids(search("logs")).subList(0, 4));
		// by member id: only tags with a husk
		assertEquals(List.of("entity_type/minecraft:zombies"), ids(search("husk")));
		// namespace of the tag id
		assertEquals(List.of("item/c:ores"), ids(search("c:ores")));
	}

	@Test
	void queryIsCaseInsensitiveAndAllTokensMustMatch() {
		assertEquals(List.of("item/minecraft:planks"), ids(search("PLANKS")));
		assertEquals(List.of("item/minecraft:logs", "item/minecraft:logs_that_burn"), ids(search("logs birch")));
		assertEquals(List.of(), ids(search("logs husk")));
	}

	@Test
	void prefixesRestrictWhereATokenMatches() {
		// #: tag id only, "oak" is a member of many tags but only in the id of one
		assertEquals(List.of("item/minecraft:oak_logs"), ids(search("#oak")));
		// @: members only, "logs" is no member id
		assertEquals(List.of(), ids(search("@logs")));
		assertEquals(5, search("@oak").size());
		// a lone prefix matches everything
		assertEquals(8, search("#").size());
	}

	@Test
	void rankingExactBeforePrefixBeforeContainsBeforeMember() {
		TagEntry exact = new TagEntry("minecraft:item", "x:log", List.of("x:a"));
		TagEntry prefix = new TagEntry("minecraft:item", "x:log_pile", List.of("x:a"));
		TagEntry contains = new TagEntry("minecraft:item", "x:big_log_pile", List.of("x:a"));
		TagEntry member = new TagEntry("minecraft:item", "x:zzz", List.of("x:log_holder"));
		TagCatalog catalog = new TagCatalog(List.of(member, contains, prefix, exact));
		assertEquals(List.of("x:log", "x:log_pile", "x:big_log_pile", "x:zzz"),
			TagSearch.filter(catalog.entries(), new TagFilter("log", null, null)).stream().map(TagEntry::id).toList());
	}

	@Test
	void registryFilter() {
		assertEquals(List.of("block/minecraft:logs"),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("logs", "minecraft:block", null))));
		assertEquals(List.of("worldgen/biome/minecraft:is_forest"),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("", "minecraft:worldgen/biome", null))));
		assertEquals(List.of(), ids(TagSearch.filter(CATALOG.entries(), new TagFilter("", "minecraft:fluid", null))));
	}

	@Test
	void memberFilterAndReverseLookup() {
		assertEquals(List.of("item/minecraft:logs", "item/minecraft:logs_that_burn", "item/minecraft:oak_logs", "block/minecraft:logs"),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("", null, new MemberRef(null, "minecraft:oak_log")))));
		assertEquals(List.of("block/minecraft:logs"),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("", null, new MemberRef("minecraft:block", "minecraft:oak_log")))));
		assertEquals(List.of("item/minecraft:logs_that_burn"),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("burn", null, new MemberRef(null, "minecraft:oak_log")))));
		assertEquals(ids(CATALOG.tagsContaining(null, "minecraft:oak_log")),
			ids(TagSearch.filter(CATALOG.entries(), new TagFilter("", null, new MemberRef(null, "minecraft:oak_log")))));
		assertEquals(List.of(), ids(CATALOG.tagsContaining("minecraft:fluid", "minecraft:oak_log")));
		assertEquals(List.of("item/minecraft:planks"), ids(CATALOG.tagsContaining("minecraft:item", "minecraft:birch_planks")));
	}

	@Test
	void findLooksUpByRegistryAndId() {
		assertNotNull(CATALOG.find("minecraft:block", "minecraft:logs"));
		assertEquals("minecraft:block", CATALOG.find("minecraft:block", "minecraft:logs").registry());
		assertEquals("minecraft:item", CATALOG.find(null, "minecraft:logs").registry());
		assertNull(CATALOG.find("minecraft:fluid", "minecraft:logs"));
		assertTrue(LOGS.hasMember("minecraft:oak_log"));
		assertEquals(3, LOGS.size());
	}
}
