package dev.emi.remastered.stats.client;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget.Alignment;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.stats.client.FoodFacts.EffectFact;
import dev.emi.remastered.stats.core.StatsMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

/**
 * The page of one food item in the {@code emi_stats:food} category: hunger shanks, saturation bar, eating time,
 * whether it can always be eaten, the item it leaves behind and its effects with their chance. Everything is read
 * from the item's food component. The item is the recipe's input and the recipe has no output, so the page shows
 * under "uses" and never counts as a way to obtain anything.
 */
final class FoodRecipe implements EmiRecipe {
	static final int WIDTH = 160;
	static final int MAX_WIDTH = 224;
	private static final int EFFECT_LABEL_X = 22;
	private static final int CHANCE_GAP = 6;
	/** Space between the right-aligned chance column and the right edge of the page. */
	private static final int RIGHT_PAD = 3;
	/** Best food first, then by saturation, then by id. */
	static final Comparator<EmiRecipe> SORT = Comparator
		.comparingInt((EmiRecipe r) -> r instanceof FoodRecipe f ? -f.facts.nutrition() : 0)
		.thenComparingDouble(r -> r instanceof FoodRecipe f ? -f.facts.saturation() : 0)
		.thenComparing(r -> r.getId() == null ? "" : r.getId().toString());
	private static final int TEXT_X = HungerIcons.WIDTH + 6;
	private static final int HUNGER_Y = 24;
	private static final int SATURATION_Y = 36;
	private static final int EAT_Y = 48;
	private static final int ALWAYS_Y = 60;
	private static final int ROWS_Y = 72;
	private static final int MAX_EFFECTS = 5;
	private static final int GREEN = 0xFF55FF55;
	private static final int GRAY = 0xFFAAAAAA;

	private final EmiStack stack;
	private final FoodFacts facts;
	private final Identifier id;

	FoodRecipe(EmiStack stack, Identifier itemId, FoodFacts facts) {
		this.stack = stack;
		this.facts = facts;
		this.id = Identifier.of("emi_stats", "/food/" + itemId.getNamespace() + "/" + itemId.getPath());
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return StatsEmi.FOOD;
	}

	@Override
	public @Nullable Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(stack);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of();
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	/**
	 * At least {@link #WIDTH}, wider when an effect row (icon, label with level and duration, chance) needs more, so
	 * that no label is cut: the enchanted golden apple's {@code Regeneration II (00:20)} next to {@code 100%} does not
	 * fit 160 pixels. Capped, the label is only trimmed beyond {@link #MAX_WIDTH}.
	 */
	@Override
	public int getDisplayWidth() {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		if (font == null) {
			return WIDTH;
		}
		int needed = Math.max(WIDTH, TEXT_X + Math.max(font.getWidth(hungerText(facts.nutrition())), font.getWidth(saturationText())) + RIGHT_PAD);
		int labelX = StatsText.effectStacks() ? EFFECT_LABEL_X : 0;
		List<EffectFact> effects = facts.effects();
		for (int i = 0; i < Math.min(effects.size(), MAX_EFFECTS); i++) {
			EffectFact effect = effects.get(i);
			needed = Math.max(needed, labelX + font.getWidth(StatsText.effectLabel(effect.instance())) + CHANCE_GAP + RIGHT_PAD
				+ font.getWidth(StatsText.percent(effect.chance())));
		}
		return Math.min(needed, MAX_WIDTH);
	}

	@Override
	public int getDisplayHeight() {
		int y = ROWS_Y;
		if (!facts.convertsTo().isEmpty()) {
			y += 20;
		}
		if (!facts.effects().isEmpty()) {
			y += 12 + Math.min(facts.effects().size(), MAX_EFFECTS) * rowHeight();
		}
		return y;
	}

	private static Text hungerText(int nutrition) {
		return EmiPort.translatable("emi_stats.food.hunger", StatsText.number(nutrition));
	}

	private Text saturationText() {
		return EmiPort.translatable("emi_stats.food.saturation", StatsText.number(StatsMath.round2(facts.saturation())));
	}

	private static int rowHeight() {
		return StatsText.effectStacks() ? 18 : 11;
	}

	private static OrderedText fit(TextRenderer font, Text text, int width) {
		if (font.getWidth(text) <= width) {
			return text.asOrderedText();
		}
		int dots = font.getWidth("...");
		return OrderedText.concat(Language.getInstance().reorder(font.trimToWidth(text, Math.max(0, width - dots))),
			OrderedText.styledForwardsVisitedString("...", Style.EMPTY));
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int width = widgets.getWidth();
		int nutrition = facts.nutrition();

		widgets.addSlot(stack, 0, 0).recipeContext(this);
		widgets.addText(fit(font, stack.getName(), width - 22), 22, 5, -1, true);

		widgets.addDrawable(0, HUNGER_Y, HungerIcons.WIDTH, HungerIcons.HEIGHT,
			(draw, mouseX, mouseY, delta) -> HungerIcons.drawShanks(draw, 0, 0, nutrition));
		widgets.addText(hungerText(nutrition), TEXT_X, HUNGER_Y + 1, -1, true);
		widgets.addTooltipText(List.of(EmiPort.translatable("emi_stats.food.hunger.tooltip", StatsText.number(nutrition),
			StatsText.number(nutrition / 2.0))), 0, HUNGER_Y, width, 10);

		float fill = StatsMath.fraction(facts.saturation(), StatsMath.MAX_FOOD);
		widgets.addDrawable(0, SATURATION_Y + 2, HungerIcons.WIDTH, 5,
			(draw, mouseX, mouseY, delta) -> HungerIcons.drawBar(draw, 0, 0, fill));
		widgets.addText(saturationText(), TEXT_X, SATURATION_Y, -1, true);
		widgets.addTooltipText(List.of(EmiPort.translatable("emi_stats.food.saturation.tooltip",
			StatsText.number(StatsMath.round2(StatsMath.saturationModifier(nutrition, facts.saturation()))))),
			0, SATURATION_Y, width, 10);

		widgets.addText(EmiPort.translatable("emi_stats.food.eat_time", StatsText.number(StatsMath.round2(facts.eatSeconds()))),
			0, EAT_Y, -1, true);
		if (facts.canAlwaysEat()) {
			widgets.addText(EmiPort.translatable("emi_stats.food.always"), 0, ALWAYS_Y, GREEN, true);
		} else {
			widgets.addText(EmiPort.translatable("emi_stats.food.hungry_only"), 0, ALWAYS_Y, GRAY, true);
		}

		int y = ROWS_Y;
		if (!facts.convertsTo().isEmpty()) {
			Text label = EmiPort.translatable("emi_stats.food.converts");
			widgets.addText(label, 0, y + 5, -1, true);
			widgets.addSlot(EmiStack.of(facts.convertsTo()), font.getWidth(label) + 4, y).recipeContext(this);
			y += 20;
		}
		List<EffectFact> effects = facts.effects();
		if (effects.isEmpty()) {
			return;
		}
		widgets.addText(EmiPort.translatable("emi_stats.food.effects"), 0, y, -1, true);
		y += 12;
		boolean slots = StatsText.effectStacks();
		int rowHeight = rowHeight();
		int rows = Math.min(Math.min(effects.size(), MAX_EFFECTS), Math.max(1, (widgets.getHeight() - y) / rowHeight));
		boolean more = effects.size() > rows;
		if (more && rows > 1) {
			rows--;
		}
		for (int i = 0; i < rows; i++) {
			EffectFact effect = effects.get(i);
			Text chance = StatsText.percent(effect.chance());
			int chanceWidth = font.getWidth(chance);
			Text label = StatsText.effectLabel(effect.instance());
			if (slots) {
				widgets.addSlot(StatsText.effectStack(effect.instance(), 1.0f), 0, y).recipeContext(this);
				widgets.addText(fit(font, label, width - EFFECT_LABEL_X - chanceWidth - CHANCE_GAP - RIGHT_PAD), EFFECT_LABEL_X, y + 5, -1, true);
				widgets.addText(chance, width - RIGHT_PAD, y + 5, effect.chance() < 1 ? 0xFFFFD27F : GREEN, true).horizontalAlign(Alignment.END);
			} else {
				widgets.addText(fit(font, label, width - chanceWidth - CHANCE_GAP - RIGHT_PAD), 0, y, -1, true);
				widgets.addText(chance, width - RIGHT_PAD, y, effect.chance() < 1 ? 0xFFFFD27F : GREEN, true).horizontalAlign(Alignment.END);
			}
			y += rowHeight;
		}
		if (more) {
			widgets.addText(EmiPort.translatable("emi_stats.food.effects.more", effects.size() - rows),
				0, y, GRAY, true);
		}
	}
}
