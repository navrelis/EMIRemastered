package dev.emi.remastered.loot.mixin;

import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(OverlayResourcePack.class)
public interface OverlayResourcePackAccessor {
	@Accessor("overlaysAndBase")
	List<ResourcePack> emi_loot$getOverlaysAndBase();
}
