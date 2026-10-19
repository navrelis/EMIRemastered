package dev.emi.remastered.info.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.client.InfoPage;
import dev.emi.remastered.info.client.SectionImpl;
import dev.emi.remastered.info.export.ExportRows.IngredientData;
import dev.emi.remastered.info.export.ExportRows.RecipeRow;
import dev.emi.remastered.info.export.ExportRows.SourceData;
import dev.emi.remastered.info.export.ExportRows.SourceRow;
import dev.emi.remastered.info.export.ExportRows.StackData;
import dev.emi.remastered.info.export.ExportRows.TagRow;
import dev.emi.remastered.info.providers.NoSourceInfoProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;

/** Reads EMI (render thread) into the plain row types of {@link ExportRows}. Client only. */
final class ExportData {

	private ExportData() {
	}

	static IngredientData ingredient(EmiIngredient ingredient) {
		if (ingredient instanceof TagEmiIngredient tag) {
			return new IngredientData(tag.key.id().toString(), List.of(), tag.getAmount(), tag.getChance());
		}
		List<String> ids = ingredient.getEmiStacks().stream().filter(s -> !s.isEmpty())
			.map(s -> s.getId().toString()).distinct().toList();
		return new IngredientData(null, ids, ingredient.getAmount(), ingredient.getChance());
	}

	private static List<IngredientData> ingredients(List<? extends EmiIngredient> list) {
		return list.stream().filter(i -> !i.isEmpty()).map(ExportData::ingredient).toList();
	}

	static RecipeRow recipe(EmiRecipe recipe) {
		return new RecipeRow(recipe.getId() == null ? "" : recipe.getId().toString(),
			recipe.getCategory().getId().toString(), ingredients(recipe.getInputs()),
			ingredients(recipe.getCatalysts()), ingredients(recipe.getOutputs()));
	}

	static StackData stack(EmiStack stack) {
		String id = stack.getId().toString();
		String name;
		try {
			name = stack.getName().getString();
		} catch (RuntimeException e) {
			name = id;
		}
		String type = stack.getKey() instanceof Item ? "item" : stack.getKey() instanceof Fluid ? "fluid" : "other";
		String components = stack.getComponentChanges().isEmpty() ? "" : stack.getComponentChanges().toString();
		boolean hidden = EmiHidden.isHidden(stack) || EmiHidden.isDisabled(stack);
		return new StackData(id, components, name, stack.getId().getNamespace(), type, hidden);
	}

	/** The marker's rule ({@link NoSourceInfoProvider}): only items and fluids can be "without source". */
	static boolean canBeWithoutSource(EmiStack stack) {
		return stack.getKey() instanceof Item || stack.getKey() instanceof Fluid;
	}

	/** The OBTAIN sections of a collected page, without the "no known source" marker (it depends on a setting). */
	static SourceRow sources(StackData data, InfoPage page) {
		boolean hasSource = false;
		List<SourceData> sections = new ArrayList<>();
		for (SectionImpl section : page.sections()) {
			if (section.kind != InfoSection.Kind.OBTAIN || section.id.equals(NoSourceInfoProvider.ID)) {
				continue;
			}
			hasSource |= section.countsAsSource();
			sections.add(new SourceData(section.id.toString(), section.providerId.toString(),
				section.title.getString(), section.countsAsSource()));
		}
		return new SourceRow(data, hasSource, sections);
	}

	static List<EmiTagKey<?>> tags() {
		return EmiTags.TAGS.stream().sorted((a, b) -> {
			int c = a.raw().registry().getValue().toString().compareTo(b.raw().registry().getValue().toString());
			return c != 0 ? c : a.id().toString().compareTo(b.id().toString());
		}).toList();
	}

	static <T> TagRow tag(EmiTagKey<T> key) {
		List<String> members = EmiTags.getValues(key).stream().map(s -> s.getId().toString()).distinct().sorted().toList();
		return new TagRow(key.raw().registry().getValue().toString(), key.id().toString(), members);
	}

	/** Seconds with one decimal, independent of the locale. */
	static String seconds(long nanos) {
		return String.format(Locale.ROOT, "%.1f", nanos / 1.0e9);
	}
}
