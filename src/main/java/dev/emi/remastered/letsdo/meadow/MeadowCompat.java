package dev.emi.remastered.letsdo.meadow;

import java.util.List;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.letsdo.LetsDoContext;
import dev.emi.remastered.letsdo.LetsDoLayouts;
import dev.emi.remastered.letsdo.LetsDoRecipe;
import dev.emi.remastered.letsdo.LetsDoSubModule;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.satisfy.meadow.core.registry.ObjectRegistry;
import net.satisfy.meadow.core.registry.RecipeRegistry;
import net.satisfy.meadow.core.registry.TagRegistry;

/**
 * Meadow 1.4.9: cheese forming, cooking cauldron and woodcutter (data pack recipes) and the two fondue pot
 * interactions (fixed rules of the block, so the recipes are generated here).
 */
public class MeadowCompat implements LetsDoSubModule {

	@Override
	public void register(LetsDoContext ctx) {
		ctx.run("fondue_fill", () -> fondueFill(ctx));
		ctx.run("fondue_use", () -> fondueUse(ctx));
		ctx.run("cheese", () -> cheese(ctx));
		ctx.run("cooking", () -> cooking(ctx));
		ctx.run("woodcutting", () -> woodcutting(ctx));
	}

	/** FonduePotBlock: a cheese wheel fills the pot to 3/3, other cheese adds 1/3. */
	private static void fondueFill(LetsDoContext ctx) {
		EmiStack pot = EmiStack.of(ObjectRegistry.FONDUE_POT.get());
		var category = ctx.addCategory(LetsDoContext.id("fondue_fill"), pot, pot);
		ctx.addRecipe(new LetsDoRecipe(category, LetsDoContext.syntheticId("fondue_fill", "cheese_wheels"),
				LetsDoLayouts.FILL_BLOCK, List.of(LetsDoContext.tag(TagRegistry.CHEESE_WHEELS)), List.of(), List.of(pot), List.of())
				.tooltip(Text.translatable("emi_letsdo_compat.fondue_fill.full"), Text.translatable("emi_letsdo_compat.fondue.heat"))
				.recipeTree(false));
		ctx.addRecipe(new LetsDoRecipe(category, LetsDoContext.syntheticId("fondue_fill", "cheese"),
				LetsDoLayouts.FILL_BLOCK, List.of(LetsDoContext.tag(TagRegistry.CHEESE)), List.of(), List.of(pot), List.of())
				.tooltip(Text.translatable("emi_letsdo_compat.fondue_fill.part"), Text.translatable("emi_letsdo_compat.fondue.heat"))
				.recipeTree(false));
	}

	/** FonduePotBlock: bread takes 1/3 of the cheese and becomes a cheese stick. */
	private static void fondueUse(LetsDoContext ctx) {
		EmiStack pot = EmiStack.of(ObjectRegistry.FONDUE_POT.get());
		var category = ctx.addCategory(LetsDoContext.id("fondue_use"), pot, pot);
		EmiStack stick = EmiStack.of(ObjectRegistry.CHEESE_STICK.get());
		ctx.addRecipe(new LetsDoRecipe(category, LetsDoContext.syntheticId("fondue_use", "bread"),
				LetsDoLayouts.USE_ON_BLOCK, List.of(LetsDoContext.tag(TagRegistry.BREAD)), List.of(stick), List.of(pot), List.of())
				.tooltip(Text.translatable("emi_letsdo_compat.fondue_use.portion"), Text.translatable("emi_letsdo_compat.fondue.heat")));
	}

	private static void cheese(LetsDoContext ctx) {
		EmiStack form = EmiStack.of(ObjectRegistry.CHEESE_FORM.get());
		var category = ctx.addCategory(Identifier.of("meadow", "cheese"), form, form);
		ctx.addRecipes(RecipeRegistry.CHEESE.get(), (id, recipe) -> LetsDoRecipe.fromRecipe(category, id, LetsDoLayouts.CHEESE_FORM, recipe));
	}

	private static void cooking(LetsDoContext ctx) {
		EmiStack cauldron = EmiStack.of(ObjectRegistry.COOKING_CAULDRON.get());
		var category = ctx.addCategory(Identifier.of("meadow", "cooking"), cauldron, cauldron);
		ctx.addRecipes(RecipeRegistry.COOKING.get(), (id, recipe) -> LetsDoRecipe.fromRecipe(category, id, LetsDoLayouts.COOKING_CAULDRON, recipe));
	}

	private static void woodcutting(LetsDoContext ctx) {
		EmiStack woodcutter = EmiStack.of(ObjectRegistry.WOODCUTTER.get());
		var category = ctx.addCategory(Identifier.of("meadow", "woodcutting"), woodcutter, woodcutter);
		ctx.addRecipes(RecipeRegistry.WOODCUTTING.get(), (id, recipe) -> LetsDoRecipe.fromRecipe(category, id, LetsDoLayouts.WOODCUTTING, recipe));
	}
}
