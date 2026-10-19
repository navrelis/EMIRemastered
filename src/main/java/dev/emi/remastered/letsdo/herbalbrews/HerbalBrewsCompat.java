package dev.emi.remastered.letsdo.herbalbrews;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.letsdo.LetsDoContext;
import dev.emi.remastered.letsdo.LetsDoLayouts;
import dev.emi.remastered.letsdo.LetsDoRecipe;
import dev.emi.remastered.letsdo.LetsDoSubModule;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.satisfy.herbalbrews.core.recipe.TeaKettleRecipe;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import net.satisfy.herbalbrews.core.registry.RecipeTypeRegistry;
import net.satisfy.herbalbrews.core.registry.TagsRegistry;

/** HerbalBrews 1.1.4: cauldron brewing and the tea kettles. The mod has no EMI plugin of its own. */
public class HerbalBrewsCompat implements LetsDoSubModule {

	@Override
	public void register(LetsDoContext ctx) {
		ctx.run("cauldron_brewing", () -> cauldron(ctx));
		ctx.run("cooking", () -> teaKettle(ctx));
	}

	private static void cauldron(LetsDoContext ctx) {
		EmiStack cauldron = EmiStack.of(ObjectRegistry.CAULDRON.get());
		var category = ctx.addCategory(Identifier.of("herbalbrews", "cauldron_brewing"), cauldron, cauldron);
		List<EmiIngredient> infusion = List.of(EmiStack.of(ObjectRegistry.HERBAL_INFUSION.get()));
		ctx.addRecipes(RecipeTypeRegistry.CAULDRON_RECIPE_TYPE.get(), (id, recipe) -> new LetsDoRecipe(category, id,
				LetsDoLayouts.CAULDRON_BREWING, LetsDoContext.ingredients(recipe), LetsDoContext.outputs(recipe), List.of(), infusion));
	}

	private static void teaKettle(LetsDoContext ctx) {
		EmiStack kettle = EmiStack.of(ObjectRegistry.TEA_KETTLE.get());
		var category = ctx.addCategory(Identifier.of("herbalbrews", "cooking"), kettle, kettle,
				EmiStack.of(ObjectRegistry.COPPER_TEA_KETTLE.get()));
		List<EmiIngredient> extras = List.of(
				LetsDoContext.tag(TagsRegistry.CONTAINER_ITEMS),
				EmiIngredient.of(List.of(LetsDoContext.tag(TagsRegistry.SMALL_WATER_FILL), LetsDoContext.tag(TagsRegistry.LARGE_WATER_FILL))),
				LetsDoContext.tag(TagsRegistry.HEAT_ITEMS));
		ctx.addRecipes(RecipeTypeRegistry.TEA_KETTLE_RECIPE_TYPE.get(), (id, recipe) ->
				new LetsDoRecipe(category, id, LetsDoLayouts.TEA_KETTLE, withoutContainers(recipe), LetsDoContext.outputs(recipe), List.of(), extras));
	}

	/** The bottle is shown in its own slot, so containers are not listed among the ingredients. */
	private static List<EmiIngredient> withoutContainers(TeaKettleRecipe recipe) {
		List<EmiIngredient> inputs = new ArrayList<>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			boolean container = false;
			for (ItemStack stack : ingredient.getMatchingStacks()) {
				if (stack.isIn(TagsRegistry.CONTAINER_ITEMS)) {
					container = true;
					break;
				}
			}
			if (!container) {
				inputs.add(EmiIngredient.of(ingredient));
			}
		}
		return inputs;
	}
}
