package dev.emi.remastered.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Pure Java: file parsing, merge order and alpha handling of {@link TextColorTable}. */
class TextColorTableTest {
	private static boolean parse(TextColorTable.Builder b, String json, List<String> warnings) {
		return b.parseInto(new StringReader(json), warnings::add);
	}

	@Test
	void parsesValidFile() {
		List<String> warnings = new ArrayList<>();
		TextColorTable.Builder b = TextColorTable.builder();
		assertTrue(parse(b, "{\"_comment\": \"x\", \"replace\": {\"404040\": \"FFFFFF\", \"6A6A6A\": \"9FA5A6\"}}", warnings));
		TextColorTable t = b.build();
		assertTrue(warnings.isEmpty());
		assertFalse(t.isEmpty());
		assertEquals(2, t.size());
		assertEquals(0xFFFFFFFF, t.remap(0xFF404040));
		assertEquals(0xFF9FA5A6, t.remap(0xFF6A6A6A));
	}

	@Test
	void hashPrefixAndLowerCase() {
		TextColorTable.Builder b = TextColorTable.builder();
		assertTrue(parse(b, "{\"replace\": {\"#6a6a6a\": \"#9fa5a6\", \"AbCdEf\": \"010203\"}}", new ArrayList<>()));
		TextColorTable t = b.build();
		assertEquals(0xFF9FA5A6, t.remap(0xFF6A6A6A));
		assertEquals(0xFF010203, t.remap(0xFFABCDEF));
	}

	@Test
	void emptyReplaceAndMissingReplaceAreValidAndEmpty() {
		List<String> warnings = new ArrayList<>();
		TextColorTable.Builder b = TextColorTable.builder();
		assertTrue(parse(b, "{\"_comment\": \"Empty on purpose.\", \"replace\": {}}", warnings));
		assertTrue(parse(b, "{}", warnings));
		assertTrue(warnings.isEmpty());
		TextColorTable t = b.build();
		assertTrue(t.isEmpty());
		assertSame(TextColorTable.EMPTY, t);
	}

	@Test
	void invalidJsonReturnsFalseWithOneWarningAndLeavesTableUnchanged() {
		List<String> warnings = new ArrayList<>();
		TextColorTable.Builder b = TextColorTable.builder().put(0x404040, 0xFFFFFF);
		assertFalse(parse(b, "{\"replace\": {\"404040\": ", warnings));
		assertFalse(parse(b, "[1, 2]", warnings));
		assertFalse(parse(b, "{\"replace\": [\"404040\"]}", warnings));
		assertFalse(parse(b, "{\"replace\": \"x\"}", warnings));
		assertEquals(4, warnings.size());
		TextColorTable t = b.build();
		assertEquals(1, t.size());
		assertEquals(0xFFFFFFFF, t.remap(0xFF404040));
	}

	@Test
	void badEntriesAreSkippedWithOneWarning() {
		List<String> warnings = new ArrayList<>();
		TextColorTable.Builder b = TextColorTable.builder();
		assertTrue(parse(b, "{\"replace\": {\"404040\": \"FFFFFF\", \"4040\": \"FFFFFF\", \"zzzzzz\": \"FFFFFF\","
			+ " \"202020\": \"12345\", \"303030\": 5, \"505050\": \"#FFFFFFF\", \"606060\": \"AAAAAA\"}}", warnings));
		assertEquals(1, warnings.size());
		for (String bad : new String[] {"4040", "zzzzzz", "202020", "303030", "505050"}) {
			assertTrue(warnings.get(0).contains(bad), bad);
		}
		assertFalse(warnings.get(0).contains("404040"));
		TextColorTable t = b.build();
		assertEquals(2, t.size());
		assertEquals(0xFFFFFFFF, t.remap(0xFF404040));
		assertEquals(0xFFAAAAAA, t.remap(0xFF606060));
		assertEquals(0xFF202020, t.remap(0xFF202020));
	}

	@Test
	void laterPutWinsAcrossFiles() {
		TextColorTable.Builder b = TextColorTable.builder();
		assertTrue(parse(b, "{\"replace\": {\"404040\": \"111111\", \"202020\": \"222222\"}}", new ArrayList<>()));
		assertTrue(parse(b, "{\"replace\": {\"404040\": \"999999\"}}", new ArrayList<>()));
		b.put(0x202020, 0x333333);
		TextColorTable t = b.build();
		assertEquals(0xFF999999, t.remap(0xFF404040));
		assertEquals(0xFF333333, t.remap(0xFF202020));
	}

	@Test
	void remapKeepsAlphaIncludingZero() {
		TextColorTable t = TextColorTable.builder().put(0x404040, 0x112233).put(0x000008, 0xFFFFFF).build();
		assertEquals(0xFF112233, t.remap(0xFF404040));
		assertEquals(0x00112233, t.remap(0x00404040));
		assertEquals(0x80112233, t.remap(0x80404040));
		assertEquals(0x00FFFFFF, t.remap(0x00000008));
		assertEquals(0x112233, t.remap(0x404040));
	}

	@Test
	void whiteIsAValidTarget() {
		TextColorTable t = TextColorTable.builder().put(0x404040, 0xFFFFFF).build();
		assertEquals(0xFFFFFFFF, t.remap(0xFF404040));
		assertEquals(0x00FFFFFF, t.remap(0x00404040));
	}

	@Test
	void blackSourceIsMappable() {
		TextColorTable t = TextColorTable.builder().put(0x000000, 0xEEEEEE).build();
		assertEquals(0xFFEEEEEE, t.remap(0xFF000000));
		assertEquals(0x00EEEEEE, t.remap(0));
	}

	@Test
	void unmappedColourIsUnchanged() {
		TextColorTable t = TextColorTable.builder().put(0x404040, 0xFFFFFF).build();
		assertEquals(0xFF123456, t.remap(0xFF123456));
		assertEquals(0, TextColorTable.EMPTY.remap(0));
		assertEquals(0xFF404040, TextColorTable.EMPTY.remap(0xFF404040));
	}

	@Test
	void emptyTableIsEmpty() {
		assertTrue(TextColorTable.EMPTY.isEmpty());
		assertTrue(TextColorTable.builder().build().isEmpty());
		assertFalse(TextColorTable.builder().put(1, 2).build().isEmpty());
	}
}
