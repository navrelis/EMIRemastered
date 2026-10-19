package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Turns the collected sources of one effect into the sections of its page. */
final class EffectSectionBuilder {
	private static final Item[] POTION_VARIANTS = {Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW};

	private EffectSectionBuilder() {
	}

	static List<EffectSection> build(StatusEffect effect, EffectSources sources, boolean attributes, boolean potionVariants) {
		List<EffectSection> sections = new ArrayList<>();
		if (attributes) {
			add(sections, attributes(effect));
		}
		int sourceStart = sections.size();
		// stacks that are already shown by a data driven section are not repeated in the hand written one
		List<EmiIngredient> shown = new ArrayList<>();
		EffectSection potions = potions(effect, sources, potionVariants);
		add(sections, potions);
		EffectSection food = food(effect, sources);
		add(sections, food);
		EffectSection stew = stew(effect, sources);
		add(sections, stew);
		for (EffectSection s : sections) {
			for (EffectSection.Group group : s.groups()) {
				shown.addAll(group.cells());
			}
		}
		add(sections, beacon(sources));
		add(sections, table(sources, false, shown, Text.translatable("info.emiffect.section.other")));
		if (sections.size() == sourceStart) {
			sections.add(EffectSection.lines(EffectSection.Kind.NOTE, Text.translatable("info.emiffect.section.sources"),
				List.of(Text.translatable("info.emiffect.no_sources"))));
		}
		add(sections, table(sources, true, shown, Text.translatable("info.emiffect.section.cures")));
		return sections;
	}

	private static void add(List<EffectSection> sections, EffectSection section) {
		if (section != null) {
			sections.add(section);
		}
	}

	private static EffectSection attributes(StatusEffect effect) {
		List<Text> lines = new ArrayList<>();
		effect.forEachAttributeModifier(0, (RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) -> {
			double value = modifier.value();
			if (value == 0.0) {
				return;
			}
			int operation = modifier.operation().getId();
			String number = EffectFormat.modifierValue(value, operation);
			Text name = Text.translatable(attribute.value().getTranslationKey());
			if (value > 0.0) {
				lines.add(Text.translatable("attribute.modifier.plus." + operation, number, name).formatted(Formatting.DARK_BLUE));
			} else {
				lines.add(Text.translatable("attribute.modifier.take." + operation, number, name).formatted(Formatting.DARK_RED));
			}
		});
		return lines.isEmpty() ? null : EffectSection.lines(EffectSection.Kind.ATTRIBUTES, Text.translatable("info.emiffect.section.attributes"), lines);
	}

	private static EffectSection potions(StatusEffect effect, EffectSources sources, boolean variants) {
		if (sources.potions.isEmpty()) {
			return null;
		}
		Map<SourceGroups.Key, List<EffectSources.PotionSource>> grouped = SourceGroups.group(sources.potions,
			p -> new SourceGroups.Key(p.amplifier(), effect.isInstant() ? -1 : p.duration(), 1f));
		List<EffectSection.Group> groups = new ArrayList<>();
		for (Map.Entry<SourceGroups.Key, List<EffectSources.PotionSource>> entry : grouped.entrySet()) {
			List<EmiIngredient> cells = new ArrayList<>();
			for (EffectSources.PotionSource source : entry.getValue()) {
				for (int i = 0; i < (variants ? POTION_VARIANTS.length : 1); i++) {
					cells.add(EmiStack.of(PotionContentsComponent.createStack(POTION_VARIANTS[i], source.potion())));
				}
			}
			groups.add(new EffectSection.Group(label(entry.getKey()), cells));
		}
		return new EffectSection(Text.translatable("info.emiffect.section.potions"), groups, List.of());
	}

	private static EffectSection food(StatusEffect effect, EffectSources sources) {
		if (sources.foods.isEmpty()) {
			return null;
		}
		Map<SourceGroups.Key, List<EffectSources.FoodSource>> grouped = SourceGroups.group(sources.foods,
			f -> new SourceGroups.Key(f.amplifier(), effect.isInstant() ? -1 : f.duration(), f.probability()));
		List<EffectSection.Group> groups = new ArrayList<>();
		for (Map.Entry<SourceGroups.Key, List<EffectSources.FoodSource>> entry : grouped.entrySet()) {
			List<EmiIngredient> cells = new ArrayList<>();
			for (EffectSources.FoodSource source : entry.getValue()) {
				addDistinct(cells, EmiStack.of(source.item()));
			}
			groups.add(new EffectSection.Group(label(entry.getKey()), cells));
		}
		return new EffectSection(Text.translatable("info.emiffect.section.food"), groups, List.of());
	}

	private static EffectSection stew(StatusEffect effect, EffectSources sources) {
		if (sources.stews.isEmpty()) {
			return null;
		}
		Map<SourceGroups.Key, List<EffectSources.StewSource>> grouped = SourceGroups.group(sources.stews,
			s -> new SourceGroups.Key(-1, effect.isInstant() ? -1 : s.duration(), 1f));
		List<EffectSection.Group> groups = new ArrayList<>();
		for (Map.Entry<SourceGroups.Key, List<EffectSources.StewSource>> entry : grouped.entrySet()) {
			List<EmiIngredient> cells = new ArrayList<>();
			for (EffectSources.StewSource source : entry.getValue()) {
				addDistinct(cells, EmiStack.of(source.flower()));
			}
			groups.add(new EffectSection.Group(label(entry.getKey()), cells));
		}
		return new EffectSection(Text.translatable("info.emiffect.section.stew"), groups, List.of());
	}

	private static EffectSection beacon(EffectSources sources) {
		if (sources.beacon.isEmpty()) {
			return null;
		}
		List<Text> lines = new ArrayList<>();
		for (EffectSources.BeaconSource source : sources.beacon) {
			String key = source.amplifier() > 0 ? "boosted" : source.secondary() ? "secondary" : "primary";
			lines.add(Text.translatable("info.emiffect.beacon." + key, source.pyramidLevel()));
		}
		return new EffectSection(Text.translatable("info.emiffect.section.beacon"),
			List.of(new EffectSection.Group(null, List.of(EmiStack.of(Items.BEACON)))), lines);
	}

	private static EffectSection table(EffectSources sources, boolean cure, List<EmiIngredient> shown, Text title) {
		// entries with the same label share a group, so several unlabelled extra_stacks files form one row
		Map<String, List<EffectSources.TableSource>> byLabel = new LinkedHashMap<>();
		for (EffectSources.TableSource source : sources.table) {
			if (source.cure() == cure) {
				byLabel.computeIfAbsent(labelKey(source), k -> new ArrayList<>()).add(source);
			}
		}
		List<EffectSection.Group> groups = new ArrayList<>();
		for (List<EffectSources.TableSource> entries : byLabel.values()) {
			EffectSources.TableSource first = entries.get(0);
			List<EmiIngredient> cells = new ArrayList<>();
			boolean references = false;
			for (EffectSources.TableSource source : entries) {
				for (EmiIngredient ingredient : source.stacks()) {
					references = true;
					if (!shown.contains(ingredient)) {
						addDistinct(cells, ingredient);
					}
				}
			}
			if (cells.isEmpty() && references) {
				continue;
			}
			groups.add(new EffectSection.Group(tableLabel(first), cells));
		}
		return groups.isEmpty() ? null : new EffectSection(cure ? EffectSection.Kind.CURES : EffectSection.Kind.SOURCE, title, groups, List.of());
	}

	private static String labelKey(EffectSources.TableSource source) {
		return source.textKey() + "|" + source.amplifier() + "|" + source.duration();
	}

	private static Text tableLabel(EffectSources.TableSource source) {
		String info = EffectFormat.label(source.amplifier(), source.duration(), 1f);
		if (source.textKey().isEmpty()) {
			return info.isEmpty() ? null : Text.literal(info);
		}
		Text text = Text.translatable(source.textKey());
		return info.isEmpty() ? text : Text.empty().append(text).append(" (" + info + ")");
	}

	private static Text label(SourceGroups.Key key) {
		String label = EffectFormat.label(key.amplifier(), key.duration(), key.probability());
		return label.isEmpty() ? null : Text.literal(label);
	}

	private static void addDistinct(List<EmiIngredient> cells, EmiIngredient ingredient) {
		if (!cells.contains(ingredient)) {
			cells.add(ingredient);
		}
	}
}
