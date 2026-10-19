package dev.emi.remastered.letsdo;

import java.util.List;
import java.util.function.BiConsumer;

import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The drawings of the Let's Do categories (ported from the former per-recipe classes of Let's Do EMI Compat). They use
 * the GUI textures of the Let's Do mods by identifier only, so this class loads without any of those mods.
 */
public final class LetsDoLayouts {
	private static final Identifier RIGHT_CLICK = Identifier.ofVanilla("textures/gui/sprites/toast/right_click.png");
	private static final Identifier CHEESE_FORM_GUI = Identifier.of("meadow", "textures/gui/cheese_form_gui.png");
	private static final Identifier COOKING_CAULDRON_GUI = Identifier.of("meadow", "textures/gui/cooking_cauldron_gui.png");
	private static final Identifier CAULDRON_GUI = Identifier.of("herbalbrews", "textures/gui/cauldron.png");
	private static final Identifier TEA_KETTLE_GUI = Identifier.of("herbalbrews", "textures/gui/tea_kettle.png");
	private static final Identifier MINI_FRIDGE_GUI = Identifier.of("beachparty", "textures/gui/freezer_gui.png");
	private static final Identifier PALM_BAR_GUI = Identifier.of("beachparty", "textures/gui/palm_bar_gui.png");

	/** Right click with an item on a block: input, click symbol, block (catalyst), arrow, result. */
	public static final Layout USE_ON_BLOCK = new Layout(118, 28, (w, r) -> {
		w.addSlot(r.input(0), 5, 5);
		w.addTexture(RIGHT_CLICK, 25, 3, 20, 20, 0, 0, 20, 20, 20, 20);
		w.addSlot(r.catalyst(0), 45, 5).catalyst(true);
		w.addFillingArrow(65, 5, 5000);
		w.addSlot(r.output(0), 95, 5).recipeContext(r);
		hoverText(w, r, 25, 3, 68, 22);
	});

	/** Right click with a tool (catalyst) on a block that is the input: tool, click symbol, input, arrow, result. */
	public static final Layout TOOL_ON_INPUT = new Layout(118, 28, (w, r) -> {
		w.addSlot(r.catalyst(0), 5, 5).catalyst(true);
		w.addTexture(RIGHT_CLICK, 25, 3, 20, 20, 0, 0, 20, 20, 20, 20);
		w.addSlot(r.input(0), 45, 5);
		w.addFillingArrow(65, 5, 5000);
		w.addSlot(r.output(0), 95, 5).recipeContext(r);
		hoverText(w, r, 25, 3, 68, 22);
	});

	/** Right click with an item on a block that changes the block: input, click symbol, arrow, block. */
	public static final Layout FILL_BLOCK = new Layout(98, 28, (w, r) -> {
		w.addSlot(r.input(0), 5, 5);
		w.addTexture(RIGHT_CLICK, 25, 3, 20, 20, 0, 0, 20, 20, 20, 20);
		w.addFillingArrow(45, 5, 5000);
		w.addSlot(r.catalyst(0), 75, 5).catalyst(true);
		hoverText(w, r, 25, 3, 48, 22);
	});

	public static final Layout CHEESE_FORM = new Layout(124, 60, (w, r) -> {
		w.addTexture(CHEESE_FORM_GUI, 0, 0, 124, 60, 26, 6);
		w.addAnimatedTexture(CHEESE_FORM_GUI, 51, 30, 26, 10, 175, 4, 5000, true, false, false);
		w.addAnimatedTexture(CHEESE_FORM_GUI, 50, 2, 26, 32, 175, 22, 5000, false, true, false);
		bare(w, r, 0, 6, 26);
		bare(w, r, 1, 24, 26);
		output(w, r, 96, 26);
	});

	public static final Layout COOKING_CAULDRON = new Layout(124, 60, (w, r) -> {
		w.addTexture(COOKING_CAULDRON_GUI, 0, 0, 124, 60, 7, 6);
		w.addAnimatedTexture(COOKING_CAULDRON_GUI, 64, 20, 24, 17, 176, 14, 5000, true, false, false);
		w.addAnimatedTexture(COOKING_CAULDRON_GUI, 102, 47, 16, 14, 176, 0, 5000, false, true, false);
		bare(w, r, 0, 3, 10);
		bare(w, r, 1, 21, 10);
		bare(w, r, 2, 39, 10);
		bare(w, r, 3, 3, 28);
		bare(w, r, 4, 21, 28);
		bare(w, r, 5, 39, 28);
		output(w, r, 97, 19);
	});

	public static final Layout WOODCUTTING = new Layout(124, 60, (w, r) -> {
		w.addFillingArrow(50, 21, 5000);
		if (r.inputCount() > 0) {
			w.addSlot(r.input(0), 23, 21);
		}
		w.addSlot(r.output(0), 83, 21).recipeContext(r);
	});

	public static final Layout CAULDRON_BREWING = new Layout(118, 68, (w, r) -> {
		w.addTexture(CAULDRON_GUI, 0, 0, 118, 68, 52, 11);
		w.addAnimatedTexture(CAULDRON_GUI, 58, 58, 18, 4, 176, 29, 5000, true, false, false);
		w.addAnimatedTexture(CAULDRON_GUI, 62, 27, 11, 29, 176, 0, 5000, false, true, false);
		bare(w, r, 0, 4, 4);
		bare(w, r, 1, 26, 10);
		bare(w, r, 2, 48, 4);
		w.addSlot(r.extra(0), 95, 30).drawBack(false);
		output(w, r, 26, 46);
	});

	/** Extras: container, water, heat. */
	public static final Layout TEA_KETTLE = new Layout(157, 69, (w, r) -> {
		w.addTexture(TEA_KETTLE_GUI, 0, 0, 157, 69, 10, 9);
		w.addAnimatedTexture(TEA_KETTLE_GUI, 131, 7, 8, 43, 183, 31, 5000, false, true, true);
		w.addAnimatedTexture(TEA_KETTLE_GUI, 146, 7, 5, 43, 176, 31, 5000, false, true, true);
		w.addAnimatedTexture(TEA_KETTLE_GUI, 141, 53, 14, 14, 176, 0, 5000, false, true, false);
		w.addAnimatedTexture(TEA_KETTLE_GUI, 44, 12, 24, 17, 176, 14, 5000, true, false, false);
		bare(w, r, 0, 2, 2);
		bare(w, r, 1, 20, 2);
		bare(w, r, 2, 2, 20);
		bare(w, r, 3, 20, 20);
		w.addSlot(r.extra(0), 20, 42).drawBack(false);
		w.addSlot(r.extra(1), 107, 33).drawBack(false);
		w.addSlot(r.extra(2), 84, 48).drawBack(false);
		output(w, r, 80, 12);
	});

	public static final Layout MINI_FRIDGE = new Layout(88, 34, (w, r) -> {
		w.addTexture(MINI_FRIDGE_GUI, 0, 0, 88, 34, 52, 26);
		w.addAnimatedTexture(MINI_FRIDGE_GUI, 27, 8, 24, 17, 176, 14, 5000, true, false, false);
		bare(w, r, 0, 3, 8);
		output(w, r, 63, 8);
	});

	public static final Layout PALM_BAR = new Layout(106, 42, (w, r) -> {
		w.addTexture(PALM_BAR_GUI, 0, 0, 106, 42, 34, 21);
		w.addAnimatedTexture(PALM_BAR_GUI, 45, 13, 24, 17, 176, 14, 5000, true, false, false);
		bare(w, r, 0, 3, 3);
		bare(w, r, 1, 21, 3);
		bare(w, r, 2, 3, 21);
		bare(w, r, 3, 21, 21);
		output(w, r, 81, 13);
	});

	private LetsDoLayouts() {
	}

	/** An input slot without its own background (the texture draws it), skipped if the recipe has fewer inputs. */
	private static void bare(WidgetHolder w, LetsDoRecipe r, int index, int x, int y) {
		if (index < r.inputCount()) {
			w.addSlot(r.input(index), x, y).drawBack(false);
		}
	}

	private static void output(WidgetHolder w, LetsDoRecipe r, int x, int y) {
		w.addSlot(r.output(0), x, y).drawBack(false).recipeContext(r);
	}

	private static void hoverText(WidgetHolder w, LetsDoRecipe r, int x, int y, int width, int height) {
		List<Text> lines = r.tooltipLines();
		if (!lines.isEmpty()) {
			w.addTooltipText(lines, x, y, width, height);
		}
	}

	/** Size of a recipe display and how it draws a {@link LetsDoRecipe}. */
	public record Layout(int width, int height, BiConsumer<WidgetHolder, LetsDoRecipe> body) {

		void build(WidgetHolder widgets, LetsDoRecipe recipe) {
			body.accept(widgets, recipe);
		}
	}
}
