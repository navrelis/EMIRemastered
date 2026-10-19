package dev.emi.remastered.info.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.remastered.info.client.InfoScreen;
import dev.emi.remastered.info.client.InfoScreens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/**
 * Lets EMI treat the Item Info screen like its own recipe screen: {@code getHandledScreen} returns the handled screen
 * below it, and showing recipes from it pushes it onto EMI's history (so EMI's back key returns to it).
 */
@Mixin(value = EmiApi.class, remap = false)
public class EmiApiMixin {

	@Inject(method = "getHandledScreen", at = @At("HEAD"), cancellable = true)
	private static void emiInfo$getHandledScreen(CallbackInfoReturnable<HandledScreen<?>> cir) {
		if (MinecraftClient.getInstance().currentScreen instanceof InfoScreen info) {
			cir.setReturnValue(info.old);
		}
	}

	@Inject(method = "setPages", at = @At("HEAD"), cancellable = true)
	private static void emiInfo$setPages(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient stack, CallbackInfo ci) {
		if (MinecraftClient.getInstance().currentScreen instanceof InfoScreen) {
			InfoScreens.showRecipes(recipes, stack);
			ci.cancel();
		}
	}
}
