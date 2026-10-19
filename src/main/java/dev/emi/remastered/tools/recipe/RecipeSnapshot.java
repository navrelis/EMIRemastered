package dev.emi.remastered.tools.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Plain-data copy of an EMI recipe, made on the client and rendered by {@link RecipeTextFormatter} and
 * {@link RecipeJsonFormatter}. No Minecraft classes, so both formatters can be unit tested.
 *
 * @param id           recipe id, or null for recipes without one
 * @param categoryId   category id, e.g. {@code emi:smelting}
 * @param categoryName localized category name
 */
public record RecipeSnapshot(@Nullable String id, String categoryId, String categoryName, List<Slot> inputs,
		List<Slot> outputs, List<Slot> catalysts) {

	/** Droplets per millibucket (Fabric fluid unit: 81000 droplets are one bucket). */
	public static final long DROPLETS_PER_MB = 81;

	public enum Kind {
		/** An empty slot (shaped crafting holes). */
		EMPTY,
		/** One concrete stack. */
		STACK,
		/** A tag ingredient. */
		TAG,
		/** Several alternatives. */
		ANY_OF
	}

	/**
	 * One ingredient or output slot.
	 *
	 * @param type       what the id belongs to: {@code item}, {@code fluid}, the registry path of a tag, or a custom stack type
	 * @param id         stack id, or tag id without {@code #}; null for EMPTY and ANY_OF
	 * @param name       localized name, or null
	 * @param amount     item count, or droplets for fluids
	 * @param chance     1.0 when the slot is always produced or consumed
	 * @param components item component changes as a string, or null
	 * @param size       number of stacks a tag or ANY_OF stands for
	 * @param options    the alternatives of ANY_OF, otherwise empty
	 */
	public record Slot(Kind kind, String type, @Nullable String id, @Nullable String name, long amount, float chance,
			@Nullable String components, int size, List<Slot> options) {

		public static Slot empty() {
			return new Slot(Kind.EMPTY, "", null, null, 0, 1, null, 0, List.of());
		}

		public static Slot stack(String type, String id, @Nullable String name, long amount, float chance,
				@Nullable String components) {
			return new Slot(Kind.STACK, type, id, name, amount, chance, components, 1, List.of());
		}

		public static Slot tag(String type, String id, @Nullable String name, int size, long amount, float chance) {
			return new Slot(Kind.TAG, type, id, name, amount, chance, null, size, List.of());
		}

		public static Slot anyOf(List<Slot> options, long amount, float chance) {
			return new Slot(Kind.ANY_OF, "", null, null, amount, chance, null, options.size(), List.copyOf(options));
		}

		public boolean isFluid() {
			return "fluid".equals(type);
		}
	}
}
