package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.loot.provider.number.LootNumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BoundedIntUnaryOperator.class)
public interface BoundedIntUnaryOperatorAccessor {

    @Accessor("min")
    LootNumberProvider emi_loot$getMin();

    @Accessor("max")
    LootNumberProvider emi_loot$getMax();

}
