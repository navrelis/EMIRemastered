package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.condition.AlternativeLootCondition;
import net.minecraft.loot.condition.LootCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AlternativeLootCondition.class)
public interface AlternativeLootConditionAccessor {

    @Accessor("terms")
    List<LootCondition> emi_loot$getConditions();

}
