package dev.emi.remastered.loot.mixin;

import net.minecraft.component.ComponentChanges;
import net.minecraft.loot.function.SetComponentsLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetComponentsLootFunction.class)
public interface SetComponentsLootFunctionAccessor {
	@Accessor("changes")
	ComponentChanges emi_loot$getChanges();
}