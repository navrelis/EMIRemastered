package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.util.ConditionalStack;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.List;

public record ClientBuiltPool(List<Pair<Integer, Text>> conditions, List<ConditionalStack> stacks){}