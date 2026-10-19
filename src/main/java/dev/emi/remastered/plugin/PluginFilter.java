package dev.emi.remastered.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/** Pure logic of the persisted set of disabled plugin ids ({@code plugins.disabled} in emi.css). */
public final class PluginFilter {
	/** EMI's own plugin (VanillaPlugin); can never be disabled. */
	public static final String CORE_ID = "emi";

	private PluginFilter() {
	}

	/** Parses {@code "a, b c"} into ids; drops blanks and {@link #CORE_ID}. */
	public static Set<String> parse(String value) {
		Set<String> set = new LinkedHashSet<>();
		if (value == null) {
			return set;
		}
		for (String part : value.split("[,\\s]+")) {
			String id = part.trim();
			if (!id.isEmpty() && canDisable(id)) {
				set.add(id);
			}
		}
		return set;
	}

	/** Canonical, sorted, comma separated form. */
	public static String format(Collection<String> ids) {
		TreeSet<String> sorted = new TreeSet<>();
		for (String id : ids) {
			if (id != null && !id.isBlank() && canDisable(id.trim())) {
				sorted.add(id.trim());
			}
		}
		return String.join(", ", sorted);
	}

	public static boolean canDisable(String id) {
		return !CORE_ID.equals(id);
	}

	public static boolean isDisabled(String id, Set<String> disabled) {
		return canDisable(id) && disabled.contains(id);
	}

	/**
	 * Removes the disabled entries from {@code list} in place, keeping the order of the others.
	 *
	 * @return the removed entries
	 */
	public static <T> List<T> removeDisabled(List<T> list, Function<T, String> idOf, Set<String> disabled) {
		List<T> removed = new ArrayList<>();
		Iterator<T> it = list.iterator();
		while (it.hasNext()) {
			T t = it.next();
			if (isDisabled(idOf.apply(t), disabled)) {
				removed.add(t);
				it.remove();
			}
		}
		return removed;
	}

	/** Returns the new config value after switching {@code id} on or off. */
	public static String withDisabled(String current, String id, boolean disabled) {
		Set<String> set = parse(current);
		if (disabled && canDisable(id)) {
			set.add(id);
		} else {
			set.remove(id);
		}
		return format(set);
	}
}
