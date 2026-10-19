package dev.emi.remastered.loot.util;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Connects the parsed condition texts to {@link ConditionKind}: special icons and level scaling hints.
 * Client only.
 */
public final class ConditionIcons {

	private ConditionIcons() {
	}

	/** The kind of the condition text if condition hints are switched on. */
	@Nullable
	public static ConditionKind kindOf(Text text) {
		return LootSettings.conditionHints ? classify(text) : null;
	}

	/** The kind of the condition text whatever the setting says. */
	@Nullable
	public static ConditionKind classify(Text text) {
		String root = text.getContent() instanceof TranslatableTextContent content ? content.getKey() : null;
		if (root == null) {
			return null;
		}
		Set<String> keys = new LinkedHashSet<>();
		collectKeys(text, keys);
		return ConditionKind.classify(root, keys);
	}

	public static Identifier texture(ConditionKind kind) {
		return Identifier.of(EMILoot.MOD_ID, "textures/gui/" + kind.texture() + ".png");
	}

	/** The icon as a 16x16 renderable (the 8x8 sprite drawn twice as large), for the Item Info screen. */
	public static EmiRenderable renderable(ConditionKind kind) {
		Identifier texture = texture(kind);
		return (draw, x, y, delta) -> draw.drawTexture(texture, x, y, 16, 16, 0, 0, 8, 8, 8, 8);
	}

	public static MutableText hint(ConditionKind kind) {
		return LText.translatable(kind.hintKey()).formatted(Formatting.GRAY);
	}

	/** The condition text, followed by the hint line when the condition has one. */
	public static List<Text> tooltipLines(Pair<Integer, Text> condition) {
		List<Text> lines = new ArrayList<>(2);
		lines.add(condition.getRight());
		ConditionKind kind = kindOf(condition.getRight());
		if (kind != null) {
			lines.add(hint(kind));
		}
		return lines;
	}

	static void collectKeys(Text text, Set<String> keys) {
		if (text.getContent() instanceof TranslatableTextContent content) {
			keys.add(content.getKey());
			for (Object arg : content.getArgs()) {
				if (arg instanceof Text nested) {
					collectKeys(nested, keys);
				}
			}
		}
		for (Text sibling : text.getSiblings()) {
			collectKeys(sibling, keys);
		}
	}
}
