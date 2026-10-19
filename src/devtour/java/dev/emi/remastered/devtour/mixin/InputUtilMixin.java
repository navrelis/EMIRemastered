package dev.emi.remastered.devtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.util.InputUtil;

/** The only place that calls glfwSetCursorPos / glfwSetInputMode(GLFW_CURSOR): dev clients never do either. */
@Mixin(InputUtil.class)
public abstract class InputUtilMixin {
	@Inject(method = "setCursorParameters", at = @At("HEAD"), cancellable = true)
	private static void emiDevtour$noCursorParameters(long handler, int inputModeValue, double x, double y, CallbackInfo info) {
		info.cancel();
	}
}
