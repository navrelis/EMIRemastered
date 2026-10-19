package dev.emi.remastered.devtour;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed access to the arguments of one tour step (the JSON object of the step). */
public final class Args {
	private final JsonObject json;

	public Args(JsonObject json) {
		this.json = json;
	}

	public JsonObject json() {
		return json;
	}

	public boolean has(String name) {
		JsonElement e = json.get(name);
		return e != null && !e.isJsonNull();
	}

	/** Required string argument. */
	public String str(String name) {
		if (!has(name)) {
			throw new IllegalArgumentException("missing argument '" + name + "'");
		}
		return json.get(name).getAsString();
	}

	public String str(String name, String fallback) {
		return has(name) ? json.get(name).getAsString() : fallback;
	}

	/** Required integer argument. */
	public int intVal(String name) {
		if (!has(name)) {
			throw new IllegalArgumentException("missing argument '" + name + "'");
		}
		return json.get(name).getAsInt();
	}

	public int intVal(String name, int fallback) {
		return has(name) ? json.get(name).getAsInt() : fallback;
	}

	public boolean bool(String name, boolean fallback) {
		return has(name) ? json.get(name).getAsBoolean() : fallback;
	}

	/** Lower-cased string, handy for enum-like arguments. */
	public String lower(String name, String fallback) {
		return str(name, fallback).toLowerCase(Locale.ROOT);
	}
}
