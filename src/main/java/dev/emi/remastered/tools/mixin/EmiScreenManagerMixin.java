package dev.emi.remastered.tools.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.remastered.tools.client.ToolsClient;

/**
 * Hooks the three places where EMI decides what a key or mouse press does to the hovered thing: a hovered recipe (copy
 * recipe), a hovered ingredient and "nothing hovered" (tag browser). The hooks run before EMI's own handling and do
 * nothing while the {@code tools} module is off.
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public abstract class EmiScreenManagerMixin {

	@Inject(method = "recipeInteraction", at = @At("HEAD"), cancellable = true, remap = false)
	private static void emiRemastered$toolsRecipe(EmiRecipe recipe, Function<EmiBind, Boolean> function,
			CallbackInfoReturnable<Boolean> cir) {
		if (ToolsClient.onRecipeInteraction(recipe, function)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "stackInteraction", at = @At("HEAD"), cancellable = true, remap = false)
	private static void emiRemastered$toolsStack(EmiStackInteraction stack, Function<EmiBind, Boolean> function,
			CallbackInfoReturnable<Boolean> cir) {
		if (stack != null && ToolsClient.onStackInteraction(stack.getStack(), function)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "genericInteraction", at = @At("HEAD"), cancellable = true, remap = false)
	private static void emiRemastered$toolsGeneric(Function<EmiBind, Boolean> function, CallbackInfoReturnable<Boolean> cir) {
		if (ToolsClient.onGenericInteraction(function)) {
			cir.setReturnValue(true);
		}
	}
}
