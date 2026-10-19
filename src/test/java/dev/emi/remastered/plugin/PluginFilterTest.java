package dev.emi.remastered.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class PluginFilterTest {

	@Test
	void parseAcceptsCommasAndWhitespaceAndDropsCore() {
		assertEquals(Set.of("jeed", "ali", "emi_loot"), PluginFilter.parse(" jeed,ali  emi_loot, emi ,, "));
		assertEquals(Set.of(), PluginFilter.parse(""));
		assertEquals(Set.of(), PluginFilter.parse(null));
	}

	@Test
	void formatIsSortedAndRoundTrips() {
		String text = PluginFilter.format(List.of("jeed", "ali", "emi", "ali"));
		assertEquals("ali, jeed", text);
		assertEquals(Set.of("ali", "jeed"), PluginFilter.parse(text));
	}

	@Test
	void coreCanNeverBeDisabled() {
		assertFalse(PluginFilter.canDisable("emi"));
		assertFalse(PluginFilter.isDisabled("emi", Set.of("emi")));
		assertEquals("", PluginFilter.withDisabled("", "emi", true));
	}

	@Test
	void withDisabledTogglesIds() {
		String s = PluginFilter.withDisabled("", "jeed", true);
		s = PluginFilter.withDisabled(s, "ali", true);
		assertEquals("ali, jeed", s);
		s = PluginFilter.withDisabled(s, "jeed", false);
		assertEquals("ali", s);
	}

	@Test
	void removeDisabledKeepsOrderAndReturnsRemoved() {
		record P(String id) {
		}
		List<P> plugins = new ArrayList<>(List.of(new P("emi"), new P("emi_loot"), new P("jeed"), new P("ali"), new P("curios")));
		List<P> removed = PluginFilter.removeDisabled(plugins, P::id, Set.of("jeed", "curios", "emi", "unknown"));
		Function<List<P>, List<String>> ids = l -> l.stream().map(P::id).toList();
		assertEquals(List.of("emi", "emi_loot", "ali"), ids.apply(plugins));
		assertEquals(List.of("jeed", "curios"), ids.apply(removed));
		assertTrue(PluginFilter.removeDisabled(plugins, P::id, Set.of()).isEmpty());
	}
}
