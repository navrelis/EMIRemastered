package dev.emi.remastered.loot.structure;

import java.util.ArrayList;
import java.util.List;
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
 * The worker threads of one structure lookup build: every structure's walk through its pools and templates runs as one
 * task, so the templates (read from the resource packs and decompressed, which is most of the build's time) of
 * different structures are read at the same time. Pure Java.
 * <p>
 * The pool is the build's own, small and bounded ({@link #defaultThreads}): never the common ForkJoinPool or
 * Minecraft's worker executor, which chunk generation, data pack reloads and other mods use; a few seconds of blocking
 * reads there could hold up world generation. The threads are daemons, so they never keep the JVM alive, and end when
 * the build ends ({@link #close}). They keep the normal priority of the lookup thread: a lower one puts them behind
 * everything else on a busy machine (on Windows also on the slower cores of a hybrid processor), which made the
 * build several times slower in a measurement; their small number is what leaves room for the game.
 * <p>
 * Results stay exactly those of reading the structures one after another: each walk is deterministic on its own
 * (its own seeded random, its own limits), {@link OnceCache} only decides which thread reads a template, and the caller
 * merges the results in the order it submitted the walks.
 * <p>
 * Cancelling never interrupts a worker: interrupting a thread while it reads from a jar's zip file system closes the
 * channel of that file system for the whole game. A stale or ended build cancels the walks that did not start yet, and
 * the running ones stop at their next {@link #aborted} check.
 */
final class StructureWalks implements AutoCloseable {
	static final String THREAD_NAME = "EMI Loot structure lookup worker";

	private final ThreadPoolExecutor executor;
	private final BooleanSupplier stale;
	private final List<CompletableFuture<?>> submitted = new ArrayList<>();
	private volatile boolean closed;

	/**
	 * @param threads number of worker threads
	 * @param stale whether the data changed since the build started; checked by the workers, so it must be cheap
	 */
	StructureWalks(int threads, BooleanSupplier stale) {
		this.stale = stale;
		AtomicInteger count = new AtomicInteger();
		this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
			Thread thread = new Thread(runnable, THREAD_NAME + " " + count.incrementAndGet());
			thread.setDaemon(true);
			thread.setPriority(Integer.getInteger("emi.xpar.prio", Thread.NORM_PRIORITY)); // TEMP-XPAR knob
			return thread;
		});
	}

	/**
	 * Two processors stay free for the server thread and the rest of the game (the client's render thread in single
	 * player). Four threads already bring the build of a large pack to about a second; more would take further cores
	 * from the game while it starts for a few hundred milliseconds less.
	 */
	static int defaultThreads() {
		return Integer.getInteger("emi.xpar.threads", Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2))); // TEMP-XPAR knob
	}

	/** Whether the walks should stop: the build became stale or ended. Workers check it between pools. */
	boolean aborted() {
		return closed || stale.getAsBoolean();
	}

	/** Starts a walk on a worker thread. Only the thread that owns this build submits. */
	<R> CompletableFuture<R> submit(Supplier<R> walk) {
		CompletableFuture<R> future = CompletableFuture.supplyAsync(walk, executor);
		submitted.add(future);
		return future;
	}

	/**
	 * Ends the build's walks: the ones that did not start are cancelled (a cancelled task is skipped when its turn
	 * comes), running ones stop at their next {@link #aborted} check, and the threads end once the queue is empty.
	 * Never blocks and never interrupts.
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
	 * A cache that computes every value once, on the thread that asks for it first; threads that ask for the same key
	 * meanwhile wait for that computation instead of repeating it. A failed computation fails for everyone who asks for
	 * that key, like a deterministic read that fails again would. Different keys are computed at the same time.
	 * (Not {@link ConcurrentHashMap#computeIfAbsent}: that would hold a lock of the map during a slow read.)
	 */
	static final class OnceCache<K, V> {
		private final ConcurrentHashMap<K, CompletableFuture<V>> values = new ConcurrentHashMap<>();
		private final Function<K, V> compute;

		OnceCache(Function<K, V> compute) {
			this.compute = compute;
		}

		V get(K key) {
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
						mine.completeExceptionally(e);
						throw e;
					}
				}
			}
			try {
				return existing.join();
			} catch (CompletionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException runtime) {
					throw runtime;
				}
				if (cause instanceof Error error) {
					throw error;
				}
				throw e;
			}
		}

		java.util.Set<K> keys() { return values.keySet(); } // TEMP-XPAR

		int size() {
			return values.size();
		}
	}
}
