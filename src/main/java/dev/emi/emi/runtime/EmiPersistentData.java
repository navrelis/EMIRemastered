package dev.emi.emi.runtime;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.emi.emi.bom.BoM;
import dev.emi.remastered.runtime.ReloadGuard;
import dev.emi.remastered.runtime.SaveDebouncer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.JsonHelper;

public class EmiPersistentData {
	public static final File FILE = new File("emi.json");
	public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
	// EMI Remastered: KB3 / KB14 state
	private static final SaveDebouncer SAVER = new SaveDebouncer(500, EmiPersistentData::saveNow, task -> {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.execute(task);
		} else {
			task.run();
		}
	}, SaveDebouncer.daemonTimer());
	private static volatile JsonObject loadedRoot = new JsonObject();
	private static final Set<String> unreadSections = Sets.newHashSet();
	private static volatile boolean loaded = false;

	/** EMI Remastered: debounced, writes 500 ms after the last change (KB14); see {@link #flush()}. */
	public static void save() {
		SAVER.request();
	}

	/** EMI Remastered: writes a pending {@link #save()} now (disconnect, client stop, before reading the file). */
	public static void flush() {
		SAVER.flush();
	}

	/** EMI Remastered: the original synchronous save. */
	public static void saveNow() {
		if (!loaded && FILE.exists()) {
			// Never replace the file with the empty state of a session that has not read it yet (KB3)
			EmiLog.warn("Not writing persistent data, emi.json was not loaded yet");
			return;
		}
		try {
			JsonObject json = new JsonObject();
			json.add("favorites", EmiFavorites.save());
			EmiSidebars.save(json);
			json.add("recipe_defaults", BoM.saveAdded());
			json.add("hidden_stacks", EmiHidden.save());
			// EMI Remastered: sections that failed to load and unknown keys are written back unchanged (KB3)
			for (Map.Entry<String, JsonElement> entry : loadedRoot.entrySet()) {
				if (!json.has(entry.getKey()) || unreadSections.contains(entry.getKey())) {
					json.add(entry.getKey(), entry.getValue().deepCopy());
				}
			}
			Path target = FILE.toPath();
			Path temp = target.resolveSibling(FILE.getName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
			try {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			EmiLog.error("Failed to write persistent data", e);
		}
	}

	public static void load() {
		// EMI Remastered: pending changes first, and never resolve stacks without a world (KB1, KB3, KB14)
		flush();
		if (ReloadGuard.isWorldGone()) {
			return;
		}
		if (!FILE.exists()) {
			loadedRoot = new JsonObject();
			unreadSections.clear();
			loaded = true;
			return;
		}
		JsonObject json;
		try (Reader reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
			json = GSON.fromJson(reader, JsonObject.class);
		} catch (Exception e) {
			EmiLog.error("Failed to parse persistent data", e);
			// EMI Remastered: keep the unreadable file instead of overwriting it on the next save
			try {
				Files.copy(FILE.toPath(), FILE.toPath().resolveSibling(FILE.getName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
				EmiLog.warn("Copied the unreadable emi.json to emi.json.broken");
			} catch (Exception e2) {
				EmiLog.error("Failed to back up emi.json", e2);
			}
			json = null;
		}
		if (json == null) {
			json = new JsonObject();
		}
		loadedRoot = json;
		unreadSections.clear();
		// EMI Remastered: one broken section no longer skips the others; it is written back as it was
		try {
			if (JsonHelper.hasArray(json, "favorites")) {
				EmiFavorites.load(JsonHelper.getArray(json, "favorites"));
			}
		} catch (Exception e) {
			unreadSections.add("favorites");
			EmiLog.error("Failed to parse favorites", e);
		}
		try {
			EmiSidebars.load(json);
		} catch (Exception e) {
			unreadSections.add("lookup_history");
			unreadSections.add("craft_history");
			EmiLog.error("Failed to parse sidebar history", e);
		}
		try {
			if (JsonHelper.hasJsonObject(json, "recipe_defaults")) {
				BoM.loadAdded(JsonHelper.getObject(json, "recipe_defaults"));
			}
		} catch (Exception e) {
			unreadSections.add("recipe_defaults");
			EmiLog.error("Failed to parse recipe defaults", e);
		}
		try {
			if (JsonHelper.hasArray(json, "hidden_stacks")) {
				EmiHidden.load(JsonHelper.getArray(json, "hidden_stacks"));
			}
		} catch (Exception e) {
			unreadSections.add("hidden_stacks");
			EmiLog.error("Failed to parse hidden stacks", e);
		}
		loaded = true;
	}
}
