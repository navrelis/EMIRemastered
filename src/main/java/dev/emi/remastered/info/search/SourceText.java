package dev.emi.remastered.info.search;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Pure string helpers of the source search: normalising, joining and splitting labels. */
public final class SourceText {
	/** Separates the labels of one stack in the joined string, so a query never matches across two labels. */
	public static final char SEPARATOR = '\n';
	/** The query {@code ~nosource}: stacks that the Item Info screen marks as "No Known Source". */
	public static final String NO_SOURCE = "nosource";

	private SourceText() {
	}

	/** Lower case, trimmed; null becomes the empty string. */
	public static String normalize(String text) {
		return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
	}

	/** Adds the normalised label to the set unless it is blank. */
	public static void add(Set<String> labels, String label) {
		String normalized = normalize(label);
		if (!normalized.isEmpty()) {
			labels.add(normalized);
		}
	}

	public static void addAll(Set<String> labels, Collection<String> more) {
		for (String label : more) {
			add(labels, label);
		}
	}

	/** The labels, deduplicated in order, joined with {@link #SEPARATOR}; empty when there are none. */
	public static String join(Collection<String> labels) {
		Set<String> unique = new LinkedHashSet<>();
		addAll(unique, labels);
		return String.join(String.valueOf(SEPARATOR), unique);
	}

	/** {@code chests/stronghold_library} also as {@code chests stronghold library}, for queries typed with spaces. */
	public static String spaced(String path) {
		return path.replace('/', ' ').replace('_', ' ').replace('.', ' ');
	}

	/** Whether the query text is the {@link #NO_SOURCE} keyword. */
	public static boolean isNoSource(String query) {
		return normalize(query).equals(NO_SOURCE);
	}
}
