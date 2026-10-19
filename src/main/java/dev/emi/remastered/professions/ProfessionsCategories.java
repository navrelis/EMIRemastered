package dev.emi.remastered.professions;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/**
 * Client only, keep out of {@link ProfessionsContent#init()}.
 */
public class ProfessionsCategories {
	public static final String NAMESPACE = "emiprofessions";
	public static final EmiTexture PROFESSION_ICON = new EmiTexture(Identifier.of(NAMESPACE, "textures/gui/profession_icon.png"), 0, 0, 16, 16);
	public static final EmiRecipeCategory PROFESSION = new EmiRecipeCategory(Identifier.of(NAMESPACE, "professions"), PROFESSION_ICON);
	public static final EmiRecipeCategory VILLAGER_TRADES = new EmiRecipeCategory(Identifier.of(NAMESPACE, "villager_trades"), EmiStack.of(Items.EMERALD));
	public static final EmiRecipeCategory WANDERING_TRADER = new EmiRecipeCategory(Identifier.of(NAMESPACE, "wandering_trader"), EmiStack.of(Items.WANDERING_TRADER_SPAWN_EGG));
}
