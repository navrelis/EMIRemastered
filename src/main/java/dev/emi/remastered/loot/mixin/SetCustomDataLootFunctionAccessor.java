package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetCustomDataLootFunction;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetCustomDataLootFunction.class)
public interface SetCustomDataLootFunctionAccessor {
	@Accessor("nbt")
	NbtCompound emi_loot$getNbt();
}