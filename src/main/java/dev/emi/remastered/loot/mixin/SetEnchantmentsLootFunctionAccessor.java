package dev.emi.remastered.loot.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.loot.function.SetEnchantmentsLootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SetEnchantmentsLootFunction.class)
public interface SetEnchantmentsLootFunctionAccessor {

    @Accessor("enchantments")
    Map<RegistryEntry<Enchantment>, LootNumberProvider> emi_loot$getEnchantments();

    @Accessor("add")
    boolean emi_loot$getAdd();

}