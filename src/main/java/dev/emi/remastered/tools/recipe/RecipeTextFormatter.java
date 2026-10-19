package dev.emi.remastered.tools.recipe;

import java.util.List;
import java.util.Locale;

import dev.emi.remastered.tools.recipe.RecipeSnapshot.Kind;
import dev.emi.remastered.tools.recipe.RecipeSnapshot.Slot;

/**
 * Renders a {@link RecipeSnapshot} as readable text:
 * <pre>
 * Smelting (emi:smelting)
 * Recipe id: minecraft:iron_ingot_from_smelting_raw_iron
 * Inputs:
 *   - 1x Raw Iron [minecraft:raw_iron]
 * Outputs:
 *   - 1x Iron Ingot [minecraft:iron_ingot]
 * Catalysts:
 *   - 1x Furnace [minecraft:furnace]
 * </pre>
 * The words come from {@link Labels} (lang keys on the client). Pure logic, no Minecraft classes.
 */
public final class RecipeTextFormatter {
	/** Alternatives listed per ingredient before "and N more". */
	public static final int MAX_OPTIONS = 6;

	private RecipeTextFormatter() {
	}

	/**
	 * Words used in the text. {@code id}, {@code anyOf}, {@code more}, {@code chance}, {@code tagSize} and
	 * {@code emptySlots} are {@link String#format} patterns.
	 *
	 * @param id         one argument: recipe id
	 * @param anyOf      one argument: the alternatives
	 * @param more       one argument: number of alternatives not listed
	 * @param chance     one argument: percent
	 * @param tagSize    one argument: number of entries of the tag
	 * @param emptySlots one argument: number of empty slots left out
	 */
	public record Labels(String id, String inputs, String outputs, String catalysts, String anyOf, String more,
			String chance, String tagSize, String emptySlots) {

		public static final Labels ENGLISH = new Labels("Recipe id: %s", "Inputs:", "Outputs:", "Catalysts:",
			"any of %s", "and %s more", "%s%% chance", "%s entries", "%s empty slots left out");
	}

	public static String format(RecipeSnapshot recipe, Labels labels) {
		StringBuilder out = new StringBuilder();
		out.append(recipe.categoryName());
		if (!recipe.categoryName().equals(recipe.categoryId())) {
			out.append(" (").append(recipe.categoryId()).append(')');
		}
		out.append('\n');
		if (recipe.id() != null) {
			out.append(String.format(labels.id(), recipe.id())).append('\n');
		}
		section(out, labels.inputs(), recipe.inputs(), labels);
		section(out, labels.outputs(), recipe.outputs(), labels);
		section(out, labels.catalysts(), recipe.catalysts(), labels);
		return out.toString().stripTrailing();
	}

	private static void section(StringBuilder out, String title, List<Slot> slots, Labels labels) {
		int empty = 0;
		StringBuilder lines = new StringBuilder();
		for (Slot slot : slots) {
			if (slot.kind() == Kind.EMPTY) {
				empty++;
				continue;
			}
			lines.append("  - ").append(slot(slot, labels)).append('\n');
		}
		if (lines.length() == 0 && empty == 0) {
			return;
		}
		out.append(title).append('\n').append(lines);
		if (empty > 0) {
			out.append("  (").append(String.format(labels.emptySlots(), empty)).append(")\n");
		}
	}

	/** One slot as a line without the leading dash. */
	public static String slot(Slot slot, Labels labels) {
		StringBuilder out = new StringBuilder();
		out.append(amount(slot)).append(' ');
		switch (slot.kind()) {
			case STACK -> out.append(named(slot.name(), slot.id()));
			case TAG -> {
				out.append(named(slot.name(), "#" + slot.id()));
				out.append(" (").append(String.format(labels.tagSize(), slot.size())).append(')');
			}
			case ANY_OF -> {
				StringBuilder options = new StringBuilder();
				int shown = Math.min(MAX_OPTIONS, slot.options().size());
				for (int i = 0; i < shown; i++) {
					Slot option = slot.options().get(i);
					if (i > 0) {
						options.append(", ");
					}
					options.append(named(option.name(), option.id()));
				}
				if (slot.options().size() > shown) {
					options.append(", ").append(String.format(labels.more(), slot.options().size() - shown));
				}
				out.append(String.format(labels.anyOf(), options));
			}
			default -> {
			}
		}
		if (slot.components() != null && !slot.components().isEmpty()) {
			out.append(' ').append(slot.components());
		}
		if (slot.chance() < 1) {
			out.append(", ").append(String.format(labels.chance(), percent(slot.chance())));
		}
		return out.toString();
	}

	private static String named(String name, String id) {
		if (name == null || name.isEmpty() || name.equals(id)) {
			return id;
		}
		return name + " [" + id + "]";
	}

	/** {@code 3x} for items, {@code 250 mB} for fluids. */
	public static String amount(Slot slot) {
		if (slot.isFluid()) {
			return millibuckets(slot.amount()) + " mB";
		}
		return slot.amount() + "x";
	}

	static String millibuckets(long droplets) {
		if (droplets % RecipeSnapshot.DROPLETS_PER_MB == 0) {
			return Long.toString(droplets / RecipeSnapshot.DROPLETS_PER_MB);
		}
		return String.format(Locale.ROOT, "%.1f", (double) droplets / RecipeSnapshot.DROPLETS_PER_MB);
	}

	private static String percent(float chance) {
		double percent = chance * 100.0;
		if (Math.abs(percent - Math.rint(percent)) < 0.005) {
			return Long.toString(Math.round(percent));
		}
		return String.format(Locale.ROOT, "%.1f", percent);
	}
}
