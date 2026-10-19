package dev.emi.remastered.professions;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import net.minecraft.util.Identifier;

/**
 * Client only, keep out of {@link ProfessionsContent#init()}.
 */
public class ProfessionsCategories {
	public static final String NAMESPACE = "emiprofessions";
	public static final EmiTexture PROFESSION_ICON = new EmiTexture(Identifier.of(NAMESPACE, "textures/gui/profession_icon.png"), 0, 0, 16, 16);
	public static final EmiRecipeCategory PROFESSION = new EmiRecipeCategory(Identifier.of(NAMESPACE, "professions"), PROFESSION_ICON);
}
