package dev.emi.remastered.devtour;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** A parsed tour file: settings plus the list of steps. */
public final class TourPlan {
	public final String name;
	public final String source;
	public final String description;
	/** Ticks to wait after EMI reported "loaded" before the first step. */
	public final int warmupTicks;
	/** Minimum ticks a screenshot step waits (on top of 3 rendered frames) so async work (search, tabs) settles. */
	public final int settleTicks;
	public final int stepTimeoutSeconds;
	public final int worldJoinTimeoutSeconds;
	/** Global timeout for the whole tour, measured from game start. */
	public final int timeoutSeconds;
	public final List<JsonObject> steps;
	/** System properties to set as soon as the tour is loaded (before the integrated server starts), e.g. dev-only delays. */
	public final Map<String, String> systemProperties;

	private TourPlan(String name, String source, JsonObject root) {
		this.name = name;
		this.source = source;
		this.description = root.has("description") ? root.get("description").getAsString() : "";
		this.warmupTicks = getInt(root, "warmupTicks", 60);
		this.settleTicks = getInt(root, "settleTicks", 10);
		this.stepTimeoutSeconds = getInt(root, "stepTimeoutSeconds", 30);
		this.worldJoinTimeoutSeconds = getInt(root, "worldJoinTimeoutSeconds", 120);
		this.timeoutSeconds = getInt(root, "timeoutSeconds", 300);
		List<JsonObject> list = new ArrayList<>();
		JsonArray arr = root.has("steps") ? root.getAsJsonArray("steps") : new JsonArray();
		for (JsonElement e : arr) {
			list.add(e.getAsJsonObject());
		}
		this.steps = list;
		Map<String, String> properties = new LinkedHashMap<>();
		if (root.has("systemProperties")) {
			root.getAsJsonObject("systemProperties").entrySet().forEach(e -> properties.put(e.getKey(), e.getValue().getAsString()));
		}
		this.systemProperties = properties;
	}

	private static int getInt(JsonObject o, String key, int fallback) {
		return o.has(key) ? o.get(key).getAsInt() : fallback;
	}

	/**
	 * Resolves a tour: a path to a json file, else {@code <gameDir>/tour/<name>.json}, else the built-in
	 * {@code /tour/<name>.json} resource of the devtour mod.
	 */
	public static TourPlan load(String nameOrPath, Path gameDir) throws IOException {
		boolean looksLikePath = nameOrPath.contains("/") || nameOrPath.contains("\\") || nameOrPath.endsWith(".json");
		Path file = null;
		if (looksLikePath) {
			file = Path.of(nameOrPath);
			if (!file.isAbsolute()) {
				file = gameDir.resolve(nameOrPath);
			}
		} else {
			Path candidate = gameDir.resolve("tour").resolve(nameOrPath + ".json");
			if (Files.isRegularFile(candidate)) {
				file = candidate;
			}
		}
		String displayName = nameOrPath;
		if (looksLikePath) {
			String fn = file.getFileName().toString();
			displayName = fn.endsWith(".json") ? fn.substring(0, fn.length() - 5) : fn;
		}
		if (file != null) {
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				return new TourPlan(displayName, file.toString(), JsonParser.parseReader(r).getAsJsonObject());
			}
		}
		String resource = "/tour/" + nameOrPath + ".json";
		try (InputStream in = TourPlan.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("tour '" + nameOrPath + "' not found: neither " + gameDir.resolve("tour").resolve(nameOrPath + ".json")
					+ " nor built-in resource " + resource);
			}
			try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return new TourPlan(displayName, "built-in:" + resource, JsonParser.parseReader(r).getAsJsonObject());
			}
		}
	}
}
