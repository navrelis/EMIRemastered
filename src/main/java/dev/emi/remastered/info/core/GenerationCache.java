package dev.emi.remastered.info.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Small LRU cache whose entries are only valid for one "generation" (one EMI reload). Asking with a different
 * generation drops every entry first. Pure Java, no Minecraft classes (unit tested).
 * <p>
 * Not thread safe: the info module only uses it on the client thread.
 *
 * @param <K> key type (needs {@code equals}/{@code hashCode})
 * @param <V> value type
 */
public final class GenerationCache<K, V> {
	/** Generation value that never matches: computing with it never stores anything. */
	public static final long NO_CACHE = Long.MIN_VALUE;

	private final int maxSize;
	private final LinkedHashMap<K, V> entries;
	private long generation = NO_CACHE;

	public GenerationCache(int maxSize) {
		if (maxSize < 1) {
			throw new IllegalArgumentException("maxSize must be >= 1");
		}
		this.maxSize = maxSize;
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > GenerationCache.this.maxSize;
			}
		};
	}

	/**
	 * Returns the cached value for {@code key} in {@code generation}, computing (and storing) it when missing.
	 * A different generation than the last one clears the cache. {@link #NO_CACHE} computes without storing.
	 */
	public V get(long generation, K key, Function<? super K, ? extends V> compute) {
		if (generation == NO_CACHE) {
			return compute.apply(key);
		}
		if (generation != this.generation) {
			entries.clear();
			this.generation = generation;
		}
		V value = entries.get(key);
		if (value == null) {
			value = compute.apply(key);
			if (value != null) {
				entries.put(key, value);
			}
		}
		return value;
	}

	/** The cached value or null, without computing. Also null when the generation differs. */
	public V peek(long generation, K key) {
		if (generation != this.generation) {
			return null;
		}
		return entries.get(key);
	}

	/** Drops every entry. */
	public void invalidate() {
		entries.clear();
		generation = NO_CACHE;
	}

	public int size() {
		return entries.size();
	}

	public long generation() {
		return generation;
	}
}
