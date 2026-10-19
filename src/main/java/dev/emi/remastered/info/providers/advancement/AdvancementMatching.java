package dev.emi.remastered.info.providers.advancement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure matching and filtering of advancements for the Item Info screen (no Minecraft classes, unit tested).
 * <p>
 * An advancement belongs to a stack when its display icon has the same key (the item; components are ignored).
 * Hidden advancements that are not done yet are never shown, exactly like the advancement screen.
 */
public final class AdvancementMatching {

	private AdvancementMatching() {
	}

	/**
	 * One advancement with a display, reduced to what the matching needs.
	 *
	 * @param id     advancement id (tie breaker for the order)
	 * @param key    what the icon is matched on (the icon's item)
	 * @param title  plain title, used for the order
	 * @param hidden the display's hidden flag
	 * @param done   whether the player completed it
	 * @param value  whatever the caller wants back with the match
	 */
	public record Candidate<K, V>(String id, K key, String title, boolean hidden, boolean done, V value) {
	}

	/** Whether the advancement screen would show the advancement: hidden ones only once they are done. */
	public static boolean isVisible(boolean hidden, boolean done) {
		return !hidden || done;
	}

	/**
	 * Groups the visible candidates by key. Within a key, open advancements come first, then done ones, each group by
	 * title (ignoring case) and id.
	 */
	public static <K, V> Map<K, List<Candidate<K, V>>> index(Collection<Candidate<K, V>> candidates) {
		Map<K, List<Candidate<K, V>>> map = new HashMap<>();
		for (Candidate<K, V> candidate : candidates) {
			if (candidate.key() != null && isVisible(candidate.hidden(), candidate.done())) {
				map.computeIfAbsent(candidate.key(), k -> new ArrayList<>()).add(candidate);
			}
		}
		Comparator<Candidate<K, V>> order = Comparator
			.comparing((Candidate<K, V> c) -> c.done())
			.thenComparing(c -> c.title().toLowerCase(Locale.ROOT))
			.thenComparing(Candidate::id);
		for (List<Candidate<K, V>> list : map.values()) {
			list.sort(order);
		}
		return map;
	}

	/**
	 * A number that changes when the visible advancements or their done state change (independent of the order of the
	 * candidates). Used to notice a changed advancement state without a listener.
	 */
	public static <K, V> long fingerprint(Collection<Candidate<K, V>> candidates) {
		long sum = 0;
		for (Candidate<K, V> candidate : candidates) {
			if (candidate.key() != null && isVisible(candidate.hidden(), candidate.done())) {
				sum += candidate.id().hashCode() * 31L + (candidate.done() ? 1 : 0) + 0x9E3779B97F4A7C15L;
			}
		}
		return sum;
	}
}
