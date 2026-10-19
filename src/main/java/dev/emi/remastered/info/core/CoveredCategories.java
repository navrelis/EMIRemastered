package dev.emi.remastered.info.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Collects the recipe categories that providers cover themselves, so that the built-in recipe provider can leave their
 * rows out. Pure Java (unit tested); the provider calls it once per page build.
 */
public final class CoveredCategories {

	private CoveredCategories() {
	}

	/**
	 * Union of the categories covered by every provider that is {@code active}. A provider whose {@code covered}
	 * function throws covers nothing (its own error is reported when it is collected).
	 *
	 * @param providers all registered providers
	 * @param active    whether a provider takes part for the current page (module on, setting on)
	 * @param covered   the categories a provider covers for the current page
	 */
	public static <P, K> Set<K> compute(List<? extends P> providers, Predicate<? super P> active,
			Function<? super P, ? extends Set<? extends K>> covered) {
		Set<K> result = new HashSet<>();
		for (P provider : providers) {
			try {
				if (active.test(provider)) {
					Set<? extends K> set = covered.apply(provider);
					if (set != null) {
						result.addAll(set);
					}
				}
			} catch (RuntimeException | LinkageError e) {
				// covers nothing
			}
		}
		return result;
	}
}
