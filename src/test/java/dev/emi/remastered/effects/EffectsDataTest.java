package dev.emi.remastered.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The resources shipped in assets/emiffect must parse and their lang keys must exist in English and German. */
class EffectsDataTest {

	private static JsonElement resource(String path) throws IOException {
		try (InputStream in = EffectsDataTest.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, path + " on the classpath");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
	}

	@Test
	void shippedSourceTableParsesWithoutErrors() throws IOException {
		List<String> errors = new ArrayList<>();
		List<EffectSourceDef> defs = EffectSourceDef.parse(resource("assets/emiffect/emiffect/effect_sources/vanilla.json"), errors::add);
		assertEquals(List.of(), errors);
		assertFalse(defs.isEmpty());
		for (String lang : List.of("en_us", "de_de")) {
			JsonObject keys = resource("assets/emiffect/lang/" + lang + ".json").getAsJsonObject();
			for (EffectSourceDef def : defs) {
				if (!def.text().isEmpty()) {
					assertTrue(keys.has(def.text()), lang + " " + def.text());
				}
			}
		}
	}

	@Test
	void shippedExtraStacksParse() throws IOException {
		for (String name : List.of("bad_omen", "bad_omen_legacy", "conduit_power", "darkness", "hunger", "levitation",
				"mining_fatigue", "water_breathing", "wither")) {
			JsonElement json = resource("assets/emiffect/emiffect/extra_stacks/" + name + ".json");
			EffectSourceDef def = EffectSourceDef.parseExtra(json.getAsJsonObject());
			assertEquals(1, def.effects().size(), name);
			assertFalse(def.stacks().isEmpty(), name);
		}
	}

	@Test
	void everyLanguageKeyOfTheDefaultLanguageExistsInGerman() throws IOException {
		JsonObject en = resource("assets/emiffect/lang/en_us.json").getAsJsonObject();
		JsonObject de = resource("assets/emiffect/lang/de_de.json").getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : en.entrySet()) {
			assertTrue(de.has(entry.getKey()), "de_de " + entry.getKey());
		}
		assertEquals(en.keySet(), de.keySet());
	}

	@Test
	void settingsKeysHaveNamesAndTooltips() throws IOException {
		JsonObject en = resource("assets/emiffect/lang/en_us.json").getAsJsonObject();
		for (String key : List.of("show_in_index", "show_attribute_modifiers", "show_potion_variants", "show_hardcoded_sources")) {
			assertTrue(en.has("config.emi.effects." + key), key);
			assertTrue(en.has("config.emi.tooltip.effects." + key), key);
		}
	}
}
