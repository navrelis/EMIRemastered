package dev.emi.remastered.distractionfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class ScreenFilterTest {
	static class BaseScreen {
	}

	static class ChildScreen extends BaseScreen {
	}

	private static final UnaryOperator<String> IDENTITY = s -> s;

	@Test
	void parseSplitsTrimsAndDropsEmpty() {
		assertEquals(List.of(), ScreenFilter.parse(null));
		assertEquals(List.of(), ScreenFilter.parse(" , ,"));
		assertEquals(List.of("A", "b.C", "D"), ScreenFilter.parse(" A, b.C ,,D "));
	}

	@Test
	void simpleNamesIgnoreCaseAndCheckSuperclasses() {
		List<String> entries = ScreenFilter.parse("childscreen");
		assertTrue(ScreenFilter.matches(entries, ChildScreen.class, IDENTITY));
		assertFalse(ScreenFilter.matches(entries, BaseScreen.class, IDENTITY));
		assertTrue(ScreenFilter.matches(ScreenFilter.parse("BaseScreen"), ChildScreen.class, IDENTITY));
		assertFalse(ScreenFilter.matches(List.of(), ChildScreen.class, IDENTITY));
		assertFalse(ScreenFilter.matches(ScreenFilter.parse("Object"), ChildScreen.class, IDENTITY));
	}

	@Test
	void fullNamesUseTheMapper() {
		String name = ChildScreen.class.getName();
		assertTrue(ScreenFilter.matches(List.of(name), ChildScreen.class, IDENTITY));
		assertFalse(ScreenFilter.matches(List.of("some.Other"), ChildScreen.class, IDENTITY));
		assertTrue(ScreenFilter.matches(List.of("named.Child"), ChildScreen.class, s -> s.equals("named.Child") ? name : s));
		// a dotted entry never matches by simple name
		assertFalse(ScreenFilter.matches(List.of("x.ChildScreen"), ChildScreen.class, IDENTITY));
	}
}
