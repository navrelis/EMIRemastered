package dev.emi.remastered.info.search;

import java.util.regex.Pattern;

/** Pure matching rules of the source search (unit tested without Minecraft). */
public final class SourceMatch {

	private SourceMatch() {
	}

	/**
	 * The plain query: a case-insensitive substring of any label, or for {@code ~nosource} the no-source flag.
	 *
	 * @param labels joined, normalised labels of the stack ({@link SourceText#join})
	 * @param needle the query text, already normalised
	 */
	public static boolean matches(String labels, boolean noSource, String needle, boolean noSourceKeyword) {
		if (noSourceKeyword) {
			return noSource;
		}
		if (needle.isEmpty()) {
			return !labels.isEmpty();
		}
		return labels.contains(needle);
	}

	/** Compiles a regex query; an invalid pattern matches nothing (null), like EMI's other regex queries. */
	public static Pattern compile(String regex) {
		try {
			return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** The regex query: found in the joined labels (a label ends at a line break, {@code .} does not cross it). */
	public static boolean matchesRegex(Pattern pattern, String labels) {
		return pattern != null && pattern.matcher(labels).find();
	}

	/**
	 * The "no known source" rule of the Item Info screen for one recipe in the by-output index of a stack: it counts as a
	 * source when its category is not an information category, it produces the stack and does not also consume it
	 * (repairing, cloning, dyeing...).
	 */
	public static boolean countsAsSource(boolean excludedCategory, boolean produces, boolean consumes) {
		return !excludedCategory && produces && !consumes;
	}
}
