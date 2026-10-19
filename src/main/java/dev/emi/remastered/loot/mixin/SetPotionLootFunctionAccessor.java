package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetPotionLootFunction;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetPotionLootFunction.class)
public interface SetPotionLootFunctionAccessor {

    @Accessor("potion")
    RegistryEntry<Potion> emi_loot$getPotion();

}
