package dev.emi.remastered.loot.structure;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/** Added to {@link net.minecraft.resource.ZipResourcePack} by a mixin: the zip file the pack reads. */
public interface ZipPackFile {
	@Nullable File emi_loot$getFile();
}
