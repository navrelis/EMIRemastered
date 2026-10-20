package dev.emi.remastered.info.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.remastered.info.client.hub.Hub;
import dev.emi.remastered.info.client.hub.HubRules;

/**
 * EMI's "view recipes" and "view uses" binds on a stack open the item view on the tab of the
 * {@code info.primary-view} setting, or on Info when their own tab would be empty (EMI then did nothing). Injected
 * right before EMI's own {@code displayRecipes}/{@code displayUses} call, so every bind EMI checks first (cheats,
 * crafting, hiding) keeps working.
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerHubMixin {

	@Inject(method = "stackInteraction", cancellable = true, at = @At(value = "INVOKE",
		target = "Ldev/emi/emi/api/EmiApi;displayRecipes(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
	private static void emiInfo$viewRecipes(EmiStackInteraction stack, Function<EmiBind, Boolean> function,
			CallbackInfoReturnable<Boolean> cir) {
		if (Hub.onViewBind(stack.getStack(), HubRules.Bind.VIEW_RECIPES)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "stackInteraction", cancellable = true, at = @At(value = "INVOKE",
		target = "Ldev/emi/emi/api/EmiApi;displayUses(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
	private static void emiInfo$viewUses(EmiStackInteraction stack, Function<EmiBind, Boolean> function,
			CallbackInfoReturnable<Boolean> cir) {
		if (Hub.onViewBind(stack.getStack(), HubRules.Bind.VIEW_USES)) {
			cir.setReturnValue(true);
		}
	}
}
