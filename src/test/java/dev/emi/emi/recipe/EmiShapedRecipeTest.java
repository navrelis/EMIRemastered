package dev.emi.emi.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmiShapedRecipeTest {

	private static boolean[] grid(int... occupiedSlots) {
		boolean[] g = new boolean[9];
		for (int s : occupiedSlots) {
			g[s] = true;
		}
		return g;
	}

	@Test
	void fullGridKeepsPaddedIndex() {
		boolean[] g = grid(0, 1, 2, 3, 4, 5, 6, 7, 8);
		for (int i = 0; i < 9; i++) {
			assertEquals(i, EmiShapedRecipe.remainderIndex(i, 3, g));
		}
	}

	@Test
	void trappedChestStyleRow() {
		// Two ingredients side by side in the top row: cropped input is 2x1
		boolean[] g = grid(0, 1);
		assertEquals(0, EmiShapedRecipe.remainderIndex(0, 3, g));
		assertEquals(1, EmiShapedRecipe.remainderIndex(1, 3, g));
	}

	@Test
	void croppedWidthIsUsedNotThree() {
		// 2x2 block in the bottom right: cropped input is 2x2
		boolean[] g = grid(4, 5, 7, 8);
		assertEquals(0, EmiShapedRecipe.remainderIndex(4, 3, g));
		assertEquals(1, EmiShapedRecipe.remainderIndex(5, 3, g));
		assertEquals(2, EmiShapedRecipe.remainderIndex(7, 3, g));
		assertEquals(3, EmiShapedRecipe.remainderIndex(8, 3, g));
	}

	@Test
	void singleStackAnywhereIsIndexZero() {
		for (int i = 0; i < 9; i++) {
			assertEquals(0, EmiShapedRecipe.remainderIndex(i, 3, grid(i)));
		}
	}

	@Test
	void verticalColumn() {
		boolean[] g = grid(1, 4, 7);
		assertEquals(0, EmiShapedRecipe.remainderIndex(1, 3, g));
		assertEquals(1, EmiShapedRecipe.remainderIndex(4, 3, g));
		assertEquals(2, EmiShapedRecipe.remainderIndex(7, 3, g));
	}

	@Test
	void emptyOrOutOfRangeSlotIsRejected() {
		boolean[] g = grid(0, 1);
		assertEquals(-1, EmiShapedRecipe.remainderIndex(2, 3, g));
		assertEquals(-1, EmiShapedRecipe.remainderIndex(-1, 3, g));
		assertEquals(-1, EmiShapedRecipe.remainderIndex(9, 3, g));
		assertEquals(-1, EmiShapedRecipe.remainderIndex(0, 0, g));
	}
}
