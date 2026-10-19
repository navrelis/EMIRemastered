package dev.emi.remastered.devtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;

/**
 * An unfocused dev client must keep running instead of opening the game menu (GameRenderer#render does that when
 * the window has lost focus and pauseOnLostFocus is set). prepareDevOptions also writes pauseOnLostFocus:false; this
 * makes it independent of the options file.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Redirect(method = "render", at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/option/GameOptions;pauseOnLostFocus:Z"))
	private boolean emiDevtour$neverPauseOnLostFocus(GameOptions options) {
		return false;
	}
}
