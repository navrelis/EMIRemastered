package dev.emi.remastered.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import dev.emi.emi.com.unascribed.qdcss.QDCSS;
import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;

/**
 * Pure (Minecraft-free) part of the module config support: collects and orders the annotated fields of module config
 * classes, reads them from a parsed {@code emi.css} and writes them in EMI's file format.
 * <p>
 * Reading and writing of single values is delegated to a {@link FieldReader}/{@link FieldWriter}; in the game these
 * are EMI's own setters/writers ({@code EmiConfig.assignField/writeField}), so module settings support every type
 * EMI supports (boolean, int, double, String, EmiBind, ConfigEnum, IntGroup, ...). {@link #BASIC_READER} and
 * {@link #BASIC_WRITER} implement the same format for boolean/int/double/String/enum and are used by unit tests.
 */
public final class ConfigCodec {
	private static final int COMMENT_WIDTH = 80;

	private ConfigCodec() {
	}

	@FunctionalInterface
	public interface FieldReader {
		void read(QDCSS css, String key, Field field) throws Exception;
	}

	@FunctionalInterface
	public interface FieldWriter {
		/** Returns the field's lines in the form {@code "\t<key>: <value>;\n"}. */
		String write(String key, Field field) throws Exception;
	}

	/** Group part of a config key ({@code loot.show-chances} -> {@code loot}). */
	public static String group(String key) {
		int dot = key.indexOf('.');
		return dot < 0 ? key : key.substring(0, dot);
	}

	/** Key part of a config key ({@code loot.show-chances} -> {@code show-chances}). */
	public static String name(String key) {
		int dot = key.indexOf('.');
		return dot < 0 ? key : key.substring(dot + 1);
	}

	public static String key(Field field) {
		ConfigValue annot = field.getAnnotation(ConfigValue.class);
		return annot == null ? null : annot.value();
	}

	/** Public static fields with {@code @ConfigValue}, in declaration order. */
	public static List<Field> configFields(Class<?> clazz) {
		List<Field> list = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			int mod = field.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isPublic(mod)
					&& field.isAnnotationPresent(ConfigValue.class)) {
				list.add(field);
			}
		}
		return list;
	}

	/**
	 * Orders fields so that the groups in {@code leadingGroups} come first (in that order) and all other groups follow
	 * in order of first appearance. Fields of one group stay together and keep their relative order, so the config
	 * screen shows one header per group.
	 */
	public static List<Field> orderByGroup(List<Field> fields, List<String> leadingGroups) {
		Map<String, List<Field>> byGroup = new LinkedHashMap<>();
		for (String g : leadingGroups) {
			byGroup.put(g, new ArrayList<>());
		}
		for (Field field : fields) {
			byGroup.computeIfAbsent(group(key(field)), g -> new ArrayList<>()).add(field);
		}
		List<Field> ordered = new ArrayList<>();
		byGroup.values().forEach(ordered::addAll);
		return ordered;
	}

	/**
	 * Assigns every field whose key is present in {@code css}.
	 *
	 * @return the keys that were consumed (all others stay "unparsed" in EMI's config and are written back)
	 */
	public static Set<String> load(QDCSS css, List<Field> fields, FieldReader reader, BiConsumer<String, Throwable> errorLog) {
		Set<String> consumed = new HashSet<>();
		for (Field field : fields) {
			String key = key(field);
			if (key != null && css.containsKey(key)) {
				consumed.add(key);
				try {
					reader.read(css, key, field);
				} catch (Throwable t) {
					errorLog.accept("Invalid value for config key " + key + ", keeping " + safeValue(field), t);
				}
			}
		}
		return consumed;
	}

	/**
	 * Appends the fields to {@code groups} (group name -> entries) in EMI's format: an optional {@code /** comment *\/}
	 * block followed by {@code key: value;} lines, one entry per field.
	 */
	public static void write(Map<String, List<String>> groups, List<Field> fields, FieldWriter writer,
			BiConsumer<String, Throwable> errorLog) {
		for (Field field : fields) {
			String key = key(field);
			if (key == null) {
				continue;
			}
			Comment comment = field.getAnnotation(Comment.class);
			String text = comment == null ? "" : formatComment(comment.value());
			try {
				text += writer.write(name(key), field);
			} catch (Throwable t) {
				errorLog.accept("Error serializing config key " + key, t);
			}
			groups.computeIfAbsent(group(key), g -> new ArrayList<>()).add(text);
		}
	}

	/** EMI's comment block: {@code \t/**}, lines of at most 80 characters prefixed with {@code \t * }, {@code \t *\/}. */
	public static String formatComment(String comment) {
		StringBuilder sb = new StringBuilder("\t/**\n");
		for (String line : wrap(comment, COMMENT_WIDTH)) {
			sb.append("\t * ").append(line).append("\n");
		}
		sb.append("\t */\n");
		return sb.toString();
	}

	static List<String> wrap(String text, int width) {
		List<String> lines = new ArrayList<>();
		for (String paragraph : text.split("\n", -1)) {
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.split(" ")) {
				if (line.length() > 0 && line.length() + 1 + word.length() > width) {
					lines.add(line.toString());
					line.setLength(0);
				}
				if (line.length() > 0) {
					line.append(' ');
				}
				line.append(word);
			}
			lines.add(line.toString());
		}
		return lines;
	}

	/**
	 * Checks the module config conventions: keys are {@code group.key}; each module has exactly one switch
	 * {@code modules.<id>} (public static boolean, in the module class); all other keys use the module id as group;
	 * no key appears twice.
	 *
	 * @param classesById module id -> classes with that module's {@code @ConfigValue} fields (module class, settings class)
	 * @param reservedKeys keys used by the framework itself
	 * @return human readable problems, empty if everything is fine
	 */
	public static List<String> validate(Map<String, List<Class<?>>> classesById, Set<String> reservedKeys) {
		List<String> problems = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>(reservedKeys);
		for (Map.Entry<String, List<Class<?>>> entry : classesById.entrySet()) {
			String id = entry.getKey();
			int switches = 0;
			for (Class<?> clazz : new LinkedHashSet<>(entry.getValue())) {
				for (Field field : configFields(clazz)) {
					String where = clazz.getSimpleName() + "." + field.getName();
					String key = key(field);
					if (key.indexOf('.') <= 0 || key.indexOf('.') != key.lastIndexOf('.')) {
						problems.add(where + ": key '" + key + "' must be 'group.key'");
						continue;
					}
					if (!seen.add(key)) {
						problems.add(where + ": duplicate key '" + key + "'");
					}
					String group = group(key);
					if (group.equals("modules")) {
						if (key.equals("modules." + id) && field.getType() == boolean.class) {
							switches++;
						} else {
							problems.add(where + ": only the boolean switch 'modules." + id + "' may use the group 'modules'");
						}
					} else if (!group.equals(id)) {
						problems.add(where + ": key '" + key + "' must use the module id '" + id + "' as group");
					}
				}
			}
			if (switches == 0) {
				problems.add("module " + id + ": missing '@ConfigValue(\"modules." + id + "\") public static boolean enabled'");
			}
		}
		return problems;
	}

	private static String safeValue(Field field) {
		try {
			return String.valueOf(field.get(null));
		} catch (Throwable t) {
			return "?";
		}
	}

	/** Reader for boolean, int, double, String and enums (by {@code name().toLowerCase()}), same format as EMI. */
	public static final FieldReader BASIC_READER = (css, key, field) -> {
		Class<?> type = field.getType();
		if (type == boolean.class) {
			field.setBoolean(null, css.getBoolean(key).get());
		} else if (type == int.class) {
			field.setInt(null, css.getInt(key).get());
		} else if (type == double.class) {
			field.setDouble(null, css.getDouble(key).get());
		} else if (type == String.class) {
			String s = css.get(key).get();
			field.set(null, s.substring(1, s.length() - 1));
		} else if (type.isEnum()) {
			String s = css.get(key).get();
			for (Object constant : type.getEnumConstants()) {
				if (((Enum<?>) constant).name().equalsIgnoreCase(s.replace('-', '_'))) {
					field.set(null, constant);
				}
			}
		} else {
			throw new IllegalArgumentException("Unsupported config type " + type);
		}
	};

	/** Writer matching {@link #BASIC_READER}. */
	public static final FieldWriter BASIC_WRITER = (key, field) -> {
		Object value = field.get(null);
		String text;
		if (value instanceof String s) {
			text = "\"" + s + "\"";
		} else if (value instanceof Enum<?> e) {
			text = e.name().toLowerCase().replace('_', '-');
		} else {
			text = String.valueOf(value);
		}
		return "\t" + key + ": " + text + ";\n";
	};
}
