package dev.emi.remastered.info.api;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.text.Text;

/**
 * One section of the Item Info screen: a title line with an optional icon, followed by building blocks laid out from
 * top to bottom. The screen renders them in EMI's style; providers never draw directly (except through
 * {@link #element}). All methods return {@code this} for chaining, except {@link #entry}, which returns the new entry.
 */
public interface InfoSection {

	/** Where a section is shown. The screen shows the kinds in this order, each under its own heading. */
	enum Kind {
		/** "How to get": recipes, loot, world generation, trades... Counts as a source unless {@code source(false)}. */
		OBTAIN,
		/** "Used for": recipes consuming the stack, workstation for, trades buying it... */
		USE,
		/** "Details": properties such as fuel value, composting, food, tool stats, effects, advancements. */
		DETAILS
	}

	/** Icon drawn left of the title, 16x16. Any {@link EmiIngredient}, EMI recipe category or texture works. */
	InfoSection icon(EmiRenderable icon);

	/**
	 * Only meaningful for {@link Kind#OBTAIN}: whether the section is a real way to obtain the stack (default true).
	 * Sections that only turn the stack into itself (repairing) or only describe it say false, so the
	 * "no known source" marker still appears.
	 */
	InfoSection source(boolean source);

	/** A paragraph of text, wrapped to the available width. Uncoloured text is drawn in EMI's dark grey. */
	InfoSection text(Text text);

	/** A paragraph of text with a tooltip. */
	InfoSection text(Text text, List<Text> tooltip);

	/**
	 * Rows of slots (wrapping as needed), each drawn like a slot of EMI's recipe screen: the amount
	 * ({@link EmiIngredient#getAmount()}) is shown on the slot and a chance other than 1
	 * ({@link EmiIngredient#getChance()}) gives the slot EMI's chance background and tooltip line. Players can use
	 * EMI's keys on the slots (recipes, uses, favourite, item info).
	 */
	InfoSection stacks(List<? extends EmiIngredient> stacks);

	/** Like {@link #stacks(List)}, with a label line above the slots. */
	InfoSection stacks(Text label, List<? extends EmiIngredient> stacks);

	/** A clickable text line that opens {@code link}. */
	InfoSection link(Text text, InfoLink link);

	/**
	 * A labelled bar for a chance or a ratio.
	 *
	 * @param label    text left of the bar
	 * @param progress fill from 0 to 1 (clamped)
	 * @param color    fill colour {@code 0xRRGGBB}
	 * @param value    text right of the bar (e.g. "65%"), or null
	 */
	InfoSection bar(Text label, float progress, int color, @Nullable Text value);

	/**
	 * Adds a compact row: icon, label, a few example stacks, an optional detail on the right (e.g. a count) and an
	 * optional link that the row opens when clicked. The built-in provider lists EMI recipe categories this way.
	 *
	 * @return the new entry, for filling
	 */
	InfoEntry entry(EmiRenderable icon, Text label);

	/** Advanced hook: an element that renders itself. Prefer the building blocks above. */
	InfoSection element(InfoElement element);
}
