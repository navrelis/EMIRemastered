package dev.emi.remastered.info.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.remastered.info.client.InfoScreens;

/**
 * The "open item info" bind: EMI routes every key and mouse bind on a hovered stack (index, favourites, inventory
 * slots, recipe screen slots, the Item Info screen itself) through {@code stackInteraction}.
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerMixin {

	@Inject(method = "stackInteraction", at = @At("HEAD"), cancellable = true)
	private static void emiInfo$stackInteraction(EmiStackInteraction stack, Function<EmiBind, Boolean> function,
			CallbackInfoReturnable<Boolean> cir) {
		if (InfoScreens.onStackInteraction(stack, function)) {
			cir.setReturnValue(true);
		}
	}
}
