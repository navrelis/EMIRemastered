package dev.emi.remastered.info.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvTest {

	@Test
	void plainValuesStayUnquoted() {
		assertEquals("minecraft:stone", Csv.escape("minecraft:stone"));
		assertEquals("", Csv.escape(""));
		assertEquals("", Csv.escape(null));
	}

	@Test
	void commaIsQuoted() {
		assertEquals("\"Stone, Smooth\"", Csv.escape("Stone, Smooth"));
	}

	@Test
	void quotesAreDoubledAndQuoted() {
		assertEquals("\"The \"\"Best\"\" Sword\"", Csv.escape("The \"Best\" Sword"));
		assertEquals("\"\"\"\"", Csv.escape("\""));
	}

	@Test
	void lineBreaksAreQuoted() {
		assertEquals("\"a\nb\"", Csv.escape("a\nb"));
		assertEquals("\"a\r\nb\"", Csv.escape("a\r\nb"));
		assertEquals("\"a\rb\"", Csv.escape("a\rb"));
	}

	@Test
	void edgeSpacesAreQuoted() {
		assertEquals("\" a\"", Csv.escape(" a"));
		assertEquals("\"a \"", Csv.escape("a "));
		assertEquals("a b", Csv.escape("a b"));
	}

	@Test
	void rowJoinsEscapedCellsWithCommas() {
		assertEquals("a,\"b,c\",,\"d\"\"e\"", Csv.row(Arrays.asList("a", "b,c", null, "d\"e")));
	}

	@Test
	void tableEndsEveryLineWithCrLf() {
		String csv = Csv.table(List.of("id", "name"), List.of(List.of("1", "Iron, Ingot"), List.of("2", "Gold")));
		assertEquals("id,name\r\n1,\"Iron, Ingot\"\r\n2,Gold\r\n", csv);
	}

	@Test
	void unicodeIsKeptAsIs() {
		assertEquals("Eisenbarren äöüß", Csv.escape("Eisenbarren äöüß"));
	}
}
