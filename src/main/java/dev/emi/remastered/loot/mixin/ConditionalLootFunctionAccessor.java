package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.function.ConditionalLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ConditionalLootFunction.class)
public interface ConditionalLootFunctionAccessor {

    @Accessor("conditions")
    List<LootCondition> emi_loot$getConditions();

}
