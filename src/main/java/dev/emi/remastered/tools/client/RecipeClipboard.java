package dev.emi.remastered.tools.client;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.tools.RecipeFormat;
import dev.emi.remastered.tools.recipe.RecipeJsonFormatter;
import dev.emi.remastered.tools.recipe.RecipeSnapshot;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter;
import dev.emi.remastered.tools.recipe.RecipeTextFormatter.Labels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

/** Copies an EMI recipe to the clipboard as readable text or as JSON and confirms it with a toast. */
public final class RecipeClipboard {
	private static final String KEY = "emi_remastered_tools.copy.";

	private RecipeClipboard() {
	}

	/** What a copy produced. */
	public record Result(String content, RecipeFormat format, boolean vanilla) {
	}

	/** Copies {@code recipe} in {@code format}; shows a toast. Returns false when nothing could be copied. */
	public static boolean copy(EmiRecipe recipe, RecipeFormat format) {
		MinecraftClient client = MinecraftClient.getInstance();
		Result result;
		try {
			result = render(recipe, format);
		} catch (RuntimeException e) {
			EmiLog.error("[Tools] Could not copy recipe " + recipe.getId(), e);
			SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
				EmiPort.translatable(KEY + "failed.title"), EmiPort.translatable(KEY + "failed.text"));
			return false;
		}
		client.keyboard.setClipboard(result.content());
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		Text name = EmiPort.literal(recipe.getId() == null ? recipe.getCategory().getName().getString() : recipe.getId().toString());
		String how = result.format() == RecipeFormat.TEXT ? "text" : result.vanilla() ? "json_vanilla" : "json_generic";
		SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
			EmiPort.translatable(KEY + "title"), EmiPort.translatable(KEY + "done." + how, name));
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

	/** The recipe encoded with the vanilla recipe codec (what a data pack file contains), or null. */
	private static @Nullable String vanillaJson(EmiRecipe recipe) {
		MinecraftClient client = MinecraftClient.getInstance();
		RecipeEntry<?> entry = recipe.getBackingRecipe();
		if (entry == null || client.world == null) {
			return null;
		}
		DataResult<JsonElement> result = Recipe.CODEC.encodeStart(client.world.getRegistryManager().getOps(JsonOps.INSTANCE),
			entry.value());
		if (result.error().isPresent()) {
			EmiLog.warn("[Tools] Vanilla recipe codec failed for " + entry.id() + ": " + result.error().get().message());
			return null;
		}
		JsonElement json = result.result().orElseThrow();
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

	private static Labels labels() {
		Language language = Language.getInstance();
		String prefix = KEY + "label.";
		return new Labels(language.get(prefix + "id"), language.get(prefix + "inputs"), language.get(prefix + "outputs"),
			language.get(prefix + "catalysts"), language.get(prefix + "any_of"), language.get(prefix + "more"),
			language.get(prefix + "chance"), language.get(prefix + "tag_size"), language.get(prefix + "empty_slots"));
	}
}
