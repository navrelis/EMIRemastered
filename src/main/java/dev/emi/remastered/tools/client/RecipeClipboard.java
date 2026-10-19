package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.tools.RecipeFormat;
import dev.emi.remastered.tools.recipe.CraftingRecipeJson;
import dev.emi.remastered.tools.recipe.CraftingRecipeJson.Cell;
import dev.emi.remastered.tools.recipe.RecipeJsonFormatter;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter.Labels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

/** Copies an EMI recipe to the clipboard as readable text or as JSON and confirms it with a toast or notice. */
public final class RecipeClipboard {
	private static final String KEY = "emi_remastered_tools.copy.";

	private RecipeClipboard() {
	}

	/** What a copy produced. */
	public record Result(String content, RecipeFormat format, boolean vanilla) {
	}

	/** Copies {@code recipe} in {@code format}; shows a toast (a notice where toasts are hidden). Returns false when nothing could be copied. */
	public static boolean copy(EmiRecipe recipe, RecipeFormat format) {
		MinecraftClient client = MinecraftClient.getInstance();
		Result result;
		try {
			result = render(recipe, format);
		} catch (RuntimeException e) {
			EmiLog.error("[Tools] Could not copy recipe " + recipe.getId(), e);
			CopyNotice.show(EmiPort.translatable(KEY + "failed.title"), EmiPort.translatable(KEY + "failed.text"));
			return false;
		}
		client.keyboard.setClipboard(result.content());
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		Text name = EmiPort.literal(recipe.getId() == null ? recipe.getCategory().getName().getString() : recipe.getId().toString());
		String how = result.format() == RecipeFormat.TEXT ? "text" : result.vanilla() ? "json_vanilla" : "json_generic";
		CopyNotice.show(EmiPort.translatable(KEY + "title"), EmiPort.translatable(KEY + "done." + how, name));
		return true;
	}

	/** Builds the clipboard content without touching the clipboard. */
	public static Result render(EmiRecipe recipe, RecipeFormat format) {
		if (format == RecipeFormat.TEXT) {
			return new Result(RecipeTextFormatter.format(RecipeSnapshots.of(recipe), labels()), format, false);
		}
		String vanilla = vanillaJson(recipe);
		if (vanilla != null) {
			return new Result(vanilla, format, true);
		}
		return new Result(RecipeJsonFormatter.pretty(RecipeJsonFormatter.toJson(RecipeSnapshots.of(recipe))), format, false);
	}

	/**
	 * The recipe as a data pack file would contain it, or null. Crafting recipes are rebuilt by hand because the client
	 * only holds the unpacked {@link ShapedRecipe} (the vanilla codec refuses to encode it); everything else goes
	 * through the vanilla recipe codec.
	 */
	private static @Nullable String vanillaJson(EmiRecipe recipe) {
		MinecraftClient client = MinecraftClient.getInstance();
		RecipeEntry<?> entry = recipe.getBackingRecipe();
		if (entry == null || client.world == null) {
			return null;
		}
		DynamicRegistryManager registries = client.world.getRegistryManager();
		DynamicOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
		JsonElement json = null;
		try {
			if (entry.value() instanceof ShapedRecipe shaped) {
				json = shapedJson(shaped, registries, ops);
			} else if (entry.value() instanceof ShapelessRecipe shapeless) {
				json = shapelessJson(shapeless, registries, ops);
			}
		} catch (RuntimeException e) {
			EmiLog.warn("[Tools] Could not rebuild the crafting recipe " + entry.id() + ": " + e.getMessage());
		}
		if (json == null) {
			DataResult<JsonElement> result = Recipe.CODEC.encodeStart(ops, entry.value());
			if (result.error().isPresent()) {
				EmiLog.warn("[Tools] Vanilla recipe codec failed for " + entry.id() + ": " + result.error().get().message());
				return null;
			}
			json = result.result().orElseThrow();
		}
		if (json instanceof JsonObject object && object.has("type")) {
			JsonObject ordered = new JsonObject();
			ordered.add("type", object.get("type"));
			object.asMap().forEach((key, value) -> {
				if (!key.equals("type")) {
					ordered.add(key, value);
				}
			});
			json = ordered;
		}
		return RecipeJsonFormatter.pretty(json);
	}

	private static @Nullable JsonElement shapedJson(ShapedRecipe recipe, DynamicRegistryManager registries, DynamicOps<JsonElement> ops) {
		TagLookup tags = new TagLookup();
		List<Cell> cells = new ArrayList<>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			cells.add(ingredient.isEmpty() ? Cell.EMPTY : new Cell(ingredientJson(ingredient, ops, tags)));
		}
		JsonElement result = ItemStack.CODEC.encodeStart(ops, recipe.getResult(registries)).getOrThrow();
		return CraftingRecipeJson.shaped(recipe.getCategory().asString(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
			cells, result, recipe.showNotification());
	}

	private static JsonElement shapelessJson(ShapelessRecipe recipe, DynamicRegistryManager registries, DynamicOps<JsonElement> ops) {
		TagLookup tags = new TagLookup();
		List<JsonElement> ingredients = new ArrayList<>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			if (!ingredient.isEmpty()) {
				ingredients.add(ingredientJson(ingredient, ops, tags));
			}
		}
		JsonElement result = ItemStack.CODEC.encodeStart(ops, recipe.getResult(registries)).getOrThrow();
		return CraftingRecipeJson.shapeless(recipe.getCategory().asString(), recipe.getGroup(), ingredients, result);
	}

	/**
	 * The client's ingredient only knows its item list. A list that is exactly the content of an item tag is written as
	 * that tag again (what the data pack most likely had), anything else as the list of items.
	 */
	private static JsonElement ingredientJson(Ingredient ingredient, DynamicOps<JsonElement> ops, TagLookup tags) {
		JsonElement json = Ingredient.ALLOW_EMPTY_CODEC.encodeStart(ops, ingredient).getOrThrow();
		if (json.isJsonArray()) {
			Identifier tag = tags.find(ingredient);
			if (tag != null) {
				JsonObject object = new JsonObject();
				object.addProperty("tag", tag.toString());
				return object;
			}
		}
		return json;
	}

	/** Finds the item tag whose content equals an ingredient's items; the tag index is built on first use. */
	private static final class TagLookup {
		private Map<Set<Item>, List<Identifier>> byItems;

		@Nullable Identifier find(Ingredient ingredient) {
			Set<Item> items = new HashSet<>();
			for (ItemStack stack : ingredient.getMatchingStacks()) {
				items.add(stack.getItem());
			}
			if (items.size() < 2) {
				return null;
			}
			if (byItems == null) {
				byItems = new HashMap<>();
				Registries.ITEM.streamTags().forEach(tag -> {
					Set<Item> content = new HashSet<>();
					Registries.ITEM.iterateEntries(tag).forEach(holder -> content.add(holder.value()));
					byItems.computeIfAbsent(content, k -> new ArrayList<>()).add(tag.id());
				});
			}
			List<Identifier> candidates = byItems.get(items);
			if (candidates == null) {
				return null;
			}
			String chosen = CraftingRecipeJson.chooseTag(candidates.stream().map(Identifier::toString).toList());
			return chosen == null ? null : Identifier.tryParse(chosen);
		}
	}

	private static Labels labels() {
		Language language = Language.getInstance();
		String prefix = KEY + "label.";
		return new Labels(language.get(prefix + "id"), language.get(prefix + "inputs"), language.get(prefix + "outputs"),
			language.get(prefix + "catalysts"), language.get(prefix + "any_of"), language.get(prefix + "more"),
			language.get(prefix + "chance"), language.get(prefix + "tag_size"), language.get(prefix + "empty_slots"));
	}
}
