package dev.emi.remastered.devtour;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiFavorites;
import dev.emi.emi.runtime.EmiPersistentData;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.mobs.emi.MobEmiStack;
import dev.emi.remastered.ores.OresSettings;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.world.emi.StructureEmiStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Dev tour helpers of the X-UX fixes (mob stacks of mobs without spawn egg, trial chamber category, biome stacks,
 * Item Info of biomes and structures), called by the tour step {@code call}. Dev only, never packaged.
 */
public final class XUxTour {
	private XUxTour() {
	}

	private static EmiStack indexStack(Predicate<EmiStack> test, String what) {
		for (EmiStack stack : EmiStackList.stacks) {
			if (test.test(stack)) {
				return stack;
			}
		}
		throw new IllegalStateException("no " + what + " in the EMI index");
	}

	private static EmiStack mob(String path) {
		return indexStack(stack -> stack instanceof MobEmiStack && stack.getId().getPath().equals(path), "mob stack " + path);
	}

	/** A key press on a stack through EMI's stack interaction, as on the stack under the mouse in the index. */
	private static String press(EmiStack stack, int key) {
		Screen before = MinecraftClient.getInstance().currentScreen;
		int scancode = GLFW.glfwGetKeyScancode(key);
		boolean handled = EmiScreenManager.stackInteraction(new EmiStackInteraction(stack), bind -> bind.matchesKey(key, scancode));
		Screen now = MinecraftClient.getInstance().currentScreen;
		if (!handled || now == before) {
			throw new IllegalStateException("key " + key + " on " + stack.getId() + " did nothing (handled=" + handled + ")");
		}
		return stack.getId() + " key " + key + " -> " + now.getClass().getSimpleName();
	}

	public static String rEnderDragon() {
		return press(mob("ender_dragon"), GLFW.GLFW_KEY_R);
	}

	public static String rWither() {
		return press(mob("wither"), GLFW.GLFW_KEY_R);
	}

	public static String uWither() {
		return press(mob("wither"), GLFW.GLFW_KEY_U);
	}

	/** Item Info on the wither mob stack. */
	public static String infoWither() {
		InfoLink.info(mob("wither")).open();
		return "item info of the wither stack -> " + MinecraftClient.getInstance().currentScreen.getClass().getSimpleName();
	}

	public static String infoPlainsBiome() {
		InfoLink.info(indexStack(stack -> stack instanceof BiomeEmiStack && stack.getId().toString().equals("minecraft:plains"), "plains biome stack")).open();
		return "item info of the plains stack -> " + MinecraftClient.getInstance().currentScreen.getClass().getSimpleName();
	}

	public static String infoVillage() {
		InfoLink.info(indexStack(stack -> stack instanceof StructureEmiStack && stack.getId().toString().equals("minecraft:village_plains"), "village stack")).open();
		return "item info of the village stack -> " + MinecraftClient.getInstance().currentScreen.getClass().getSimpleName();
	}

	/** Ctrl+Y: every category as a tab. */
	public static String allRecipes() {
		EmiApi.displayAllRecipes();
		Screen screen = MinecraftClient.getInstance().currentScreen;
		if (!(screen instanceof RecipeScreen)) {
			throw new IllegalStateException("no recipe screen");
		}
		boolean trial = false;
		int categories = 0;
		for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
			categories++;
			trial |= category.getId().toString().equals("emi_loot:trial_chamber");
		}
		return categories + " categories, trial chamber category registered: " + trial;
	}

	/** Switches ores.add-biomes-to-index off (not saved) and reloads EMI; follow with emi_wait_loaded. */
	public static String oresBiomeIndexOff() {
		OresSettings.addBiomesToIndex = false;
		EmiReloadManager.reload();
		return "ores.add-biomes-to-index = false, reload started";
	}

	public static String oresBiomeIndexOn() {
		OresSettings.addBiomesToIndex = true;
		EmiReloadManager.reload();
		return "ores.add-biomes-to-index = true, reload started";
	}

	/** Biome stacks in the index and how many of them share an id (must be none). */
	public static String biomeStacks() {
		int total = 0;
		Set<String> ids = new HashSet<>();
		for (EmiStack stack : EmiStackList.stacks) {
			if (stack instanceof BiomeEmiStack) {
				total++;
				ids.add(stack.getId().toString());
			}
		}
		return total + " biome stacks, " + ids.size() + " distinct, " + (total - ids.size()) + " duplicates";
	}

	/** Favourites the ender dragon mob stack and writes emi.json right away. */
	public static String favoriteDragon() {
		EmiFavorites.addFavorite(mob("ender_dragon"));
		EmiPersistentData.saveNow();
		return "ender dragon favourited, emi.json written: " + EmiFavorites.favorites.size() + " favourites";
	}

	private static boolean dragonFavourited() {
		return EmiFavorites.favorites.stream().anyMatch(f -> f.getEmiStacks().get(0) instanceof MobEmiStack mob && mob.getId().getPath().equals("ender_dragon"));
	}

	/** Fails unless the ender dragon mob stack is among the favourites read back from emi.json. */
	public static String checkDragonFavourite() {
		if (!dragonFavourited()) {
			throw new IllegalStateException("the ender dragon favourite was not restored from emi.json");
		}
		return "ender dragon favourite restored from emi.json (" + EmiFavorites.favorites.size() + " favourites)";
	}

	public static String unfavoriteDragon() {
		EmiFavorites.removeFavorite(mob("ender_dragon"));
		EmiPersistentData.saveNow();
		return "ender dragon favourite removed: " + !dragonFavourited();
	}

	/** Mob stacks (mobs without spawn egg) in the index. */
	public static String mobStacks() {
		StringBuilder names = new StringBuilder();
		int total = 0;
		for (EmiStack stack : EmiStackList.stacks) {
			if (stack instanceof MobEmiStack) {
				total++;
				names.append(' ').append(stack.getId().getPath());
			}
		}
		return total + " mob stacks:" + names;
	}
}
