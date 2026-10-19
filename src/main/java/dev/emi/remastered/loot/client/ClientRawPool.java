package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.util.TextKey;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

public record ClientRawPool(Map<List<TextKey>, Object2FloatMap<ItemStack>> map) {
}
