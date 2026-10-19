package dev.emi.remastered.loot.mixin;

import net.minecraft.item.Instrument;
import net.minecraft.loot.function.SetInstrumentLootFunction;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetInstrumentLootFunction.class)
public interface SetInstrumentLootFunctionAccessor {

    @Accessor("options")
    TagKey<Instrument> emi_loot$getInstrumentTag();

}
