package dev.emi.remastered.loot.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The worker threads of one structure lookup build. Reading and decompressing the structure templates is nearly all of
 * the build's time (about ten seconds for the thousand structures of a large pack when done one after another), and
 * those reads are independent, so every structure's walk through its pools and templates runs as one task and the
 * templates of different structures are read at the same time. Pure Java.
 * <p>
 * The pool is the build's own, small and bounded ({@link #defaultThreads}): never the common ForkJoinPool or
 * Minecraft's worker executor, which chunk generation, data pack reloads and other mods use; a few seconds of blocking
 * reads there could hold up world generation. The threads are daemons of normal priority, so they never keep the JVM
 * alive, and end when the build ends ({@link #close}); their small number is what leaves room for the game.
 * <p>
 * Results stay exactly those of reading the structures one after another: each walk is deterministic on its own
 * (its own seeded random, its own limits), {@link OnceCache} only decides which thread reads a template, and
 * {@link #walkInOrder} merges the results in the order of the structures, whatever order the walks finish in.
 * <p>
 * Ending a build never interrupts a worker: interrupting a thread while it reads from a jar's zip file system closes the
 * file channel of that file system for the whole game. A stale or ended build cancels the walks that did not start yet,
 * and the running ones stop at their next {@link #aborted} check (between two pools).
 */
final class StructureWalks implements AutoCloseable {
	static final String THREAD_NAME = "EMI Loot structure lookup worker";

	private final ThreadPoolExecutor executor;
	private final BooleanSupplier abandoned;
	private final List<CompletableFuture<?>> submitted = new ArrayList<>();
	private volatile boolean closed;

	/** How a walk's outcome is merged: its result, or the exception that ended it (then the result is null). */
	interface Merge<T, R> {
		void accept(T item, R result, Throwable failure);
	}

	/**
	 * @param threads number of worker threads
	 * @param abandoned whether the build's results are no longer wanted (the data changed or the server stops); asked
	 * by the workers between two pools, so it must be cheap, lock free and, once true, stay true
	 */
	StructureWalks(int threads, BooleanSupplier abandoned) {
		this.abandoned = abandoned;
		AtomicInteger count = new AtomicInteger();
		this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
			Thread thread = new Thread(runnable, THREAD_NAME + " " + count.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Two processors stay free for the server thread and the rest of the game (the client's render thread in single
	 * player). Four threads bring the build of a large pack from about ten seconds to about two; more would take further
	 * cores from the game while it starts for little gain, as the reads then wait for each other.
	 */
	static int defaultThreads() {
		return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2));
	}

	/** Whether the walks should stop: the build was abandoned or ended. */
	boolean aborted() {
		return closed || abandoned.getAsBoolean();
	}

	/** Starts a walk on a worker thread. Only the thread that owns this build submits. */
	<R> CompletableFuture<R> submit(Supplier<R> walk) {
		CompletableFuture<R> future = CompletableFuture.supplyAsync(walk, executor);
		submitted.add(future);
		return future;
	}

	/**
	 * Starts the walks of all items at once and merges their outcomes on this thread in the order of {@code items}, as
	 * if they had been walked one after another. An item without a walk ({@code walkOf} gives null) is merged with a
	 * null result. A walk that throws is merged with its exception and the others go on, like a sequential loop that
	 * catches per item. Stops without merging further once the build is {@link #aborted}, also right after a walk
	 * ended, since a walk that saw the abort ends early with a partial result.
	 *
	 * @return true when every item was merged, false when the build was aborted
	 */
	<T, R> boolean walkInOrder(List<T> items, Function<T, Supplier<R>> walkOf, Merge<T, R> merge) {
		List<CompletableFuture<R>> walks = new ArrayList<>(items.size());
		for (T item : items) {
			Supplier<R> walk = walkOf.apply(item);
			walks.add(walk == null ? null : submit(walk));
		}
		for (int i = 0; i < items.size(); i++) {
			if (aborted()) {
				return false;
			}
			CompletableFuture<R> walk = walks.get(i);
			R result = null;
			Throwable failure = null;
			if (walk != null) {
				try {
					result = walk.join();
				} catch (CompletionException e) {
					failure = e.getCause() == null ? e : e.getCause();
				}
				if (aborted()) {
					return false;
				}
			}
			merge.accept(items.get(i), result, failure);
		}
		return true;
	}

	/**
	 * Ends the build's walks: the ones that did not start are cancelled (a cancelled task is skipped when its turn
	 * comes), running ones stop at their next {@link #aborted} check, and the threads end once the queue is empty.
	 * {@link #aborted} turns true before the cancelling starts, so a walk that a worker takes meanwhile stops before
	 * its first read. Never blocks and never interrupts a running walk ({@link ThreadPoolExecutor#shutdown} only wakes
	 * idle threads, and a worker clears that wake-up before it runs its next task).
	 */
	@Override
	public void close() {
		closed = true;
		for (CompletableFuture<?> future : submitted) {
			future.cancel(false);
		}
		executor.shutdown();
	}

	/** Waits until every worker thread ended, for tests. */
	boolean awaitTermination(long millis) throws InterruptedException {
		return executor.awaitTermination(millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * A cache that reads every template once although several walks need it: the value is computed on the thread that
	 * asks for it first, and threads that ask for the same key meanwhile wait for that computation instead of repeating
	 * it. Different keys are computed at the same time. (Not {@link ConcurrentHashMap#computeIfAbsent}: that would hold
	 * a lock of the map, and so block other keys, during a slow read.)
	 * <p>
	 * A computation that throws is not kept, like with {@code computeIfAbsent} of the plain map the sequential lookup
	 * used: its exception goes to the thread that computed it, and whoever asks for that key later (or was waiting for
	 * it) computes it again. The computation must not ask the cache for another key.
	 */
	static final class OnceCache<K, V> {
		private final ConcurrentHashMap<K, CompletableFuture<V>> values = new ConcurrentHashMap<>();
		private final Function<K, V> compute;

		OnceCache(Function<K, V> compute) {
			this.compute = compute;
		}

		V get(K key) {
			while (true) {
				CompletableFuture<V> existing = values.get(key);
				if (existing == null) {
					CompletableFuture<V> mine = new CompletableFuture<>();
					existing = values.putIfAbsent(key, mine);
					if (existing == null) {
						try {
							V value = compute.apply(key);
							mine.complete(value);
							return value;
						} catch (Throwable e) {
							// removed before the waiters wake up, so they compute it again instead of finding this failure
							values.remove(key, mine);
							mine.completeExceptionally(e);
							throw e;
						}
					}
				}
				try {
					return existing.join();
				} catch (CompletionException | CancellationException e) {
					// the computation we waited for failed and is gone: compute it (or wait for a new one) again
				}
			}
		}

		int size() {
			return values.size();
		}
	}
}
