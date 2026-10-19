package dev.emi.remastered.devtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Mouse;

/**
 * Dev clients must never capture, hide or move the real OS cursor (several clients run side by side on the
 * developer's PC). The cursor is simply never locked; tours inject input through the screen handlers instead.
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
	private void emiDevtour$noLock(CallbackInfo info) {
		info.cancel();
	}

	@Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true)
	private void emiDevtour$noUnlock(CallbackInfo info) {
		info.cancel();
	}
}
