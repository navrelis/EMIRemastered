package dev.emi.remastered.loot.mixin;

import net.minecraft.item.Item;
import net.minecraft.loot.function.SetItemLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetItemLootFunction.class)
public interface SetItemLootFunctionAccessor {
	@Accessor("item")
	RegistryEntry<Item> emi_loot$getItem();
}