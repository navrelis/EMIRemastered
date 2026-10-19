package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.stats.StatsSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Item Info provider of the effects module.
 * <ul>
 * <li>An item that applies status effects (potions, splash and lingering potions, tipped arrows, suspicious stew,
 * ominous bottles) gets a Details section with every effect, its level and its duration. Foods are left to the Food
 * section of the stats module, which already lists their effects with chances; they are only handled here while that
 * section is not shown.</li>
 * <li>An effect stack ({@code emiffect:effect}) gets the sources, cures and attribute modifiers of the effect, taken
 * from the blocks of its {@link EffectInfoRecipe} page, so the data is the one-pass index of the EMI reload and goes
 * away with it.</li>
 * </ul>
 * It covers the effect page category (see {@link #coveredCategories}), so the built-in recipe provider leaves its
 * "Status Effect Info" row out for those stacks. Client only.
 */
public final class EffectInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Identifier.of("emiffect", "effects");
	private static final Identifier ITEM_EFFECTS_ID = Identifier.of("emiffect", "item_effects");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "effects";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return Modules.EFFECTS.isEnabled() && EffectsSettings.itemInfo && (stack instanceof EffectEmiStack || stack.getKey() instanceof Item);
	}

	/**
	 * The effect page category, only for stacks whose effects or sources this page lists itself: an item with effects
	 * or an effect with sources. The built-in recipe provider then leaves its "Status Effect Info" row out.
	 */
	@Override
	public Set<Identifier> coveredCategories(EmiStack stack) {
		boolean shown = stack instanceof EffectEmiStack effect ? !pageSections(effect).isEmpty()
			: !itemLines(stack.getItemStack()).lines().isEmpty();
		return shown ? Set.of(EffectsEmi.CATEGORY.getId()) : Set.of();
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		if (stack instanceof EffectEmiStack effect) {
			sources(effect, collector);
		} else {
			itemEffects(stack.getItemStack(), collector);
		}
	}

	/** One effect an item applies. */
	private record Line(StatusEffectInstance instance, float probability) {
	}

	/** The effects of an item and the factor its durations are shown with. */
	private record ItemLines(List<Line> lines, float factor) {
	}

	private static ItemLines itemLines(ItemStack item) {
		if (item.isEmpty()) {
			return new ItemLines(List.of(), 1f);
		}
		Item type = item.getItem();
		float factor = type == Items.TIPPED_ARROW ? ItemEffectFormat.ARROW_FACTOR
			: type == Items.LINGERING_POTION ? ItemEffectFormat.LINGERING_FACTOR : 1f;
		List<Line> lines = new ArrayList<>();
		PotionContentsComponent contents = item.get(DataComponentTypes.POTION_CONTENTS);
		if (contents != null) {
			for (StatusEffectInstance instance : contents.getEffects()) {
				lines.add(new Line(instance, 1f));
			}
		}
		SuspiciousStewEffectsComponent stew = item.get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
		if (stew != null) {
			for (SuspiciousStewEffectsComponent.StewEffect effect : stew.effects()) {
				lines.add(new Line(effect.createStatusEffectInstance(), 1f));
			}
		}
		Integer ominous = item.get(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);
		if (ominous != null) {
			lines.add(new Line(new StatusEffectInstance(StatusEffects.BAD_OMEN, EffectSourceCollector.OMINOUS_BOTTLE_DURATION, ominous), 1f));
		}
		FoodComponent food = item.get(DataComponentTypes.FOOD);
		if (food != null && !statsShowsFood()) {
			for (FoodComponent.StatusEffectEntry entry : food.effects()) {
				lines.add(new Line(entry.effect(), entry.probability()));
			}
		}
		return new ItemLines(lines, factor);
	}

	private static void itemEffects(ItemStack item, InfoCollector collector) {
		ItemLines found = itemLines(item);
		List<Line> lines = found.lines();
		float factor = found.factor();
		if (lines.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ITEM_EFFECTS_ID, InfoSection.Kind.DETAILS, Text.translatable("info.emiffect.item.title"))
			.icon(EffectsEmi.CATEGORY);
		List<Text> note = factor == ItemEffectFormat.ARROW_FACTOR ? List.of(Text.translatable("info.emiffect.item.arrow").formatted(Formatting.GRAY))
			: factor == ItemEffectFormat.LINGERING_FACTOR ? List.of(Text.translatable("info.emiffect.item.lingering").formatted(Formatting.GRAY))
			: List.of();
		for (Line line : lines) {
			StatusEffectInstance instance = line.instance();
			StatusEffect effect = instance.getEffectType().value();
			EffectEmiStack icon = new EffectEmiStack(effect);
			Text label = Text.empty().append(effect.getName()).append(" " + EffectFormat.level(instance.getAmplifier()));
			String detail = ItemEffectFormat.detail(instance.isInfinite() ? -1 : instance.getDuration(), factor, line.probability());
			section.entry(icon, label)
				.detail(detail.isEmpty() ? null : Text.literal(detail))
				.link(InfoLink.recipesOf(icon, null))
				.tooltip(note);
		}
	}

	/** Whether the stats module adds its Food section (with the food's effects) to the page. */
	private static boolean statsShowsFood() {
		return Modules.STATS.isEnabled() && StatsSettings.infoSections;
	}

	private static void sources(EffectEmiStack stack, InfoCollector collector) {
		int index = 0;
		boolean linked = false;
		for (EffectSection block : pageSections(stack)) {
			index++;
			boolean source = block.kind() == EffectSection.Kind.SOURCE;
			InfoSection section = collector.addSection(Identifier.of("emiffect", block.kind().name().toLowerCase(Locale.ROOT) + "/" + name(block, index)),
				source ? InfoSection.Kind.OBTAIN : InfoSection.Kind.DETAILS, block.title());
			EmiIngredient icon = stack;
			for (EffectSection.Group group : block.groups()) {
				if (group.cells().isEmpty()) {
					continue;
				}
				if (source && icon == stack) {
					icon = group.cells().get(0);
				}
				if (group.label() == null) {
					section.stacks(group.cells());
				} else {
					section.stacks(group.label(), group.cells());
				}
			}
			for (Text line : block.lines()) {
				section.text(line);
			}
			section.icon(icon);
			if (!linked) {
				// the built-in row opened the effect page; this link takes its place
				section.link(Text.translatable("info.emiffect.item.open"), InfoLink.recipesOf(stack, null));
				linked = true;
			}
		}
	}

	/** The blocks of the effect's EMI page that this page lists (everything but the "no sources" note). */
	private static List<EffectSection> pageSections(EffectEmiStack stack) {
		EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(EffectInfoRecipe.recipeId(stack.getId()));
		if (!(recipe instanceof EffectInfoRecipe page)) {
			return List.of();
		}
		List<EffectSection> blocks = new ArrayList<>();
		for (EffectSection block : page.sections()) {
			if (block.kind() != EffectSection.Kind.NOTE) {
				blocks.add(block);
			}
		}
		return blocks;
	}

	/** Last part of the title's lang key ({@code potions}), or the position when the title is no lang key. */
	private static String name(EffectSection block, int index) {
		if (block.title().getContent() instanceof TranslatableTextContent content) {
			String key = content.getKey();
			return key.substring(key.lastIndexOf('.') + 1);
		}
		return Integer.toString(index);
	}
}
