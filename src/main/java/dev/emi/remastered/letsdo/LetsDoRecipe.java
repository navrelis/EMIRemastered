package dev.emi.remastered.letsdo;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.recipe.Recipe;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Display recipe of the Let's Do categories. It holds only EMI stacks and a {@link LetsDoLayouts.Layout}, copied out of
 * the Let's Do recipe when it is registered, so drawing it never needs a class of the Let's Do mod.
 */
public class LetsDoRecipe extends BasicEmiRecipe {
	private final LetsDoLayouts.Layout layout;
	private final List<EmiIngredient> extras;
	private final List<Text> tooltip = new ArrayList<>();
	private boolean recipeTree = true;

	/**
	 * @param inputs    ingredients, in the order the layout places them
	 * @param outputs   results
	 * @param catalysts blocks or tools that are not used up (shown by interaction layouts)
	 * @param extras    additional decoration slots a layout needs (water, heat, ...); not part of the recipe lookup
	 */
	public LetsDoRecipe(EmiRecipeCategory category, Identifier id, LetsDoLayouts.Layout layout,
			List<? extends EmiIngredient> inputs, List<? extends EmiStack> outputs, List<? extends EmiIngredient> catalysts,
			List<? extends EmiIngredient> extras) {
		super(category, id, layout.width(), layout.height());
		this.layout = layout;
		this.inputs.addAll(inputs);
		this.outputs.addAll(outputs);
		this.catalysts.addAll(catalysts);
		this.extras = List.copyOf(extras);
	}

	/** Display of a plain Let's Do recipe: its ingredients as inputs and its result as output. */
	public static LetsDoRecipe fromRecipe(EmiRecipeCategory category, Identifier id, LetsDoLayouts.Layout layout, Recipe<?> recipe) {
		return new LetsDoRecipe(category, id, layout, LetsDoContext.ingredients(recipe), LetsDoContext.outputs(recipe), List.of(), List.of());
	}

	/** Lines shown when hovering the arrow area of an interaction layout. */
	public LetsDoRecipe tooltip(Text... lines) {
		tooltip.addAll(List.of(lines));
		return this;
	}

	public LetsDoRecipe recipeTree(boolean recipeTree) {
		this.recipeTree = recipeTree;
		return this;
	}

	@Override
	public boolean supportsRecipeTree() {
		return recipeTree && super.supportsRecipeTree();
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		layout.build(widgets, this);
	}

	int inputCount() {
		return inputs.size();
	}

	EmiIngredient input(int index) {
		return index < inputs.size() ? inputs.get(index) : EmiStack.EMPTY;
	}

	EmiStack output(int index) {
		return index < outputs.size() ? outputs.get(index) : EmiStack.EMPTY;
	}

	EmiIngredient catalyst(int index) {
		return index < catalysts.size() ? catalysts.get(index) : EmiStack.EMPTY;
	}

	EmiIngredient extra(int index) {
		return index < extras.size() ? extras.get(index) : EmiStack.EMPTY;
	}

	List<Text> tooltipLines() {
		return tooltip;
	}
}
