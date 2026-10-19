package dev.emi.remastered.enchanting;

import java.util.List;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.remastered.enchanting.recipe.EnchantmentCatalog;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.recipe.EnchantmentRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.featuretoggle.FeatureFlags;

/**
 * EMI Enchanting 0.1.2 (fzzyhmstrs, MIT): an info recipe for every enchantment with its books, the items it applies
 * to, its conflicts and flags such as treasure, curse and villager trades.
 * <p>
 * Nothing is kept between EMI reloads: every reload rebuilds the recipes from the registries of the current world.
 */
public final class EnchantingContent {
	/** Namespace of the old add-on; recipe and category ids and the resource paths stay in it. */
	public static final String NAMESPACE = "emi_enchanting";

	private EnchantingContent() {
	}

	/** Both sides, always. Nothing to set up: the module is client side only. */
	public static void init() {
	}

	/** Client, only when the module is enabled at startup. Nothing to set up. */
	public static void initClient() {
	}

	/** EMI registration, called on every EMI reload while the module is enabled. */
	public static void register(EmiRegistry registry) {
		registry.addCategory(EnchantmentRecipe.CATEGORY);
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			return;
		}
		long start = System.currentTimeMillis();
		List<EnchantmentInfo> infos = EnchantmentCatalog.build(world.getRegistryManager(),
			world.getEnabledFeatures().contains(FeatureFlags.TRADE_REBALANCE));
		int added = 0;
		for (EnchantmentInfo info : infos) {
			try {
				registry.addRecipe(new EnchantmentRecipe(info));
				added++;
			} catch (Throwable t) {
				EmiReloadLog.warn("EMI Enchanting: could not add the recipe of " + info.id(), t);
			}
		}
		EmiLog.info("EMI Enchanting: " + added + " enchantment info recipes in " + (System.currentTimeMillis() - start) + "ms");
	}
}
