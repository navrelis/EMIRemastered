package dev.emi.emi.runtime;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.chess.EmiChess;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.remastered.runtime.PreservedEntries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class EmiSidebars {
	public static List<EmiIngredient> craftables = List.of();
	public static List<EmiIngredient> lookupHistory = Lists.newArrayList();
	public static List<EmiIngredient> craftHistory = Lists.newArrayList();
	// EMI Remastered: history entries that could not be read stay in emi.json (KB3)
	private static final PreservedEntries unreadLookups = new PreservedEntries();
	private static final PreservedEntries unreadCrafts = new PreservedEntries();

	public static List<? extends EmiIngredient> getStacks(SidebarType type) {
		return switch (type) {
			case INDEX -> EmiConfig.editMode ? EmiStackList.stacks : EmiStackList.filteredStacks;
			case CRAFTABLES -> craftables;
			case FAVORITES -> EmiFavorites.favoriteSidebar;
			case LOOKUP_HISTORY -> lookupHistory;
			case CRAFT_HISTORY -> craftHistory;
			case EMPTY -> List.of();
			case CHESS -> EmiChess.SIDEBAR;
			default -> List.of();
		};
	}

	public static void lookup(EmiIngredient stack) {
		if (!stack.isEmpty()) {
			if (lookupHistory.size() >= 1 && lookupHistory.get(0).equals(stack)) {
				return;
			}
			lookupHistory.remove(stack);
			lookupHistory.add(0, stack);
			EmiPersistentData.save();
			EmiScreenManager.repopulatePanels(SidebarType.LOOKUP_HISTORY);
		}
	}

	public static void craft(EmiRecipe recipe) {
		if (!recipe.getOutputs().isEmpty()) {
			if (craftHistory.size() >= 1 && EmiApi.getRecipeContext(craftHistory.get(0)).equals(recipe)) {
				return;
			}
			EmiIngredient stack = new EmiFavorite.Craftable(recipe);
			craftHistory.removeIf(i -> i instanceof EmiFavorite.Craftable c && c.getRecipe().equals(recipe));
			craftHistory.add(0, stack);
			EmiPersistentData.save();
			EmiScreenManager.repopulatePanels(SidebarType.CRAFT_HISTORY);
		}
	}

	public static void save(JsonObject json) {
		JsonArray arr = new JsonArray();
		for (int i = 0; i < 1024; i++) {
			if (i >= lookupHistory.size()) {
				break;
			}
			EmiIngredient stack = lookupHistory.get(i);
			JsonElement el = EmiIngredientSerializer.getSerialized(stack);
			if (el != null && !el.isJsonNull()) {
				arr.add(el);
			}
		}
		json.add("lookup_history", limit(unreadLookups.mergeInto(arr), 1024)); // EMI Remastered

		arr = new JsonArray();
		for (int i = 0; i < 1024; i++) {
			if (i >= craftHistory.size()) {
				break;
			}
			EmiIngredient stack = craftHistory.get(i);
			EmiRecipe recipe = EmiApi.getRecipeContext(stack);
			if (recipe != null && recipe.getId() != null) {
				arr.add(recipe.getId().toString());
			}
		}
		json.add("craft_history", limit(unreadCrafts.mergeInto(arr), 1024)); // EMI Remastered
	}

	// EMI Remastered
	private static JsonArray limit(JsonArray arr, int max) {
		while (arr.size() > max) {
			arr.remove(arr.size() - 1);
		}
		return arr;
	}

	public static void load(JsonObject json) {
		lookupHistory.clear();
		unreadLookups.clear(); // EMI Remastered: entries that do not load are kept verbatim (KB3)
		if (JsonHelper.hasArray(json, "lookup_history")) {
			JsonElement previous = null;
			int index = 0;
			for (JsonElement el : JsonHelper.getArray(json, "lookup_history")) {
				EmiIngredient stack = EmiIngredientSerializer.getDeserialized(el.deepCopy());
				if (!stack.isEmpty()) {
					lookupHistory.add(stack);
				} else {
					unreadLookups.keep(el, previous, index);
				}
				previous = el;
				index++;
			}
		}

		craftHistory.clear();
		unreadCrafts.clear(); // EMI Remastered
		if (JsonHelper.hasArray(json, "craft_history")) {
			JsonElement previous = null;
			int index = 0;
			for (JsonElement el : JsonHelper.getArray(json, "craft_history")) {
				boolean read = false;
				if (JsonHelper.isString(el)) {
					String s = el.getAsString();
					if (Identifier.tryParse(s) instanceof Identifier id) {
						EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
						if (recipe != null) {
							craftHistory.add(new EmiFavorite.Craftable(recipe));
							read = true;
						}
					}
				}
				if (!read) {
					unreadCrafts.keep(el, previous, index);
				}
				previous = el;
				index++;
			}
		}
		if (!unreadLookups.isEmpty() || !unreadCrafts.isEmpty()) {
			EmiLog.warn("Keeping " + (unreadLookups.size() + unreadCrafts.size()) + " sidebar history entries that could not be read (missing mod or invalid data)");
		}
	}
}
