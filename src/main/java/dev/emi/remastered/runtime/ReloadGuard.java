package dev.emi.remastered.runtime;

import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.MinecraftClient;

/**
 * Cheap checks for work that runs during or after an EMI reload (client only). Once the world or the connection is
 * gone every world or registry access throws, so long loops should stop instead of logging one exception per entry.
 */
public final class ReloadGuard {

	private ReloadGuard() {
	}

	/** True when there is no client world or no play connection (kicked, disconnected, quitting, reconfiguring). */
	public static boolean isWorldGone() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client == null || client.world == null || client.getNetworkHandler() == null;
	}

	/**
	 * True when the running EMI reload is obsolete: the world is gone, a restart or clear was requested, or the
	 * current thread was interrupted (EMI interrupts its reload thread on disconnect).
	 */
	public static boolean shouldAbort() {
		return isWorldGone() || EmiReloadManager.isRestartRequested() || Thread.currentThread().isInterrupted();
	}
}
