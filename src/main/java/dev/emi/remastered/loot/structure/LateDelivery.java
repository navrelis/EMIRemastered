package dev.emi.remastered.loot.structure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Who still waits for data that is built in the background, for one generation of that data. A key added for a newer
 * generation drops the keys of the older one (their data will never come). The first key of a generation "arms" the
 * delivery: the caller then registers one completion callback, which takes the keys. Pure Java, thread safe.
 *
 * @param <K> what identifies a receiver (the player's UUID)
 */
public final class LateDelivery<K> {
	private final Set<K> keys = new LinkedHashSet<>();
	private int generation = Integer.MIN_VALUE;
	private boolean armed;

	/**
	 * Adds a receiver that got the data of {@code generation} without the late part.
	 *
	 * @return true when the caller has to register the completion callback for this generation (first key since the
	 * generation started or since the last {@link #take})
	 */
	public synchronized boolean add(int generation, K key) {
		if (generation != this.generation) {
			this.generation = generation;
			keys.clear();
			armed = false;
		}
		keys.add(key);
		if (armed) {
			return false;
		}
		armed = true;
		return true;
	}

	/** The receiver got the complete data (or must not get the late part any more). */
	public synchronized void remove(K key) {
		keys.remove(key);
	}

	/** The receivers waiting for the late part of {@code generation}, which are removed; empty for another generation. */
	public synchronized List<K> take(int generation) {
		if (generation != this.generation) {
			return List.of();
		}
		List<K> result = List.copyOf(keys);
		keys.clear();
		armed = false;
		return result;
	}

	/** Forgets everything (server stop). */
	public synchronized void clear() {
		keys.clear();
		armed = false;
		generation = Integer.MIN_VALUE;
	}

	public synchronized int size() {
		return keys.size();
	}
}
