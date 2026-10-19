package dev.emi.remastered.distractionfree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Matching of the "excluded screens" setting: a comma separated list of class names. An entry with a dot is a full
 * class name (Yarn names are accepted and translated with the mapper, so they also work in a production
 * environment); an entry without a dot is a simple class name, compared ignoring case. A screen matches when its
 * class or one of its super classes matches. Pure logic, no Minecraft classes.
 */
public final class ScreenFilter {
	private ScreenFilter() {
	}

	public static List<String> parse(String csv) {
		List<String> entries = new ArrayList<>();
		if (csv != null) {
			for (String part : csv.split(",")) {
				String entry = part.trim();
				if (!entry.isEmpty()) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	/**
	 * @param mapper translates a full (Yarn) class name to the running environment's class name; never null
	 */
	public static boolean matches(List<String> entries, Class<?> screenClass, UnaryOperator<String> mapper) {
		if (entries.isEmpty()) {
			return false;
		}
		for (Class<?> c = screenClass; c != null && c != Object.class; c = c.getSuperclass()) {
			String name = c.getName();
			String simple = c.getSimpleName();
			for (String entry : entries) {
				if (entry.indexOf('.') >= 0 || entry.indexOf('$') >= 0) {
					if (name.equals(entry) || name.equals(mapper.apply(entry))) {
						return true;
					}
				} else if (!simple.isEmpty() && simple.equalsIgnoreCase(entry)) {
					return true;
				}
			}
		}
		return false;
	}
}
