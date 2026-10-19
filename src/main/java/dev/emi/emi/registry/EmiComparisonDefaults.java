package dev.emi.emi.registry;

import java.util.Map;

import dev.emi.emi.api.stack.Comparison;

public class EmiComparisonDefaults {
	public static Map<Object, Comparison> comparisons = Map.of();

	public static Comparison get(Object obj) {
		// EMI Remastered: one lookup instead of containsKey + get (same result, this is on the recipe index hot path)
		return comparisons.getOrDefault(obj, Comparison.DEFAULT_COMPARISON);
	}
}
