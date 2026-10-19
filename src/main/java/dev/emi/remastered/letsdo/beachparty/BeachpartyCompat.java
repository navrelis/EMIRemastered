package dev.emi.remastered.letsdo.beachparty;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.letsdo.LetsDoContext;
import dev.emi.remastered.letsdo.LetsDoLayouts;
import dev.emi.remastered.letsdo.LetsDoRecipe;
import dev.emi.remastered.letsdo.LetsDoSubModule;
import net.minecraft.util.Identifier;
import net.satisfy.beachparty.core.registry.ObjectRegistry;
import net.satisfy.beachparty.core.registry.RecipeTypeRegistry;

/** Beach Party 2.1.5: mini fridge and tiki bar (palm bar). The mod has no EMI plugin of its own. */
public class BeachpartyCompat implements LetsDoSubModule {

	@Override
	public void register(LetsDoContext ctx) {
		ctx.run("mini_fridge_mixing", () -> {
			EmiStack fridge = EmiStack.of(ObjectRegistry.MINI_FRIDGE.get());
			var category = ctx.addCategory(Identifier.of("beachparty", "mini_fridge_mixing"), fridge, fridge);
			ctx.addRecipes(RecipeTypeRegistry.MINI_FRIDGE_RECIPE_TYPE.get(),
					(id, recipe) -> LetsDoRecipe.fromRecipe(category, id, LetsDoLayouts.MINI_FRIDGE, recipe));
		});
		ctx.run("tiki_bar_mixing", () -> {
			EmiStack bar = EmiStack.of(ObjectRegistry.PALM_BAR.get());
			var category = ctx.addCategory(Identifier.of("beachparty", "tiki_bar_mixing"), bar, bar);
			ctx.addRecipes(RecipeTypeRegistry.PALM_BAR_RECIPE_TYPE.get(),
					(id, recipe) -> LetsDoRecipe.fromRecipe(category, id, LetsDoLayouts.PALM_BAR, recipe));
		});
	}
}
