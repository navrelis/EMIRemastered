package dev.emi.remastered.devtour;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.world.emi.StructureEmiStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Dev tour helpers of the {@code world} module, called by the tour step {@code call} (public static, no arguments).
 * Biome and structure stacks are no items, so the tour cannot reach their pages through {@code emi_recipes}; these
 * methods do what clicking the stack in EMI's index does. Dev only, never packaged.
 */
public final class WorldTour {
	private WorldTour() {
	}

	private static String show(EmiStack stack) {
		MinecraftClient client = MinecraftClient.getInstance();
		Screen before = client.currentScreen;
		EmiApi.displayRecipes(stack);
		Screen now = client.currentScreen;
		if (!(now instanceof RecipeScreen) || now == before) {
			throw new IllegalStateException("EMI has no page for " + stack.getId());
		}
		return stack.getId() + " -> " + now.getClass().getSimpleName();
	}

	private static String structure(String id) {
		return show(StructureEmiStack.of(Identifier.of(id)));
	}

	private static String biome(String id) {
		Biome biome = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME).get(Identifier.of(id));
		if (biome == null) {
			throw new IllegalArgumentException("unknown biome " + id);
		}
		return show(BiomeEmiStack.of(biome));
	}

	public static String pillagerOutpost() {
		return structure("minecraft:pillager_outpost");
	}

	public static String villagePlains() {
		return structure("minecraft:village_plains");
	}

	public static String stronghold() {
		return structure("minecraft:stronghold");
	}

	public static String ruinedPortalNether() {
		return structure("minecraft:ruined_portal_nether");
	}

	public static String trialChambers() {
		return structure("minecraft:trial_chambers");
	}

	public static String plains() {
		return biome("minecraft:plains");
	}

	public static String desert() {
		return biome("minecraft:desert");
	}

	public static String netherWastes() {
		return biome("minecraft:nether_wastes");
	}

	public static String deepDark() {
		return biome("minecraft:deep_dark");
	}
}
