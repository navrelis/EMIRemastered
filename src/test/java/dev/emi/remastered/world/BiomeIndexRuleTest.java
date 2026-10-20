package dev.emi.remastered.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.world.data.BiomeIndexRule;

class BiomeIndexRuleTest {
	@Test
	void oresAddWhenModuleAndSettingAreOn() {
		assertTrue(BiomeIndexRule.oresAdd(true, true));
		assertFalse(BiomeIndexRule.oresAdd(true, false));
		assertFalse(BiomeIndexRule.oresAdd(false, true));
	}

	@Test
	void worldAddsWhenOresDoNot() {
		// ores off, or ores on with their setting off: the world module fills the gap
		assertTrue(BiomeIndexRule.worldAdds(true, true, false, true));
		assertTrue(BiomeIndexRule.worldAdds(true, true, true, false));
		assertTrue(BiomeIndexRule.worldAdds(true, true, false, false));
	}

	@Test
	void neverBothAndNeverAgainstTheWorldSettings() {
		for (int bits = 0; bits < 16; bits++) {
			boolean pages = (bits & 1) != 0;
			boolean setting = (bits & 2) != 0;
			boolean oresModule = (bits & 4) != 0;
			boolean oresSetting = (bits & 8) != 0;
			boolean world = BiomeIndexRule.worldAdds(pages, setting, oresModule, oresSetting);
			assertFalse(world && BiomeIndexRule.oresAdd(oresModule, oresSetting));
			if (world) {
				assertTrue(pages && setting);
			}
		}
		assertFalse(BiomeIndexRule.worldAdds(false, true, false, false));
		assertFalse(BiomeIndexRule.worldAdds(true, false, true, false));
	}
}
