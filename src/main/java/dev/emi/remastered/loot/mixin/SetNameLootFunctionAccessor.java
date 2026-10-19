package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.function.SetNameLootFunction;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(SetNameLootFunction.class)
public interface SetNameLootFunctionAccessor {

    @Accessor("name")
    Optional<Text> emi_loot$getName();

}
