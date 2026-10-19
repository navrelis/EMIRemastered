package dev.emi.remastered.devtour;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.bom.MaterialNode;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/**
 * Dev tour helpers for KB7 (recipe tree remainders), called by the tour step {@code call} (public static, no
 * arguments). Dev only, never packaged.
 */
public final class Kb7Tour {
	private Kb7Tour() {
	}

	private static String goal(String id) {
		EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(Identifier.of(id));
		if (recipe == null) {
			throw new IllegalStateException("no EMI recipe " + id);
		}
		BoM.setGoal(recipe);
		EmiRecipe sticks = EmiApi.getRecipeManager().getRecipe(Identifier.of("minecraft:stick"));
		if (sticks == null) {
			throw new IllegalStateException("no EMI recipe minecraft:stick");
		}
		BoM.tree.addResolution(EmiStack.of(Items.STICK), sticks);
		BoM.craftingMode = true;
		EmiScreenManager.forceRecalculate();
		return "BoM goal " + id + " with minecraft:stick as the stick recipe, crafting mode on";
	}

	/** Activator rail: 2 sticks directly and a redstone torch (1 stick); the stick recipe makes 4 per batch. */
	public static String goalActivatorRail() {
		return goal("minecraft:activator_rail");
	}

	private static void sum(MaterialNode node, Map<String, long[]> out) {
		if (node.recipe == null) {
			return;
		}
		long[] v = out.computeIfAbsent(String.valueOf(node.recipe.getId()), k -> new long[4]);
		v[0] += node.neededBatches;
		v[1] += node.totalNeeded;
		v[2] += node.usedRemainder;
		v[3]++;
		for (MaterialNode child : node.children) {
			sum(child, out);
		}
	}

	/**
	 * Per recipe of the tree for the player's current inventory: batches, remaining amount with the old formula
	 * (sum of totalNeeded) and with the fix (plus usedRemainder), then the synthetic favourites the sidebar shows.
	 */
	public static String report() {
		MinecraftClient client = MinecraftClient.getInstance();
		BoM.tree.calculateProgress(EmiPlayerInventory.of(client.player));
		Map<String, long[]> sums = new LinkedHashMap<>();
		sum(BoM.tree.goal, sums);
		StringBuilder sb = new StringBuilder("tree:");
		sums.forEach((id, v) -> sb.append(" [").append(id).append(" nodes=").append(v[3]).append(" batches=").append(v[0])
			.append(" remaining-old=").append(v[1]).append(" remaining-fixed=").append(v[1] + v[2]).append(']'));
		sb.append(" | synthetic favourites:");
		for (EmiFavorite.Synthetic fav : EmiFavorites.syntheticFavorites) {
			sb.append(" [").append(fav.getEmiStacks().get(0).getId()).append(fav.getRecipe() == null ? " cost" : " recipe")
				.append(" shown=").append(fav.amount).append(" total=").append(fav.total).append(" batches=").append(fav.batches).append(']');
		}
		return sb.toString();
	}
}
