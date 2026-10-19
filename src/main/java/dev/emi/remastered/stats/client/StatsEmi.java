package dev.emi.remastered.stats.client;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.stats.StatsSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Client side of the {@code stats} module: the EMI registration. Never loaded on a dedicated server. */
public final class StatsEmi {
	/** The Food category; only registered while {@code stats.food-category} is on. */
	public static final EmiRecipeCategory FOOD = new EmiRecipeCategory(Identifier.of("emi_stats", "food"),
		EmiStack.of(Items.COOKED_BEEF), EmiStack.of(Items.COOKED_BEEF), FoodRecipe.SORT);

	private StatsEmi() {
	}

	/** Whether the Food category exists in the current EMI reload (for links from the Item Info page). */
	public static volatile boolean foodCategoryRegistered = false;

	public static void register(EmiRegistry registry) {
		long start = System.nanoTime();
		ItemInfoRegistry.register(new StatsInfoProvider());

		int foods = 0;
		foodCategoryRegistered = false;
		if (StatsSettings.foodCategory) {
			registry.addCategory(FOOD);
			for (Item item : Registries.ITEM) {
				ItemStack stack = item.getDefaultStack();
				if (stack.isEmpty() || !stack.contains(DataComponentTypes.FOOD)) {
					continue;
				}
				FoodFacts facts = FoodFacts.of(stack);
				if (facts != null) {
					registry.addRecipe(new FoodRecipe(EmiStack.of(item), Registries.ITEM.getId(item), facts));
					foods++;
				}
			}
			foodCategoryRegistered = true;
		}
		EmiLog.info("stats: " + foods + " food recipes in " + (System.nanoTime() - start) / 1_000_000 + " ms");
	}
}
