package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetStewEffectLootFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SetStewEffectLootFunction.class)
public interface SetStewEffectLootFunctionAccessor {

    @Accessor("stewEffects")
    List<SetStewEffectLootFunction.StewEffect> emi_loot$getEffects();

}
