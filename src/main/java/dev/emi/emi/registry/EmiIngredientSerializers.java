package dev.emi.emi.registry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.util.JsonHelper;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EmiIngredientSerializers {
	public static final Map<Class<?>, EmiIngredientSerializer<?>> BY_CLASS = Maps.newHashMap();
	public static final Map<String, EmiIngredientSerializer<?>> BY_TYPE = Maps.newHashMap();
	// EMI Remastered: types already reported as unknown, kept across reloads so the line appears once per session
	private static final Set<String> REPORTED_UNKNOWN_TYPES = ConcurrentHashMap.newKeySet();

	public static void clear() {
		BY_CLASS.clear();
		BY_TYPE.clear();
	}

	public static @Nullable JsonElement serialize(EmiIngredient ingredient) {
		if (ingredient == null || !BY_CLASS.containsKey(ingredient.getClass())) {
			return null;
		}
		try {
			return ((EmiIngredientSerializer) BY_CLASS.get(ingredient.getClass())).serialize(ingredient);
		} catch (Exception e) {
			EmiLog.error("Exception serializing stack " + ingredient, e);
			return null;
		}
	}

	public static EmiIngredient deserialize(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return EmiStack.EMPTY;
		}
		try {
			String type;
			if (element.isJsonObject()) {
				JsonObject json = element.getAsJsonObject();
				JsonElement typeElement = json.get("type"); // EMI Remastered: an object without a type is unreadable, not an exception
				if (typeElement == null || typeElement.isJsonNull()) {
					EmiLog.warn("Stack without a type: " + element);
					return EmiStack.EMPTY;
				}
				type = typeElement.getAsString();
				if (type.equals("emi:item")) {
					json.addProperty("type", "item");
					if (!json.has("id")) {
						json.addProperty("id", JsonHelper.getString(json, "item", ""));
					}
				} else if (type.equals("emi:fluid")) {
					json.addProperty("type", "fluid");
					if (!json.has("id")) {
						json.addProperty("id", JsonHelper.getString(json, "fluid", ""));
					}
				} else if (type.equals("emi:item_tag")) {
					json.addProperty("type", "tag");
					json.addProperty("registry", "minecraft:item");
					if (!json.has("id")) {
						json.addProperty("id", JsonHelper.getString(json, "tag", ""));
					}
				}
				type = json.get("type").getAsString();
			} else if (element.isJsonArray()) {
				type = "list";
			} else {
				String[] split = element.getAsString().split(":");
				type = split[0];
				if (!BY_TYPE.containsKey(type) && type.startsWith("#")) {
					type = "tag";
				}
			}
			EmiIngredientSerializer serializer = BY_TYPE.get(type);
			if (serializer == null) {
				// EMI Remastered: a missing mod or disabled plugin is normal, the callers keep the raw entry
				if (REPORTED_UNKNOWN_TYPES.add(type)) {
					EmiLog.warn("Unknown stack type \"" + type + "\" (mod missing or its EMI plugin disabled), entries of this type are kept but not shown");
				}
				return EmiStack.EMPTY;
			}
			return serializer.deserialize(element);
		} catch (Exception e) {
			EmiLog.error("Exception deserializing stack " + element, e);
			return EmiStack.EMPTY;
		}
	}
}
