package dev.emi.remastered.effects;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.text.Text;

/**
 * One block of an effect page: a heading, groups of clickable stacks (each with an optional label line above) and
 * plain text lines below them.
 */
record EffectSection(Text title, List<Group> groups, List<Text> lines) {

	/** {@code label} may be null. */
	record Group(@Nullable Text label, List<EmiIngredient> cells) {
	}

	static EffectSection lines(Text title, List<Text> lines) {
		return new EffectSection(title, List.of(), lines);
	}
}
