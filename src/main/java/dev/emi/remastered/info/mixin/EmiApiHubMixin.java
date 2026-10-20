package dev.emi.remastered.info.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.remastered.info.client.hub.Hub;

/**
 * Tells the item view which stack a recipe screen shows: {@code displayRecipes}/{@code displayUses} of one stack
 * (from EMI's binds or any other mod) give the screen they create the tab bar.
 */
@Mixin(value = EmiApi.class, remap = false)
public class EmiApiHubMixin {

	@Inject(method = "displayRecipes", at = @At("HEAD"))
	private static void emiInfo$beginRecipes(EmiIngredient stack, CallbackInfo ci) {
		Hub.beginDisplay(stack, false);
	}

	@Inject(method = "displayUses", at = @At("HEAD"))
	private static void emiInfo$beginUses(EmiIngredient stack, CallbackInfo ci) {
		Hub.beginDisplay(stack, true);
	}

	@Inject(method = { "displayRecipes", "displayUses" }, at = @At("RETURN"))
	private static void emiInfo$end(EmiIngredient stack, CallbackInfo ci) {
		Hub.endDisplay();
	}
}
