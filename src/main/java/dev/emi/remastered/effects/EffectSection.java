package dev.emi.remastered.effects;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.text.Text;

/**
 * One block of an effect page: a heading, groups of clickable stacks (each with an optional label line above) and
 * plain text lines below them.
 *
 * @param kind what the block is about; the Item Info provider sorts the blocks by it
 */
record EffectSection(Kind kind, Text title, List<Group> groups, List<Text> lines) {

	/** What a block describes. */
	enum Kind {
		/** Attribute modifiers per level. */
		ATTRIBUTES,
		/** A way to get the effect (potions, food, stew, beacon, table entries). */
		SOURCE,
		/** Things that remove the effect. */
		CURES,
		/** Placeholder text such as "no known sources". */
		NOTE
	}

	/** {@code label} may be null. */
	record Group(@Nullable Text label, List<EmiIngredient> cells) {
	}

	/** A block of kind {@link Kind#SOURCE}. */
	EffectSection(Text title, List<Group> groups, List<Text> lines) {
		this(Kind.SOURCE, title, groups, lines);
	}

	static EffectSection lines(Kind kind, Text title, List<Text> lines) {
		return new EffectSection(kind, title, List.of(), lines);
	}
}
