package dev.emi.remastered.info.providers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.emi.emi.EmiPort;
import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiTagRecipe;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.info.client.InfoService;
import dev.emi.remastered.info.core.CategoryGrouping;
import dev.emi.remastered.info.core.CategoryGrouping.Group;
import dev.emi.remastered.info.core.CoveredCategories;
import dev.emi.remastered.info.core.TagNames;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Built-in provider for EMI's recipe index: recipes producing the stack and recipes using it (grouped by EMI category,
 * from EMI's by-output/by-input indexes), the categories it is a workstation for, EMI info pages and item tags.
 */
public final class RecipesInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EmiPort.id("emi_info", "recipes");
	/** Example stacks collected per category; the screen shows as many as fit and the setting allows. */
	private static final int EXAMPLES = 12;
	private static final int MAX_TAG_LINES = 8;
	private Map<EmiRecipeCategory, Integer> categoryOrder;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public int getOrder() {
		return ORDER_RECIPES;
	}

	@Override
	public void onReload() {
		categoryOrder = null;
	}

	private int order(EmiRecipeCategory category) {
		if (categoryOrder == null) {
			Map<EmiRecipeCategory, Integer> map = new IdentityHashMap<>();
			List<EmiRecipeCategory> categories = EmiApi.getRecipeManager().getCategories();
			for (int i = 0; i < categories.size(); i++) {
				map.put(categories.get(i), i);
			}
			categoryOrder = map;
		}
		return categoryOrder.getOrDefault(category, -1);
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		Set<Identifier> covered = coveredByOthers(stack);
		List<EmiRecipe> infoPages = new ArrayList<>();

		List<EmiRecipe> produced = new ArrayList<>();
		for (EmiRecipe recipe : manager.getRecipesByOutput(stack)) {
			if (recipe.getCategory() == VanillaEmiRecipeCategories.INFO) {
				infoPages.add(recipe);
			} else if (produces(recipe, stack) && !covered.contains(recipe.getCategory().getId())) {
				produced.add(recipe);
			}
		}
		List<Group<EmiRecipeCategory, EmiRecipe>> sources = CategoryGrouping.group(produced, EmiRecipe::getCategory, this::order,
			Set.of(VanillaPlugin.TAG, VanillaPlugin.INGREDIENT, VanillaPlugin.RESOLUTION, VanillaEmiRecipeCategories.FUEL));
		if (!sources.isEmpty()) {
			InfoSection section = collector.addSection(EmiPort.id("emi_info", "produced_by"), InfoSection.Kind.OBTAIN,
				EmiPort.translatable("emi_info.section.produced_by", CategoryGrouping.total(sources)))
				.icon(EmiStack.of(Items.CRAFTING_TABLE));
			boolean anySource = false;
			for (Group<EmiRecipeCategory, EmiRecipe> group : sources) {
				boolean source = group.recipes().stream().anyMatch(r -> !consumes(r, stack));
				anySource |= source;
				section.entry(group.category(), group.category().getName())
					.count(group.count())
					.stacks(CategoryGrouping.examples(group.recipes(), r -> without(r.getInputs(), stack), EmiIngredient::isEmpty,
						EmiIngredient::areEqual, EXAMPLES))
					.link(InfoLink.recipesOf(stack, group.category()))
					.tooltip(source ? List.of() : List.of(EmiPort.translatable("emi_info.tooltip.not_a_source", Formatting.GRAY)));
			}
			section.source(anySource);
		}

		List<EmiRecipe> used = new ArrayList<>();
		List<EmiTagRecipe> tags = new ArrayList<>();
		for (EmiRecipe recipe : manager.getRecipesByInput(stack)) {
			if (recipe instanceof EmiTagRecipe tag) {
				tags.add(tag);
			} else if (recipe.getCategory() == VanillaEmiRecipeCategories.INFO) {
				if (!infoPages.contains(recipe)) {
					infoPages.add(recipe);
				}
			} else if (uses(recipe, stack) && !covered.contains(recipe.getCategory().getId())) {
				used.add(recipe);
			}
		}
		List<Group<EmiRecipeCategory, EmiRecipe>> uses = CategoryGrouping.group(used, EmiRecipe::getCategory, this::order,
			Set.of(VanillaPlugin.TAG, VanillaPlugin.INGREDIENT, VanillaPlugin.RESOLUTION));
		if (!uses.isEmpty()) {
			InfoSection section = collector.addSection(EmiPort.id("emi_info", "used_in"), InfoSection.Kind.USE,
				EmiPort.translatable("emi_info.section.used_in", CategoryGrouping.total(uses)))
				.icon(EmiStack.of(Items.KNOWLEDGE_BOOK));
			for (Group<EmiRecipeCategory, EmiRecipe> group : uses) {
				section.entry(group.category(), group.category().getName())
					.count(group.count())
					.stacks(CategoryGrouping.examples(group.recipes(), r -> without(r.getOutputs(), stack), EmiIngredient::isEmpty,
						EmiIngredient::areEqual, EXAMPLES))
					.link(InfoLink.usesOf(stack, group.category()));
			}
		}

		List<EmiRecipe> workstation = EmiRecipes.byWorkstation.getOrDefault(stack, List.of());
		List<Group<EmiRecipeCategory, EmiRecipe>> stations = CategoryGrouping.group(workstation, EmiRecipe::getCategory,
			this::order, Set.of());
		if (!stations.isEmpty()) {
			InfoSection section = collector.addSection(EmiPort.id("emi_info", "workstation"), InfoSection.Kind.USE,
				EmiPort.translatable("emi_info.section.workstation"))
				.icon(stack);
			for (Group<EmiRecipeCategory, EmiRecipe> group : stations) {
				section.entry(group.category(), group.category().getName())
					.count(group.count())
					.link(InfoLink.usesOf(stack, group.category()));
			}
		}

		if (!infoPages.isEmpty()) {
			collector.addSection(EmiPort.id("emi_info", "information"), InfoSection.Kind.DETAILS,
				EmiPort.translatable("emi_info.section.information"))
				.icon(VanillaEmiRecipeCategories.INFO)
				.link(EmiPort.translatable("emi_info.information.open", infoPages.size()), InfoLink.recipes(infoPages));
		}

		if (!tags.isEmpty()) {
			InfoSection section = collector.addSection(EmiPort.id("emi_info", "tags"), InfoSection.Kind.DETAILS,
				EmiPort.translatable("emi_info.section.tags", tags.size()))
				.icon(VanillaPlugin.TAG);
			int shown = Math.min(tags.size(), MAX_TAG_LINES);
			List<Text> names = new ArrayList<>(shown);
			List<String> ids = new ArrayList<>(shown);
			for (int i = 0; i < shown; i++) {
				names.add(tagName(tags.get(i).key));
				ids.add(tags.get(i).key.id().toString());
			}
			List<TagNames.Label> labels = TagNames.disambiguate(names.stream().map(Text::getString).toList(), ids);
			for (int i = 0; i < shown; i++) {
				MutableText name = names.get(i).copy();
				if (!labels.get(i).suffix().isEmpty()) {
					name.append(EmiPort.literal(labels.get(i).suffix(), Formatting.GRAY));
				}
				section.text(name, List.of(EmiPort.literal("#" + ids.get(i), Formatting.GRAY)));
			}
			section.link(EmiPort.translatable("emi_info.tags.open", tags.size()), InfoLink.usesOf(stack, VanillaPlugin.TAG));
		}
	}

	/** The translated tag name if there is one, else a readable name made from the tag path. */
	private static Text tagName(TagKey<?> key) {
		EmiTagKey<?> tag = EmiTagKey.of(key);
		return tag.hasTranslation() ? tag.getTagName() : EmiPort.literal(TagNames.readable(key.id().getPath()));
	}

	/**
	 * Categories that other active providers list themselves for this stack (computed once per page build): their
	 * rows are left out here so the page does not show the same recipes twice.
	 */
	private Set<Identifier> coveredByOthers(EmiStack stack) {
		return CoveredCategories.compute(ItemInfoRegistry.getProviders(),
			p -> p != this && InfoService.isModuleOn(p) && p.appliesTo(stack), p -> p.coveredCategories(stack));
	}

	private static boolean produces(EmiRecipe recipe, EmiStack stack) {
		for (EmiStack output : recipe.getOutputs()) {
			if (output.isEqual(stack)) {
				return true;
			}
		}
		return false;
	}

	private static boolean contains(EmiIngredient ingredient, EmiStack stack) {
		for (EmiStack s : ingredient.getEmiStacks()) {
			if (s.isEqual(stack)) {
				return true;
			}
		}
		return false;
	}

	/** Whether the recipe turns the stack into itself (repairing, cloning, dyeing...). */
	private static boolean consumes(EmiRecipe recipe, EmiStack stack) {
		for (EmiIngredient input : recipe.getInputs()) {
			if (contains(input, stack)) {
				return true;
			}
		}
		return false;
	}

	/** Same rule as EMI's "view uses": the stack is in an input or a catalyst. */
	private static boolean uses(EmiRecipe recipe, EmiStack stack) {
		if (consumes(recipe, stack)) {
			return true;
		}
		for (EmiIngredient catalyst : recipe.getCatalysts()) {
			if (contains(catalyst, stack)) {
				return true;
			}
		}
		return false;
	}

	private static List<EmiIngredient> without(List<? extends EmiIngredient> list, EmiStack stack) {
		List<EmiIngredient> result = new ArrayList<>(list.size());
		for (EmiIngredient ingredient : list) {
			if (!ingredient.isEmpty() && !contains(ingredient, stack)) {
				result.add(ingredient);
			}
		}
		return result;
	}
}
