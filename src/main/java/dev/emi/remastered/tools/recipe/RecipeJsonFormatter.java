package dev.emi.remastered.tools.recipe;

import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dev.emi.remastered.tools.recipe.RecipeSnapshot.Slot;

/**
 * Generic JSON of a {@link RecipeSnapshot}, for recipes that have no vanilla recipe behind them. Shape:
 * <pre>
 * { "id": "...", "category": "emi:smelting",
 *   "inputs":    [ {"type": "item", "id": "minecraft:raw_iron", "count": 1} ],
 *   "outputs":   [ ... ], "catalysts": [ ... ] }
 * </pre>
 * Slots: a stack is {@code {"type","id","count"}} ({@code "amount"} in droplets for fluids), a tag is
 * {@code {"type","tag","count"}}, alternatives are {@code {"any_of":[stacks],"count"}}; {@code "chance"} appears when it
 * is below 1. Empty input slots stay as {@code null} so shaped layouts keep their positions. Pure logic, no Minecraft
 * classes.
 */
public final class RecipeJsonFormatter {
	private RecipeJsonFormatter() {
	}

	public static JsonObject toJson(RecipeSnapshot recipe) {
		JsonObject json = new JsonObject();
		if (recipe.id() != null) {
			json.addProperty("id", recipe.id());
		}
		json.addProperty("category", recipe.categoryId());
		json.add("inputs", slots(recipe.inputs()));
		json.add("outputs", slots(recipe.outputs()));
		if (!recipe.catalysts().isEmpty()) {
			json.add("catalysts", slots(recipe.catalysts()));
		}
		return json;
	}

	private static JsonArray slots(List<Slot> slots) {
		JsonArray array = new JsonArray();
		for (Slot slot : slots) {
			array.add(slot(slot, true));
		}
		return array;
	}

	private static JsonElement slot(Slot slot, boolean withAmount) {
		JsonObject json = new JsonObject();
		switch (slot.kind()) {
			case EMPTY -> {
				return JsonNull.INSTANCE;
			}
			case STACK -> {
				json.addProperty("type", slot.type());
				json.addProperty("id", slot.id());
			}
			case TAG -> {
				json.addProperty("type", slot.type());
				json.addProperty("tag", slot.id());
			}
			case ANY_OF -> {
				JsonArray options = new JsonArray();
				for (Slot option : slot.options()) {
					options.add(slot(option, false));
				}
				json.add("any_of", options);
			}
		}
		if (withAmount) {
			json.addProperty(slot.isFluid() ? "amount" : "count", slot.amount());
		}
		if (slot.components() != null && !slot.components().isEmpty()) {
			json.addProperty("components", slot.components());
		}
		if (slot.chance() < 1) {
			json.addProperty("chance", slot.chance());
		}
		return json;
	}

	/** Pretty printed JSON text with two-space indentation. */
	public static String pretty(JsonElement json) {
		return new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create().toJson(json);
	}
}
