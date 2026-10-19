package dev.emi.emi.bom;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.data.RecipeDefaults;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiPersistentData;
import dev.emi.remastered.runtime.PreservedEntries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class BoM {
	private static RecipeDefaults defaults = new RecipeDefaults();
	public static MaterialTree tree;
	public static Map<EmiIngredient, EmiRecipe> defaultRecipes = Maps.newHashMap();
	public static Map<EmiIngredient, EmiRecipe> addedRecipes = Maps.newHashMap();
	public static Set<EmiRecipe> disabledRecipes = Sets.newHashSet();
	public static boolean craftingMode = false;
	// EMI Remastered: recipe defaults that could not be read stay in emi.json (KB3)
	private static final PreservedEntries unreadAdded = new PreservedEntries();
	private static final PreservedEntries unreadDisabled = new PreservedEntries();
	private static JsonObject unreadResolutions = new JsonObject();
	private static JsonObject unreadTags = new JsonObject();

	public static void setDefaults(RecipeDefaults defaults) {
		BoM.defaults = defaults;
		MinecraftClient.getInstance().execute(() -> reload());
	}

	public static JsonObject saveAdded() {
		JsonArray added = new JsonArray();
		JsonObject addedTags = new JsonObject();
		JsonObject resolutions = new JsonObject();
		Set<Identifier> placed = Sets.newHashSet();
		for (Map.Entry<EmiIngredient, EmiRecipe> entry : addedRecipes.entrySet()) {
			EmiRecipe recipe = entry.getValue();
			if (recipe instanceof EmiResolutionRecipe err) {
				if (err.ingredient instanceof TagEmiIngredient tei) {
					JsonElement el = EmiIngredientSerializer.getSerialized(tei.copy().setAmount(1).setChance(1));
					JsonElement val = EmiIngredientSerializer.getSerialized(err.stack);
					if (el != null && JsonHelper.isString(el) && val != null) {
						addedTags.add(el.getAsString(), val);
					}
				}
			} else if (recipe != null && recipe.getId() != null && !placed.contains(recipe.getId())) {
				DefaultStatus status = getRecipeStatus(recipe);
				placed.add(recipe.getId());
				if (status == DefaultStatus.FULL) {
					added.add(recipe.getId().toString());
				} else if (status == DefaultStatus.PARTIAL) {
					JsonArray arr = new JsonArray();
					for (EmiStack stack : recipe.getOutputs()) {
						if (getRecipe(stack) == recipe) {
							JsonElement el = EmiIngredientSerializer.getSerialized(stack);
							if (el != null) {
								arr.add(el);
							}
						}
					}
					if (!arr.isEmpty()) {
						resolutions.add(recipe.getId().toString(), arr);
					}
				}
			}
		}
		JsonArray disabled = new JsonArray();
		for (EmiRecipe recipe : disabledRecipes) {
			if (recipe != null && recipe.getId() != null) {
				disabled.add(recipe.getId().toString());
			}
		}
		// EMI Remastered: write back what could not be read (KB3)
		PreservedEntries.mergeObject(addedTags, unreadTags);
		PreservedEntries.mergeObject(resolutions, unreadResolutions);
		JsonObject obj = new JsonObject();
		obj.add("added", unreadAdded.mergeInto(added));
		obj.add("tags", addedTags);
		obj.add("resolutions", resolutions);
		obj.add("disabled", unreadDisabled.mergeInto(disabled));
		return obj;
	}

	public static void loadAdded(JsonObject object) {
		addedRecipes.clear();
		disabledRecipes.clear();
		// EMI Remastered: ids of missing recipes and stacks that do not load are kept verbatim (KB3); an unknown id
		// no longer puts null into disabledRecipes and a malformed entry no longer aborts the whole section
		unreadAdded.clear();
		unreadDisabled.clear();
		unreadResolutions = new JsonObject();
		unreadTags = new JsonObject();
		JsonArray disabled = JsonHelper.getArray(object, "disabled", new JsonArray());
		JsonElement previous = null;
		int index = 0;
		for (JsonElement el : disabled) {
			EmiRecipe recipe = recipeOf(el);
			if (recipe != null) {
				disabledRecipes.add(recipe);
			} else {
				unreadDisabled.keep(el, previous, index);
			}
			previous = el;
			index++;
		}
		JsonArray added = JsonHelper.getArray(object, "added", new JsonArray());
		previous = null;
		index = 0;
		for (JsonElement el : added) {
			EmiRecipe recipe = recipeOf(el);
			if (recipe == null) {
				unreadAdded.keep(el, previous, index);
			} else if (!disabledRecipes.contains(recipe)) {
				for (EmiStack output : recipe.getOutputs()) {
					addedRecipes.put(output, recipe);
				}
			}
			previous = el;
			index++;
		}
		JsonObject resolutions = JsonHelper.getObject(object, "resolutions", new JsonObject());
		for (String key : resolutions.keySet()) {
			EmiRecipe recipe = recipeOf(new JsonPrimitive(key));
			if (recipe != null && JsonHelper.hasArray(resolutions, key)) {
				JsonArray arr = JsonHelper.getArray(resolutions, key);
				for (JsonElement el : arr) {
					EmiIngredient stack = EmiIngredientSerializer.getDeserialized(el.deepCopy());
					if (!stack.isEmpty()) {
						addedRecipes.put(stack, recipe);
					} else {
						PreservedEntries.keepInObject(unreadResolutions, key, el);
					}
				}
			} else {
				unreadResolutions.add(key, resolutions.get(key));
			}
		}
		JsonObject addedTags = JsonHelper.getObject(object, "tags", new JsonObject());
		for (String key : addedTags.keySet()) {
			EmiIngredient tag = EmiIngredientSerializer.getDeserialized(new JsonPrimitive(key));
			EmiIngredient stack = EmiIngredientSerializer.getDeserialized(addedTags.get(key).deepCopy());
			if (!tag.isEmpty() && !stack.isEmpty() && stack.getEmiStacks().size() == 1 && tag.getEmiStacks().containsAll(stack.getEmiStacks())) {
				addedRecipes.put(tag, new EmiResolutionRecipe(tag, stack.getEmiStacks().get(0)));
			} else {
				unreadTags.add(key, addedTags.get(key));
			}
		}
		int unread = unreadAdded.size() + unreadDisabled.size() + unreadResolutions.size() + unreadTags.size();
		if (unread > 0) {
			EmiLog.warn("Keeping " + unread + " recipe defaults that could not be read (missing mod or invalid data)");
		}
	}

	// EMI Remastered: null for a missing recipe or a malformed id instead of an exception
	private static EmiRecipe recipeOf(JsonElement el) {
		if (!JsonHelper.isString(el)) {
			return null;
		}
		Identifier id = Identifier.tryParse(el.getAsString());
		return id == null ? null : EmiApi.getRecipeManager().getRecipe(id);
	}

	public static void reload() {
		defaultRecipes = defaults.bake();
	}

	public static boolean isRecipeEnabled(EmiRecipe recipe) {
		return !disabledRecipes.contains(recipe) && (defaultRecipes.values().contains(recipe) || addedRecipes.values().contains(recipe));
	}

	public static DefaultStatus getRecipeStatus(EmiRecipe recipe) {
		int found = 0;
		for (EmiStack stack : recipe.getOutputs()) {
			if (recipe.equals(getRecipe(stack))) {
				found++;
			}
		}
		if (found == 0) {
			return DefaultStatus.EMPTY;
		} else if (found >= recipe.getOutputs().size()) {
			return DefaultStatus.FULL;
		} else {
			return DefaultStatus.PARTIAL;
		}
	}

	public static EmiRecipe getRecipe(EmiIngredient stack) {
		EmiRecipe recipe = addedRecipes.get(stack);
		if (recipe == null) {
			recipe = defaultRecipes.get(stack);
			if (recipe != null && disabledRecipes.contains(recipe)) {
				return null;
			}
		}
		return recipe;
	}

	public static void setGoal(EmiRecipe recipe) {
		tree = new MaterialTree(recipe);
		craftingMode = false;
	}

	public static void addResolution(EmiIngredient ingredient, EmiRecipe recipe) {
		tree.addResolution(ingredient, recipe);
	}

	public static boolean isDefaultRecipe(EmiIngredient stack, EmiRecipe recipe) {
		if (recipe instanceof EmiResolutionRecipe err) {
			if (getRecipe(err.ingredient) instanceof EmiResolutionRecipe res) {
				return stack.equals(res.stack);
			}
		}
		return getRecipe(stack) == recipe;
	}

	public static void addRecipe(EmiRecipe recipe) {
		disabledRecipes.remove(recipe);
		for (EmiStack stack : recipe.getOutputs()) {
			addedRecipes.put(stack, recipe);
		}
		EmiPersistentData.save();
		recalculate();
	}

	public static void addRecipe(EmiIngredient stack, EmiRecipe recipe) {
		if (recipe instanceof EmiResolutionRecipe err) {
			stack = err.ingredient;
		}
		addedRecipes.put(stack, recipe);
		EmiPersistentData.save();
		recalculate();
	}

	public static void removeRecipe(EmiRecipe recipe) {
		for (EmiStack stack : recipe.getOutputs()) {
			addedRecipes.remove(stack, recipe);
		}
		if (getRecipeStatus(recipe) != DefaultStatus.EMPTY) {
			disabledRecipes.add(recipe);
		}
		EmiPersistentData.save();
		recalculate();
	}

	public static void removeRecipe(EmiIngredient stack, EmiRecipe recipe) {
		if (recipe instanceof EmiResolutionRecipe err) {
			if (addedRecipes.get(err.ingredient) instanceof EmiResolutionRecipe res && stack.equals(res.stack)) {
				addedRecipes.remove(err.ingredient);
			}
		} else {
			addedRecipes.remove(stack, recipe);
		}
		if (getRecipeStatus(recipe) != DefaultStatus.EMPTY) {
			disabledRecipes.add(recipe);
		}
		EmiPersistentData.save();
		recalculate();
	}

	private static void recalculate() {
		if (tree != null) {
			tree.recalculate();
		}
	}

	public static enum DefaultStatus {
		EMPTY,
		PARTIAL,
		FULL
	}
}
