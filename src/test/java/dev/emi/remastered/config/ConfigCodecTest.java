package dev.emi.remastered.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.emi.emi.com.unascribed.qdcss.QDCSS;
import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

class ConfigCodecTest {

	/** Stands in for a module class: holds the switch. */
	public static class TestModule {
		@Comment("Switch of the test module.")
		@ConfigValue("modules.test")
		public static boolean enabled = true;
	}

	/** Stands in for a porter's settings class: plain public static fields. */
	public static class TestSettings {
		@Comment("A boolean setting with a comment that is long enough to be wrapped by the comment formatter at eighty characters.")
		@ConfigValue("test.show-things")
		public static boolean showThings = true;

		@ConfigValue("test.decimal-places")
		public static int decimalPlaces = 1;

		@ConfigValue("test.scale")
		public static double scale = 0.5;

		@ConfigValue("test.label")
		public static String label = "default";

		@ConfigValue("test.mode")
		public static Mode mode = Mode.FIRST;

		public static int notAConfigValue = 7;
	}

	public enum Mode {
		FIRST, SECOND_ONE
	}

	@BeforeEach
	void reset() {
		TestModule.enabled = true;
		TestSettings.showThings = true;
		TestSettings.decimalPlaces = 1;
		TestSettings.scale = 0.5;
		TestSettings.label = "default";
		TestSettings.mode = Mode.FIRST;
	}

	private static List<Field> fields() {
		List<Field> list = new ArrayList<>(ConfigCodec.configFields(TestSettings.class));
		list.addAll(ConfigCodec.configFields(TestModule.class));
		return ConfigCodec.orderByGroup(list, List.of("modules"));
	}

	private static Map<String, List<String>> write() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		ConfigCodec.write(groups, fields(), ConfigCodec.BASIC_WRITER, (m, t) -> { throw new AssertionError(m, t); });
		return groups;
	}

	/** Same file layout as EmiConfig.getSavedConfig. */
	private static String render(Map<String, List<String>> groups) {
		StringBuilder sb = new StringBuilder("/** EMI Config */\n\n");
		boolean first = true;
		for (Map.Entry<String, List<String>> e : groups.entrySet()) {
			if (!first) {
				sb.append("\n");
			}
			first = false;
			sb.append("#").append(e.getKey()).append(" {\n").append(String.join("\n", e.getValue())).append("}\n");
		}
		return sb.toString();
	}

	@Test
	void collectsOnlyAnnotatedPublicStaticFieldsInDeclarationOrder() {
		List<String> keys = ConfigCodec.configFields(TestSettings.class).stream().map(ConfigCodec::key).toList();
		assertEquals(List.of("test.show-things", "test.decimal-places", "test.scale", "test.label", "test.mode"), keys);
	}

	@Test
	void ordersLeadingGroupsFirstAndKeepsGroupsTogether() {
		List<String> keys = fields().stream().map(ConfigCodec::key).toList();
		assertEquals("modules.test", keys.get(0));
		assertEquals(6, keys.size());
	}

	@Test
	void loadsValuesFromQdcss() {
		QDCSS css = QDCSS.load("test", """
			#modules {
				test: false;
			}
			#test {
				show-things: false;
				decimal-places: 3;
				scale: 2.25;
				label: "hello world";
				mode: second-one;
			}
			""");
		Set<String> consumed = ConfigCodec.load(css, fields(), ConfigCodec.BASIC_READER, (m, t) -> { throw new AssertionError(m, t); });
		assertFalse(TestModule.enabled);
		assertFalse(TestSettings.showThings);
		assertEquals(3, TestSettings.decimalPlaces);
		assertEquals(2.25, TestSettings.scale);
		assertEquals("hello world", TestSettings.label);
		assertEquals(Mode.SECOND_ONE, TestSettings.mode);
		assertEquals(Set.of("modules.test", "test.show-things", "test.decimal-places", "test.scale", "test.label", "test.mode"), consumed);
	}

	@Test
	void unknownKeysAreNotConsumedSoEmiConfigPreservesThem() {
		QDCSS css = QDCSS.load("test", """
			#test {
				decimal-places: 4;
				removed-option: true;
			}
			#othermod {
				anything: "x";
			}
			""");
		Set<String> consumed = ConfigCodec.load(css, fields(), ConfigCodec.BASIC_READER, (m, t) -> { throw new AssertionError(m, t); });
		assertEquals(Set.of("test.decimal-places"), consumed);
		assertEquals(4, TestSettings.decimalPlaces);
		assertTrue(css.containsKey("test.removed-option"));
		assertTrue(css.containsKey("othermod.anything"));
	}

	@Test
	void badValueKeepsDefaultAndOtherKeysStillLoad() {
		QDCSS css = QDCSS.load("test", """
			#test {
				decimal-places: lots;
				scale: 3.0;
			}
			""");
		List<String> errors = new ArrayList<>();
		Set<String> consumed = ConfigCodec.load(css, fields(), ConfigCodec.BASIC_READER, (m, t) -> errors.add(m));
		assertEquals(1, TestSettings.decimalPlaces);
		assertEquals(3.0, TestSettings.scale);
		assertEquals(1, errors.size());
		assertTrue(consumed.contains("test.decimal-places"));
	}

	@Test
	void writesKeysValuesAndCommentsInEmiFormat() {
		Map<String, List<String>> groups = write();
		assertEquals(List.of("modules", "test"), List.copyOf(groups.keySet()));
		String text = render(groups);
		assertTrue(text.contains("#modules {\n\t/**\n\t * Switch of the test module.\n\t */\n\ttest: true;\n}"), text);
		assertTrue(text.contains("\tshow-things: true;\n"), text);
		assertTrue(text.contains("\tdecimal-places: 1;\n"), text);
		assertTrue(text.contains("\tscale: 0.5;\n"), text);
		assertTrue(text.contains("\tlabel: \"default\";\n"), text);
		assertTrue(text.contains("\tmode: first;\n"), text);
		assertFalse(text.contains("not-a-config-value") || text.contains("notAConfigValue"), text);
		for (String line : text.split("\n")) {
			if (line.startsWith("\t * ")) {
				assertTrue(line.length() - 4 <= 80, "comment line too long: " + line);
			}
		}
	}

	@Test
	void roundTripThroughTheFileFormat() {
		TestModule.enabled = false;
		TestSettings.showThings = false;
		TestSettings.decimalPlaces = 9;
		TestSettings.scale = -1.75;
		TestSettings.label = "a b c";
		TestSettings.mode = Mode.SECOND_ONE;
		String text = render(write());
		reset();
		ConfigCodec.load(QDCSS.load("roundtrip", text), fields(), ConfigCodec.BASIC_READER, (m, t) -> { throw new AssertionError(m, t); });
		assertFalse(TestModule.enabled);
		assertFalse(TestSettings.showThings);
		assertEquals(9, TestSettings.decimalPlaces);
		assertEquals(-1.75, TestSettings.scale);
		assertEquals("a b c", TestSettings.label);
		assertEquals(Mode.SECOND_ONE, TestSettings.mode);
	}

	public static class BadSettings {
		@ConfigValue("modules.other")
		public static boolean stolenSwitch = true;

		@ConfigValue("wronggroup.key")
		public static int wrongGroup = 1;

		@ConfigValue("nodot")
		public static int noDot = 1;

		@ConfigValue("test.show-things")
		public static boolean duplicate = true;
	}

	@Test
	void validationAcceptsTheConventionAndReportsViolations() {
		Map<String, List<Class<?>>> good = new LinkedHashMap<>();
		good.put("test", List.of(TestModule.class, TestSettings.class));
		assertEquals(List.of(), ConfigCodec.validate(good, Set.of()));

		Map<String, List<Class<?>>> bad = new LinkedHashMap<>();
		bad.put("test", List.of(TestModule.class, TestSettings.class, BadSettings.class));
		List<String> problems = ConfigCodec.validate(bad, Set.of());
		assertEquals(4, problems.size(), problems.toString());

		Map<String, List<Class<?>>> noSwitch = new LinkedHashMap<>();
		noSwitch.put("test", List.of(TestSettings.class));
		assertEquals(1, ConfigCodec.validate(noSwitch, Set.of()).size());
	}

	@Test
	void keyParts() {
		assertEquals("loot", ConfigCodec.group("loot.show-chances"));
		assertEquals("show-chances", ConfigCodec.name("loot.show-chances"));
	}
}
