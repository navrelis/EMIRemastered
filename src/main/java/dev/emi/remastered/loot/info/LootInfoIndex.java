package dev.emi.remastered.loot.info;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.client.ClientBuiltPool;
import dev.emi.remastered.loot.emi.BlockLootRecipe;
import dev.emi.remastered.loot.emi.ChestLootRecipe;
import dev.emi.remastered.loot.emi.GameplayLootRecipe;
import dev.emi.remastered.loot.emi.LootEmiRegistration;
import dev.emi.remastered.loot.emi.MobLootRecipe;
import dev.emi.remastered.loot.util.ConditionIcons;
import dev.emi.remastered.loot.util.ConditionKind;
import dev.emi.remastered.loot.util.ConditionalStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the Item Info provider needs to know about an item, filled once per EMI reload from the loot recipes EMI
 * holds: the chest loot recipes that put the item into a chest of a known structure, and the condition kinds of its
 * block, mob and gameplay drops. Client render thread only.
 */
public final class LootInfoIndex {
	private final Map<Item, List<ChestLootRecipe>> chests = new HashMap<>();
	private final Map<Item, Map<ConditionKind, List<EmiRecipe>>> conditions = new HashMap<>();

	private LootInfoIndex() {
	}

	/** Reads the loot recipes that are registered with EMI right now. */
	public static LootInfoIndex build() {
		LootInfoIndex index = new LootInfoIndex();
		for (EmiRecipeCategory category : List.of(LootEmiRegistration.CHEST_CATEGORY, LootEmiRegistration.TRIAL_CATEGORY)) {
			for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(category)) {
				if (recipe instanceof ChestLootRecipe chest && !chest.getStructures().isEmpty()) {
					index.addChest(chest);
				}
			}
		}
		for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(LootEmiRegistration.BLOCK_CATEGORY)) {
			if (recipe instanceof BlockLootRecipe block) {
				index.addPools(block, block.getPools());
			}
		}
		for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(LootEmiRegistration.MOB_CATEGORY)) {
			if (recipe instanceof MobLootRecipe mob) {
				index.addPools(mob, mob.getPools());
			}
		}
		for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes(LootEmiRegistration.GAMEPLAY_CATEGORY)) {
			if (recipe instanceof GameplayLootRecipe gameplay) {
				index.addPools(gameplay, gameplay.getPools());
			}
		}
		return index;
	}

	private void addChest(ChestLootRecipe recipe) {
		Set<Item> seen = new LinkedHashSet<>();
		for (EmiStack output : recipe.getOutputs()) {
			if (output.getKey() instanceof Item item && seen.add(item)) {
				chests.computeIfAbsent(item, i -> new ArrayList<>()).add(recipe);
			}
		}
	}

	private void addPools(EmiRecipe recipe, List<ClientBuiltPool> pools) {
		for (ClientBuiltPool pool : pools) {
			Set<ConditionKind> kinds = new LinkedHashSet<>();
			for (Pair<Integer, Text> condition : pool.conditions()) {
				ConditionKind kind = ConditionIcons.classify(condition.getRight());
				if (kind != null) {
					kinds.add(kind);
				}
			}
			if (kinds.isEmpty()) {
				continue;
			}
			for (ConditionalStack stack : pool.stacks()) {
				for (ItemStack raw : stack.getRawStacks()) {
					Map<ConditionKind, List<EmiRecipe>> byKind = conditions.computeIfAbsent(raw.getItem(), i -> new EnumMap<>(ConditionKind.class));
					for (ConditionKind kind : kinds) {
						List<EmiRecipe> list = byKind.computeIfAbsent(kind, k -> new ArrayList<>());
						if (!list.contains(recipe)) {
							list.add(recipe);
						}
					}
				}
			}
		}
	}

	/** Chest loot recipes (of chests found in a known structure) that can hold the item. */
	public List<ChestLootRecipe> chestsWith(Item item) {
		return chests.getOrDefault(item, List.of());
	}

	/** Condition kinds of the drops of the item, with the recipes that have a drop of that kind. */
	public Map<ConditionKind, List<EmiRecipe>> conditionsOf(Item item) {
		return conditions.getOrDefault(item, Map.of());
	}
}
