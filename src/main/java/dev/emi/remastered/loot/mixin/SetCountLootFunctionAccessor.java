package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetCountLootFunction.class)
public interface SetCountLootFunctionAccessor {

    @Accessor("countRange")
    LootNumberProvider emi_loot$getCountRange();

    @Accessor("add")
    boolean emi_loot$getAdd();

}
