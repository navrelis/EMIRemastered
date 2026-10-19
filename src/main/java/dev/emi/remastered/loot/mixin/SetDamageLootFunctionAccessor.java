package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetDamageLootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetDamageLootFunction.class)
public interface SetDamageLootFunctionAccessor {

    @Accessor("durabilityRange")
    LootNumberProvider emi_loot$getDurabilityRange();

    @Accessor("add")
    boolean emi_loot$getAdd();

}
