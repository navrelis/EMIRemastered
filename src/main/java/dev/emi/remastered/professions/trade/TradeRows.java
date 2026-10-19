package dev.emi.remastered.professions.trade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Decides how the trades of a stack are laid out on the Item Info page: one row per trade while there are only a few,
 * otherwise one row per owner (profession or wandering trader) so that items such as the emerald stay readable. Pure
 * logic, no Minecraft classes.
 */
public final class TradeRows {
	/** Up to this many trades each get their own row. */
	public static final int DETAIL_LIMIT = 10;
	/** At most this many owner rows are shown, the rest is reachable through the "show all" link. */
	public static final int GROUP_LIMIT = 24;

	private TradeRows() {
	}

	/** One owner with its trades in the order they were given. */
	public record Group<T>(String owner, List<T> trades) {
	}

	/** Whether {@code count} trades are shown one by one. */
	public static boolean detailed(int count) {
		return count <= DETAIL_LIMIT;
	}

	/** Groups the trades by owner, groups in order of first appearance. */
	public static <T> List<Group<T>> groupByOwner(List<? extends T> trades, Function<? super T, String> ownerOf) {
		Map<String, List<T>> map = new LinkedHashMap<>();
		for (T trade : trades) {
			map.computeIfAbsent(ownerOf.apply(trade), o -> new ArrayList<>()).add(trade);
		}
		List<Group<T>> groups = new ArrayList<>(map.size());
		for (Map.Entry<String, List<T>> e : map.entrySet()) {
			groups.add(new Group<>(e.getKey(), List.copyOf(e.getValue())));
		}
		return groups;
	}

	/**
	 * Picks up to {@code max} distinct candidates from the trades, one per trade first and then further ones, like the
	 * example stacks of the recipe rows.
	 */
	public static <T, I> List<I> distinct(List<? extends T> trades, Function<? super T, ? extends List<? extends I>> candidates, int max) {
		List<I> picked = new ArrayList<>();
		for (T trade : trades) {
			for (I candidate : candidates.apply(trade)) {
				if (picked.size() >= max) {
					return picked;
				}
				if (candidate != null && !picked.contains(candidate)) {
					picked.add(candidate);
				}
			}
		}
		return picked;
	}
}
