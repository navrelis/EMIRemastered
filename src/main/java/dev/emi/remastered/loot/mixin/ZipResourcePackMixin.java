package dev.emi.remastered.loot.mixin;

import dev.emi.remastered.loot.structure.ZipPackFile;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ZipResourcePack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/** Keeps the file of a zip pack (its wrapper class is package-private) for the structure lookup cache's fingerprint. */
@Mixin(ZipResourcePack.class)
public abstract class ZipResourcePackMixin implements ZipPackFile {
	@Unique
	private @Nullable File emi_loot$file;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void emi_loot$keepFile(ResourcePackInfo info, @Coerce Object zipFile, String overlay, CallbackInfo ci) {
		emi_loot$file = ((ZipFileWrapperAccessor) zipFile).emi_loot$getFile();
	}

	@Override
	public @Nullable File emi_loot$getFile() {
		return emi_loot$file;
	}
}
