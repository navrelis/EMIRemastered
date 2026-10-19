package dev.emi.remastered.loot.mixin;

import net.minecraft.item.map.MapDecorationType;
import net.minecraft.loot.function.ExplorationMapLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplorationMapLootFunction.class)
public interface ExplorationMapLootFunctionAccessor {

    @Accessor("decoration")
    RegistryEntry<MapDecorationType> emi_loot$getDecoration();

    @Accessor("destination")
    TagKey<Structure> emi_loot$getDestination();

}