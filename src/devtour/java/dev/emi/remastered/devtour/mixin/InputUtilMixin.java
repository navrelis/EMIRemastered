package dev.emi.remastered.devtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.util.InputUtil;

/**
 * The only vanilla callers of glfwSetCursorPos and glfwSetInputMode: dev clients never move the OS cursor, never
 * change its mode (hidden / captured) and never switch raw mouse motion.
 */
@Mixin(InputUtil.class)
public abstract class InputUtilMixin {
	@Inject(method = "setCursorParameters", at = @At("HEAD"), cancellable = true)
	private static void emiDevtour$noCursorParameters(long handler, int inputModeValue, double x, double y, CallbackInfo info) {
		info.cancel();
	}

	@Inject(method = "setRawMouseMotionMode", at = @At("HEAD"), cancellable = true)
	private static void emiDevtour$noRawMouseMotion(long handler, boolean value, CallbackInfo info) {
		info.cancel();
	}
}
