package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.LootTable;
import net.minecraft.loot.function.SetLootTableLootFunction;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetLootTableLootFunction.class)
public interface SetLootTableLootFunctionAccessor {

    @Accessor("lootTable")
    RegistryKey<LootTable> emi_loot$getLootTable();

}