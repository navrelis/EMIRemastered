package dev.emi.remastered.letsdo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.letsdo.bakery.CakeCuts;

class LetsDoContentTest {

	@Test
	void everySubModuleClassExistsAndImplementsTheInterface() throws ClassNotFoundException {
		Set<String> mods = new HashSet<>();
		for (LetsDoContent.Entry entry : LetsDoContent.SUB_MODULES) {
			assertTrue(mods.add(entry.modId()), "duplicate mod " + entry.modId());
			// no initialisation: the classes reference the Let's Do mods, which are not on the test class path
			Class<?> type = Class.forName(entry.className(), false, LetsDoContentTest.class.getClassLoader());
			assertTrue(LetsDoSubModule.class.isAssignableFrom(type), entry.className());
		}
		assertEquals(Set.of("vinery", "meadow", "herbalbrews", "beachparty", "bakery"), mods);
	}

	@Test
	void nativePluginsAreListedByClassPath() {
		for (LetsDoContent.NativePlugin plugin : LetsDoContent.NATIVE_PLUGINS) {
			assertTrue(plugin.pluginClass().endsWith(".class"), plugin.modId());
			assertTrue(plugin.pluginClass().startsWith("net/satisfy/"), plugin.modId());
		}
	}

	@Test
	void cakeCutTableIsConsistent() {
		List<CakeCuts.Cut> cuts = CakeCuts.CUTS;
		assertEquals(cuts.size(), cuts.stream().map(CakeCuts.Cut::whole).distinct().count());
		assertEquals(cuts.size(), cuts.stream().map(CakeCuts.Cut::slice).distinct().count());
		for (CakeCuts.Cut cut : cuts) {
			assertTrue(cut.whole().matches("[a-z0-9_]+"), cut.whole());
			assertTrue(cut.slice().matches("[a-z0-9_]+"), cut.slice());
		}
		assertEquals(4, CakeCuts.SLICES);
	}
}
