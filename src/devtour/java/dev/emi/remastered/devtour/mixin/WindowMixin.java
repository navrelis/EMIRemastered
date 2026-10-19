package dev.emi.remastered.devtour.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.util.Window;

/**
 * Dev clients never take the keyboard focus from whatever the developer is working in and never cover the screen:
 * the window is created without input focus and shown without activating it, and fullscreen is never entered.
 */
@Mixin(Window.class)
public abstract class WindowMixin {
	@Shadow
	private boolean fullscreen;
	@Shadow
	private boolean currentFullscreen;

	@Inject(method = "<init>", at = @At(value = "INVOKE",
		target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
	private void emiDevtour$noFocus(CallbackInfo info) {
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
	}

	/** F11, the fullscreen option and the start-up fullscreen toggle. */
	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	private void emiDevtour$noToggleFullscreen(CallbackInfo info) {
		info.cancel();
	}

	/** Also covers the window being created fullscreen (--fullscreen / fullscreen option): always windowed. */
	@Inject(method = "updateWindowRegion", at = @At("HEAD"))
	private void emiDevtour$alwaysWindowed(CallbackInfo info) {
		this.fullscreen = false;
		this.currentFullscreen = false;
	}
}
