package dev.emi.remastered.letsdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * What a {@link LetsDoSubModule} gets: registers categories and recipes, isolates failures per category and counts
 * what was added. Only vanilla and EMI classes; never touches a Let's Do class.
 */
public final class LetsDoContext {
	public static final String NAMESPACE = "emi_letsdo_compat";

	private final EmiRegistry registry;
	private final String subModule;
	private final FailureReport failures;
	private int categories;
	private int recipes;

	public LetsDoContext(EmiRegistry registry, String subModule) {
		this.registry = registry;
		this.subModule = subModule;
		this.failures = new FailureReport(subModule);
	}

	/** Runs the body of one category; an incompatible mod class or a broken recipe only skips that category. */
	public void run(String name, Runnable body) {
		try {
			body.run();
		} catch (LinkageError | RuntimeException e) {
			failures.add(name, e);
		}
	}

	/** Registers a category together with its workstations and returns it. */
	public EmiRecipeCategory addCategory(Identifier id, EmiRenderable icon, EmiIngredient... workstations) {
		EmiRecipeCategory category = new EmiRecipeCategory(id, icon);
		registry.addCategory(category);
		for (EmiIngredient workstation : workstations) {
			registry.addWorkstation(category, workstation);
		}
		categories++;
		return category;
	}

	public void addRecipe(EmiRecipe recipe) {
		registry.addRecipe(recipe);
		recipes++;
	}

	/** Adds one display recipe for every recipe of the given (Let's Do) recipe type. */
	public <I extends RecipeInput, R extends Recipe<I>> void addRecipes(RecipeType<R> type, RecipeFactory<R> factory) {
		for (RecipeEntry<R> entry : registry.getRecipeManager().listAllOfType(type)) {
			EmiRecipe recipe = factory.create(entry.id(), entry.value());
			if (recipe != null) {
				addRecipe(recipe);
			}
		}
	}

	/** All ingredients of a recipe, in recipe order. */
	public static List<EmiIngredient> ingredients(Recipe<?> recipe) {
		List<EmiIngredient> list = new ArrayList<>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			list.add(EmiIngredient.of(ingredient));
		}
		return list;
	}

	/** The result of a recipe, empty if it has none. */
	public static List<EmiStack> outputs(Recipe<?> recipe) {
		EmiStack stack = EmiStack.of(EmiPort.getOutput(recipe));
		return stack.isEmpty() ? List.of() : List.of(stack);
	}

	/** An item tag as ingredient; falls back to the vanilla tag contents when EMI did not index the tag. */
	public static EmiIngredient tag(TagKey<Item> tag) {
		EmiIngredient ingredient = EmiIngredient.of(tag);
		return ingredient.isEmpty() ? EmiIngredient.of(Ingredient.fromTag(tag)) : ingredient;
	}

	public static TagKey<Item> itemTag(String namespace, String path) {
		return TagKey.of(RegistryKeys.ITEM, Identifier.of(namespace, path));
	}

	/** An item by id, empty if the installed version of its mod has no such item. */
	public static Optional<Item> item(String namespace, String path) {
		return Registries.ITEM.getOrEmpty(Identifier.of(namespace, path));
	}

	/** Id of a category in the namespace of this module. */
	public static Identifier id(String path) {
		return Identifier.of(NAMESPACE, path);
	}

	/** Recipe id for recipes that are not backed by a data pack recipe. */
	public static Identifier syntheticId(String category, String name) {
		return Identifier.of(NAMESPACE, "/" + category + "/" + name);
	}

	/** Logs one info line with the counts, and ONE warning line if something was skipped. */
	public void report() {
		if (failures.isEmpty()) {
			EmiLog.info("[Let's Do] " + subModule + ": " + categories + " categories, " + recipes + " recipes");
		} else {
			EmiLog.info("[Let's Do] " + subModule + ": " + categories + " categories, " + recipes + " recipes (incomplete)");
			EmiLog.warn("[Let's Do] " + failures.format());
		}
	}

	/** Reports a failure of the whole sub-module (it could not be loaded or its register method failed early). */
	public void fail(String what, Throwable t) {
		failures.add(what, t);
	}

	@FunctionalInterface
	public interface RecipeFactory<R extends Recipe<?>> {
		/** Returns the display recipe, or null to skip this recipe. */
		EmiRecipe create(Identifier id, R recipe);
	}
}
