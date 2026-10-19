package dev.emi.remastered.loot.mixin;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    List<LootPoolEntry> emi_loot$getEntries();

    @Accessor("conditions")
    List<LootCondition> emi_loot$getConditions();

    @Accessor("functions")
    List<LootFunction> emi_loot$getFunctions();

    @Accessor("rolls")
    LootNumberProvider emi_loot$getRolls();
}
