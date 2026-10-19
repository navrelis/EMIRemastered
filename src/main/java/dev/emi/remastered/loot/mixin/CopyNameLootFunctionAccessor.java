package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.CopyNameLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CopyNameLootFunction.class)
public interface CopyNameLootFunctionAccessor {

    @Accessor("source")
    CopyNameLootFunction.Source emi_loot$getSource();

}
