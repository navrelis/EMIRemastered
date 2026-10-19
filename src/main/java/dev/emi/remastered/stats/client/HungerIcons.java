package dev.emi.remastered.stats.client;

import dev.emi.remastered.stats.core.StatsMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Draws the hunger shanks and a saturation bar in the style of the vanilla HUD. */
public final class HungerIcons {
	private static final Identifier EMPTY = Identifier.ofVanilla("hud/food_empty");
	private static final Identifier HALF = Identifier.ofVanilla("hud/food_half");
	private static final Identifier FULL = Identifier.ofVanilla("hud/food_full");
	/** Distance between two shanks (they overlap by one pixel, like on the HUD). */
	private static final int STEP = 8;
	private static final int SIZE = 9;
	/** Width of the shank row; the saturation bar uses the same width. */
	public static final int WIDTH = 9 * STEP + SIZE;
	public static final int HEIGHT = SIZE;
	private static final int SATURATION = 0xFFE3A41E;

	private HungerIcons() {
	}

	/** Ten shanks for a food level (levels above 20 look like 20). */
	public static void drawShanks(DrawContext draw, int x, int y, int nutrition) {
		int[] shanks = StatsMath.shanks(nutrition);
		for (int i = 0; i < 10; i++) {
			draw.drawGuiTexture(EMPTY, x + i * STEP, y, SIZE, SIZE);
		}
		for (int i = 0; i < shanks[0]; i++) {
			draw.drawGuiTexture(FULL, x + i * STEP, y, SIZE, SIZE);
		}
		if (shanks[1] > 0) {
			draw.drawGuiTexture(HALF, x + shanks[0] * STEP, y, SIZE, SIZE);
		}
	}

	/** A thin bar of {@link #WIDTH} x 5 filled by {@code fraction}. */
	public static void drawBar(DrawContext draw, int x, int y, float fraction) {
		draw.fill(x, y, x + WIDTH, y + 5, 0xFF373737);
		draw.fill(x + 1, y + 1, x + WIDTH - 1, y + 4, 0xFF1B1B1B);
		int filled = Math.round((WIDTH - 2) * Math.max(0, Math.min(1, fraction)));
		if (filled > 0) {
			draw.fill(x + 1, y + 1, x + 1 + filled, y + 4, SATURATION);
		}
	}
}
