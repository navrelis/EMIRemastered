package dev.emi.remastered.tools;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.input.EmiInput;

public class ToolsSettings {

	@Comment("Opens the tag browser. While hovering a tag ingredient it opens at that tag, while hovering an item it shows the tags of the item.")
	@ConfigValue("tools.open-tag-browser")
	public static EmiBind openTagBrowser = new EmiBind("config.emi.tools.open_tag_browser", GLFW.GLFW_KEY_T);

	@Comment("Copies the hovered recipe of EMI's recipe screen to the clipboard. Hold shift to copy the other format.")
	@ConfigValue("tools.copy-recipe")
	public static EmiBind copyRecipe = new EmiBind("config.emi.tools.copy_recipe", EmiInput.CONTROL_MASK, GLFW.GLFW_KEY_C);

	@Comment("Format the copy recipe key copies: readable text or JSON (vanilla recipe JSON when EMI knows the vanilla recipe).")
	@ConfigValue("tools.copy-recipe-format")
	public static RecipeFormat copyRecipeFormat = RecipeFormat.TEXT;
}
