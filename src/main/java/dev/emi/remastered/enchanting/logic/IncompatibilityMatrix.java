package dev.emi.remastered.enchanting.logic;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Which elements cannot be combined with which, computed once (every unordered pair is asked once in each direction,
 * so about n^2 calls). The result is symmetric even when the predicate is not, which mods that mix into
 * {@code Enchantment.canBeCombined} can cause: a pair counts as incompatible if either direction says so.
 * Pure logic, generic so it can be tested without Minecraft. Elements are compared by identity.
 */
public final class IncompatibilityMatrix<T> {
	private final Map<T, List<T>> incompatible = new IdentityHashMap<>();

	private IncompatibilityMatrix() {
	}

	/**
	 * @param elements all elements, in display order (the lists of the result keep this order)
	 * @param combinable whether the first element can be combined with the second
	 * @param onError called for a pair whose test threw; such a test counts as "can be combined"
	 */
	public static <T> IncompatibilityMatrix<T> build(List<T> elements, BiPredicate<T, T> combinable, BiConsumer<T, Throwable> onError) {
		int size = elements.size();
		boolean[][] bad = new boolean[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = i + 1; j < size; j++) {
				T a = elements.get(i);
				T b = elements.get(j);
				bad[i][j] = !test(a, b, combinable, onError) || !test(b, a, combinable, onError);
			}
		}
		IncompatibilityMatrix<T> matrix = new IncompatibilityMatrix<>();
		for (int i = 0; i < size; i++) {
			List<T> list = new ArrayList<>();
			for (int j = 0; j < size; j++) {
				if (i != j && (i < j ? bad[i][j] : bad[j][i])) {
					list.add(elements.get(j));
				}
			}
			matrix.incompatible.put(elements.get(i), list);
		}
		return matrix;
	}

	private static <T> boolean test(T a, T b, BiPredicate<T, T> combinable, BiConsumer<T, Throwable> onError) {
		try {
			return combinable.test(a, b);
		} catch (Throwable t) {
			onError.accept(a, t);
			return true;
		}
	}

	/** @return the elements that cannot be combined with {@code element}, in display order; empty for an unknown one */
	public List<T> incompatibleWith(T element) {
		return incompatible.getOrDefault(element, List.of());
	}

	public boolean areIncompatible(T a, T b) {
		return incompatibleWith(a).contains(b);
	}
}
