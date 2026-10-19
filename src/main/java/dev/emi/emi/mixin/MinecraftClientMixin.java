package dev.emi.emi.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiPersistentData;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Shadow
	public ClientWorld world;

	@Inject(at = @At("RETURN"), method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;")
	private void reloadResources(boolean force, @Coerce Object loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> info) {
		CompletableFuture<Void> future = info.getReturnValue();
		if (future != null) {
			future.thenRunAsync(() -> {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client.world != null && client.world.getRecipeManager() != null) {
					EmiReloadManager.reload();
				}
			}, (MinecraftClient) (Object) this); // EMI Remastered: was a new, never closed thread pool per reload (KB15)
		}
	}

	// EMI Remastered: was disconnect(Screen); a kick or a lost connection calls disconnect(Screen, boolean) directly,
	// so EMI was never cleared and its reload kept running without a world (KB1, KB38)
	@Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V")
	private void disconnect(CallbackInfo info) {
		EmiLog.info("Disconnecting from server, EMI data cleared");
		EmiPersistentData.flush(); // EMI Remastered: write a pending debounced save (KB14)
		EmiReloadManager.clear();
		EmiClient.onServer = false;
	}
}
