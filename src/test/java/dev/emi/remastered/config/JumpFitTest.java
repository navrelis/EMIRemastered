package dev.emi.remastered.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JumpFitTest {

	@Test
	void everythingFits() {
		// 11 EMI jumps + 13 module entries = 24 * 16 = 384
		assertEquals(13, JumpFit.fittingModuleJumps(384, 11, 13, 16));
		assertEquals(13, JumpFit.fittingModuleJumps(500, 11, 13, 16));
	}

	@Test
	void keepsAsManyAsFit() {
		// space for 16 entries: 11 EMI + 5 module entries
		assertEquals(5, JumpFit.fittingModuleJumps(16 * 16, 11, 13, 16));
		assertEquals(5, JumpFit.fittingModuleJumps(16 * 16 + 15, 11, 13, 16));
		assertEquals(12, JumpFit.fittingModuleJumps(24 * 16 - 1, 11, 13, 16));
	}

	@Test
	void keepsAtLeastTheModulesJump() {
		assertEquals(1, JumpFit.fittingModuleJumps(11 * 16, 11, 13, 16));
		assertEquals(1, JumpFit.fittingModuleJumps(50, 11, 13, 16));
		assertEquals(1, JumpFit.fittingModuleJumps(-10, 11, 13, 16));
	}

	@Test
	void nothingToKeep() {
		assertEquals(0, JumpFit.fittingModuleJumps(10, 11, 0, 16));
	}
}
