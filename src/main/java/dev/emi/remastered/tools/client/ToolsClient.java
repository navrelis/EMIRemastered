package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.input.EmiInput;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.remastered.tools.RecipeFormat;
import dev.emi.remastered.tools.ToolsModule;
import dev.emi.remastered.tools.ToolsSettings;
import dev.emi.remastered.tools.client.TagBrowserScreen.Target;
import dev.emi.remastered.tools.tags.TagCatalog;
import dev.emi.remastered.tools.tags.TagFilter.MemberRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluid;

/**
 * Client entry points of the {@code tools} module, called by
 * {@link dev.emi.remastered.tools.mixin.EmiScreenManagerMixin} from EMI's input handling. Every entry point does
 * nothing while the module switch is off.
 */
public final class ToolsClient {
	private ToolsClient() {
	}

	/**
	 * EMI offers a hovered recipe to {@code function}, which tells whether a given bind is pressed. Key binds are ignored
	 * while a text field is being typed into, so Ctrl+C in a text field stays a normal copy.
	 */
	public static boolean onRecipeInteraction(@Nullable EmiRecipe recipe, Function<EmiBind, Boolean> function) {
		if (!ToolsModule.enabled || recipe == null) {
			return false;
		}
		function = TextInput.guard(function);
		RecipeFormat format = ToolsSettings.copyRecipeFormat;
		if (function.apply(ToolsSettings.copyRecipe)) {
			RecipeClipboard.copy(recipe, format);
			return true;
		}
		EmiBind shifted = withShift(ToolsSettings.copyRecipe);
		if (shifted != null && function.apply(shifted)) {
			RecipeClipboard.copy(recipe, format.other());
			return true;
		}
		return false;
	}

	/** EMI offers a hovered ingredient (never empty-checked by EMI) to {@code function}. */
	public static boolean onStackInteraction(EmiIngredient ingredient, Function<EmiBind, Boolean> function) {
		if (!ToolsModule.enabled || ingredient == null || ingredient.isEmpty()) {
			return false;
		}
		if (TextInput.guard(function).apply(ToolsSettings.openTagBrowser)) {
			openTagBrowser(ingredient);
			return true;
		}
		return false;
	}

	/** EMI offers the plain "nothing hovered" interaction to {@code function}. */
	public static boolean onGenericInteraction(Function<EmiBind, Boolean> function) {
		if (!ToolsModule.enabled) {
			return false;
		}
		if (TextInput.guard(function).apply(ToolsSettings.openTagBrowser)) {
			openTagBrowser(null);
			return true;
		}
		return false;
	}

	/**
	 * Opens the tag browser over the current screen.
	 *
	 * @param hovered a tag ingredient opens the browser at that tag, any other ingredient lists the tags that contain it,
	 *                null shows all tags
	 */
	public static void openTagBrowser(@Nullable EmiIngredient hovered) {
		MinecraftClient client = MinecraftClient.getInstance();
		TagCatalog catalog = TagIndex.get();
		if (catalog == null) {
			return;
		}
		client.setScreen(new TagBrowserScreen(client.currentScreen, catalog, targetOf(hovered)));
	}

	static Target targetOf(@Nullable EmiIngredient hovered) {
		if (hovered instanceof EmiFavorite favorite) {
			hovered = favorite.getStack();
		}
		if (hovered == null || hovered.isEmpty()) {
			return Target.NONE;
		}
		if (hovered instanceof TagEmiIngredient tag) {
			return Target.tag(tag.key.registry().getValue().toString(), tag.key.id().toString());
		}
		EmiStack first = hovered.getEmiStacks().get(0);
		String registry = first.getKey() instanceof Fluid ? "minecraft:fluid" : null;
		return Target.member(new MemberRef(registry, first.getId().toString()));
	}

	/** The bind with shift added to every bound key, or null when it is unbound or already uses shift. */
	private static @Nullable EmiBind withShift(EmiBind bind) {
		List<EmiBind.ModifiedKey> keys = new ArrayList<>();
		for (EmiBind.ModifiedKey key : bind.boundKeys) {
			if (!key.isUnbound() && (key.modifiers() & EmiInput.SHIFT_MASK) == 0) {
				keys.add(new EmiBind.ModifiedKey(key.key(), key.modifiers() | EmiInput.SHIFT_MASK));
			}
		}
		return keys.isEmpty() ? null : new EmiBind(bind.translationKey, keys.toArray(new EmiBind.ModifiedKey[0]));
	}
}
