package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.ModifyContentsLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModifyContentsLootFunction.class)
public interface ModifyContentsLootFunctionAccessor {
	@Accessor("modifier")
	LootFunction emi_loot$getModifier();
}