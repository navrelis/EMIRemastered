package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.LimitCountLootFunction;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimitCountLootFunction.class)
public interface LimitCountLootFunctionAccessor {

    @Accessor("limit")
    BoundedIntUnaryOperator emi_loot$getLimit();

}