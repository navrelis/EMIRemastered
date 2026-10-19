package dev.emi.remastered.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.emi.remastered.config.ConfigCodec;
import dev.emi.remastered.config.ModuleConfigs;
import dev.emi.remastered.effects.EffectsEmiPlugin;
import dev.emi.remastered.enchanting.EnchantingEmiPlugin;
import dev.emi.remastered.letsdo.LetsDoEmiPlugin;
import dev.emi.remastered.loot.LootEmiPlugin;
import dev.emi.remastered.ores.OresEmiPlugin;
import dev.emi.remastered.plugin.PluginIds;
import dev.emi.remastered.professions.ProfessionsEmiPlugin;

/** Guards the module contract (docs/MODULES.md) for the real modules. */
class ModulesTest {
	private static final List<String> IDS = List.of("loot", "ores", "effects", "enchanting", "professions", "letsdo", "distraction_free", "info", "mobs", "tools");

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = ModulesTest.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, path + " on the classpath");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static Set<String> strings(JsonArray array) {
		Set<String> set = new HashSet<>();
		for (JsonElement e : array) {
			set.add(e.getAsString());
		}
		return set;
	}

	@Test
	void registryOrderAndIds() {
		assertEquals(IDS, Modules.all().stream().map(Module::getId).toList());
	}

	@Test
	void everyModuleFollowsTheConfigConvention() {
		Map<String, List<Class<?>>> classes = new LinkedHashMap<>();
		for (Module module : Modules.all()) {
			classes.put(module.getId(), List.of(module.getSwitchClass(), module.getSettingsClass()));
			Field field = module.getSwitchField();
			assertEquals("modules." + module.getId(), ConfigCodec.key(field));
		}
		assertEquals(List.of(), ConfigCodec.validate(classes, Set.of("plugins.disabled")));
	}

	@Test
	void moduleConfigFieldOrder() {
		List<String> keys = ModuleConfigs.getFields().stream().map(ConfigCodec::key).toList();
		List<String> expected = new ArrayList<>();
		for (String id : IDS) {
			expected.add("modules." + id);
		}
		expected.add("plugins.disabled");
		assertEquals(expected, keys.subList(0, expected.size()));
		// every module switch is on by default (descriptor default applied)
		for (Module module : Modules.all()) {
			assertEquals(module.isDefaultEnabled(), module.isEnabled(), module.getId());
		}
		assertTrue(ModuleConfigs.getScreenFields().stream().noneMatch(f -> ConfigCodec.key(f).startsWith("plugins.")));
	}

	@Test
	void modulePluginsKeepTheOldAddonIds() {
		assertEquals("emi_loot", PluginIds.resolve(new LootEmiPlugin(), "emi"));
		assertEquals("emi_ores", PluginIds.resolve(new OresEmiPlugin(), "emi"));
		assertEquals("emiffect", PluginIds.resolve(new EffectsEmiPlugin(), "emi"));
		assertEquals("emi_enchanting", PluginIds.resolve(new EnchantingEmiPlugin(), "emi"));
		assertEquals("emiprofessions", PluginIds.resolve(new ProfessionsEmiPlugin(), "emi"));
		assertEquals("emi_letsdo_compat", PluginIds.resolve(new LetsDoEmiPlugin(), "emi"));
		assertEquals(Modules.LOOT, new LootEmiPlugin().getModule());
	}

	@Test
	void fabricModJsonListsEntrypointsAndMixinConfigs() throws IOException {
		JsonObject mod = json("fabric.mod.json");
		JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
		assertTrue(strings(entrypoints.getAsJsonArray("main")).contains("dev.emi.remastered.EmiRemasteredMain"));
		assertTrue(strings(entrypoints.getAsJsonArray("client")).contains("dev.emi.remastered.EmiRemasteredClient"));
		Set<String> emi = strings(entrypoints.getAsJsonArray("emi"));
		for (Class<?> plugin : List.of(LootEmiPlugin.class, OresEmiPlugin.class, EffectsEmiPlugin.class,
				EnchantingEmiPlugin.class, ProfessionsEmiPlugin.class, LetsDoEmiPlugin.class)) {
			assertTrue(emi.contains(plugin.getName()), plugin.getName());
		}
		Set<String> mixins = strings(mod.getAsJsonArray("mixins"));
		for (String id : IDS) {
			String config = "emi_remastered_" + id + ".mixins.json";
			assertTrue(mixins.contains(config), config);
			JsonObject mixin = json(config);
			String pkg = "dev.emi.remastered." + id.replace("_", "") + ".mixin";
			assertEquals(pkg, mixin.get("package").getAsString(), config);
			assertEquals("JAVA_21", mixin.get("compatibilityLevel").getAsString(), config);
		}
	}

	@Test
	void switchAndGroupLangKeysExistInEnglishAndGerman() throws IOException {
		for (String lang : List.of("en_us", "de_de")) {
			JsonObject emi = json("assets/emi/lang/" + lang + ".json");
			assertTrue(emi.has("config.emi.group.modules"), lang);
			for (Module module : Modules.all()) {
				assertTrue(emi.has("config.emi.modules." + module.getId()), lang + " switch " + module.getId());
				assertTrue(emi.has("config.emi.tooltip.modules." + module.getId()), lang + " switch tooltip " + module.getId());
				JsonObject own = json("assets/" + module.getLangNamespace() + "/lang/" + lang + ".json");
				assertTrue(own.has(module.getNameKey()), lang + " " + module.getNameKey());
				for (Field field : ConfigCodec.configFields(module.getSettingsClass())) {
					String key = "config.emi." + ConfigCodec.key(field).replace('-', '_');
					assertTrue(own.has(key), lang + " " + key);
				}
			}
			JsonObject framework = json("assets/emi_remastered/lang/" + lang + ".json");
			assertTrue(framework.has("screen.emi_remastered.plugins.button"), lang);
		}
		assertEquals(json("assets/emi_remastered/lang/en_us.json").keySet(), json("assets/emi_remastered/lang/de_de.json").keySet());
	}
}
