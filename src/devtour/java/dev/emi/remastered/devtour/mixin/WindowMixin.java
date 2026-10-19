package dev.emi.remastered.devtour.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.remastered.devtour.DevWindow;
import net.minecraft.client.util.Window;

/**
 * Dev clients never take the keyboard focus from whatever the developer is working in and never cover the screen:
 * the window is created hidden (or, with -Demi.devtour.visible=true, shown without focus and without activation),
 * never as a fullscreen window, and fullscreen is never entered later.
 */
@Mixin(Window.class)
public abstract class WindowMixin {
	@Shadow
	private boolean fullscreen;
	@Shadow
	private boolean currentFullscreen;
	@Shadow
	@Final
	private long handle;

	@Inject(method = "<init>", at = @At(value = "INVOKE",
		target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
	private void emiDevtour$noFocus(CallbackInfo info) {
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, DevWindow.VISIBLE ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
		this.fullscreen = false;
	}

	/** A window created on a monitor (--fullscreen) is always shown and focused by GLFW: always create it windowed. */
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false), index = 3)
	private long emiDevtour$noMonitor(long monitor) {
		return 0L;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void emiDevtour$created(CallbackInfo info) {
		DevWindow.created(this.handle);
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
