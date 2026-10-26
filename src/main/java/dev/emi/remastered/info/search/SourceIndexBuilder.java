package dev.emi.remastered.info.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;

/**
 * Computes the source labels of stacks from EMI's recipe index. One instance per build (it caches the labels of the
 * categories); not thread safe, create one per thread. The "no known source" rule mirrors the recipe provider of the
 * Item Info screen, see {@link SourceMatch#countsAsSource}.
 */
final class SourceIndexBuilder {
	/** What the Item Info screen leaves out of "Produced by": information, tag, ingredient, resolution and fuel pages. */
	private static volatile Set<EmiRecipeCategory> excluded;

	private final EmiRecipeManager manager;
	private final Map<EmiRecipeCategory, List<String>> categoryLabels = new IdentityHashMap<>();
	private final Set<String> labels = new LinkedHashSet<>();
	private int labelCount;
	private boolean noSource;

	SourceIndexBuilder() {
		this(EmiApi.getRecipeManager());
	}

	SourceIndexBuilder(EmiRecipeManager manager) {
		this.manager = manager;
	}

	private static Set<EmiRecipeCategory> excluded() {
		Set<EmiRecipeCategory> set = excluded;
		if (set == null) {
			set = new HashSet<>(List.of(VanillaEmiRecipeCategories.INFO, VanillaPlugin.TAG, VanillaPlugin.INGREDIENT,
				VanillaPlugin.RESOLUTION, VanillaEmiRecipeCategories.FUEL));
			excluded = set;
		}
		return set;
	}

	/** The joined labels of the stack (empty if nothing produces it); {@link #noSource()} reports the second fact. */
	String compute(EmiStack stack) {
		labels.clear();
		boolean source = false;
		Set<EmiRecipeCategory> excluded = excluded();
		for (EmiRecipe recipe : manager.getRecipesByOutput(stack)) {
			EmiRecipeCategory category = recipe.getCategory();
			if (category == null) {
				continue;
			}
			boolean counts;
			try {
				counts = SourceMatch.countsAsSource(excluded.contains(category), produces(recipe, stack), consumes(recipe, stack));
			} catch (RuntimeException e) {
				continue;
			}
			if (!counts) {
				continue;
			}
			source = true;
			SourceText.addAll(labels, categoryLabels(category));
			if (recipe instanceof SourceLabels extra) {
				try {
					SourceText.addAll(labels, extra.sourceLabels());
				} catch (RuntimeException e) {
					EmiLog.error("A recipe failed to name its source for the search: " + recipe.getId(), e);
				}
			}
		}
		noSource = !source && (stack.getKey() instanceof Item || stack.getKey() instanceof Fluid);
		String joined = SourceText.join(labels);
		if (!joined.isEmpty()) {
			labelCount += labels.size();
		}
		return joined;
	}

	/** Whether the last {@link #compute} found no source for an item or fluid. */
	boolean noSource() {
		return noSource;
	}

	/** Distinct labels of all stacks computed so far, summed per stack. */
	int labelCount() {
		return labelCount;
	}

	private List<String> categoryLabels(EmiRecipeCategory category) {
		List<String> list = categoryLabels.get(category);
		if (list == null) {
			list = new ArrayList<>(3);
			try {
				list.add(category.getName().getString());
			} catch (RuntimeException e) {
				// unnamed category: the id still matches
			}
			list.add(category.getId().toString());
			list.add(category.getId().getPath());
			categoryLabels.put(category, list);
		}
		return list;
	}

	private static boolean produces(EmiRecipe recipe, EmiStack stack) {
		for (EmiStack output : recipe.getOutputs()) {
			if (output.isEqual(stack)) {
				return true;
			}
		}
		return false;
	}

	private static boolean consumes(EmiRecipe recipe, EmiStack stack) {
		for (EmiIngredient input : recipe.getInputs()) {
			for (EmiStack s : input.getEmiStacks()) {
				if (s.isEqual(stack)) {
					return true;
				}
			}
		}
		return false;
	}
}
