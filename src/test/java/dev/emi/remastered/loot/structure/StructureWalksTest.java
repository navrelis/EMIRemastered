package dev.emi.remastered.loot.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * The parallel structure walks of the structure lookup must give exactly the result of walking the structures one after
 * another. A fake template graph stands in for the structure templates: every template names loot tables and the
 * templates it leads to, and a walk is a breadth-first search with its own seeded random and a template limit, like
 * the lookup's walk through pools. The merge is {@link StructureWalks#walkInOrder}, the one the lookup uses.
 */
class StructureWalksTest {
	private static final int WALK_LIMIT = 60;

	private record Template(List<String> loot, List<String> next) {
	}

	/** The outcome of all walks, merged in walk order: loot table -> walks in the order they were added, and errors. */
	private record Merged(Map<String, List<Integer>> byTable, Map<Integer, String> errors) {
	}

	private static Map<String, Template> graph(long seed, int templates) {
		Random random = new Random(seed);
		Map<String, Template> graph = new HashMap<>();
		for (int i = 0; i < templates; i++) {
			List<String> loot = new ArrayList<>();
			for (int n = random.nextInt(3); n > 0; n--) {
				loot.add("chests/t" + random.nextInt(80));
			}
			List<String> next = new ArrayList<>();
			for (int n = random.nextInt(5); n > 0; n--) {
				next.add("t" + random.nextInt(templates + 20)); // some templates are missing, like a broken pack
			}
			graph.put("t" + i, new Template(loot, next));
		}
		return graph;
	}

	/** Mirrors the lookup's walk: queue order, shuffled with a per walk random, limits, early stop when aborted. */
	private static Set<String> walk(int start, Function<String, Template> templates, BooleanSupplier aborted) {
		Set<String> tables = new LinkedHashSet<>();
		Set<String> visited = new HashSet<>();
		Deque<String> queue = new ArrayDeque<>();
		queue.add("t" + start);
		Random random = new Random(0);
		while (!queue.isEmpty() && visited.size() < WALK_LIMIT) {
			if (aborted.getAsBoolean()) {
				return tables;
			}
			String id = queue.poll();
			if (!visited.add(id)) {
				continue;
			}
			Template template = templates.apply(id);
			tables.addAll(template.loot());
			List<String> next = new ArrayList<>(template.next());
			Collections.shuffle(next, random);
			queue.addAll(next);
		}
		return tables;
	}

	private static Function<String, Template> reader(Map<String, Template> graph, Map<String, AtomicInteger> reads, String failing) {
		return id -> {
			reads.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();
			if (id.equals(failing)) {
				throw new IllegalStateException("broken template " + id);
			}
			// a little work, so that walks overlap
			long sum = 0;
			for (int i = 0; i < 20_000; i++) {
				sum += i * (long) id.hashCode();
			}
			return sum == 42 ? null : graph.getOrDefault(id, new Template(List.of(), List.of()));
		};
	}

	private static void add(Merged merged, int walk, Set<String> tables) {
		for (String table : tables) {
			List<Integer> walks = merged.byTable().computeIfAbsent(table, t -> new ArrayList<>());
			if (!walks.contains(walk)) {
				walks.add(walk);
			}
		}
	}

	/** The lookup before it was parallel: one walk after another, a plain map as template cache, errors caught per walk. */
	private static Merged sequential(List<Integer> starts, Function<String, Template> read) {
		Merged merged = new Merged(new LinkedHashMap<>(), new LinkedHashMap<>());
		Map<String, Template> cache = new HashMap<>();
		for (int i = 0; i < starts.size(); i++) {
			try {
				add(merged, i, walk(starts.get(i), id -> cache.computeIfAbsent(id, read), () -> false));
			} catch (Throwable e) {
				merged.errors().put(i, e.toString());
			}
		}
		return merged;
	}

	/** The lookup now: the walks on workers, the shared once-cache, merged by walkInOrder. */
	private static Merged parallel(List<Integer> starts, Function<String, Template> read, int threads) {
		Merged merged = new Merged(new LinkedHashMap<>(), new LinkedHashMap<>());
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < starts.size(); i++) {
			indices.add(i);
		}
		try (StructureWalks walks = new StructureWalks(threads, () -> false)) {
			StructureWalks.OnceCache<String, Template> cache = new StructureWalks.OnceCache<>(read);
			boolean complete = walks.<Integer, Set<String>>walkInOrder(indices, i -> () -> walk(starts.get(i), cache::get, walks::aborted), (i, tables, failure) -> {
				if (failure != null) {
					merged.errors().put(i, failure.toString());
				} else {
					add(merged, i, tables);
				}
			});
			assertTrue(complete);
		}
		return merged;
	}

	private static List<Integer> starts(long seed, int count, int templates) {
		Random random = new Random(seed);
		List<Integer> starts = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			starts.add(random.nextInt(templates));
		}
		return starts;
	}

	@Test
	void parallelWalksGiveTheSequentialResultInTheSameOrder() {
		for (long seed = 1; seed <= 5; seed++) {
			Map<String, Template> graph = graph(seed, 1500);
			List<Integer> starts = starts(seed, 300, 1500);
			Map<String, AtomicInteger> sequentialReads = new ConcurrentHashMap<>();
			Map<String, AtomicInteger> parallelReads = new ConcurrentHashMap<>();
			Merged expected = sequential(starts, reader(graph, sequentialReads, null));
			Merged actual = parallel(starts, reader(graph, parallelReads, null), 4);
			assertEquals(expected, actual, "seed " + seed);
			assertEquals(List.copyOf(expected.byTable().keySet()), List.copyOf(actual.byTable().keySet()), "table order, seed " + seed);
			// every template is read once, as with the sequential cache
			assertEquals(sequentialReads.keySet(), parallelReads.keySet());
			assertTrue(parallelReads.values().stream().allMatch(count -> count.get() == 1), "a template was read twice");
		}
	}

	@Test
	void oneThreadGivesTheSameResultToo() {
		Map<String, Template> graph = graph(7, 800);
		List<Integer> starts = starts(7, 150, 800);
		assertEquals(sequential(starts, reader(graph, new ConcurrentHashMap<>(), null)), parallel(starts, reader(graph, new ConcurrentHashMap<>(), null), 1));
	}

	@Test
	void theMergeOrderIsTheItemOrderWhateverOrderTheWalksFinishIn() throws InterruptedException {
		// four walks on four threads that finish last to first, and items without a walk in between
		int walked = 4;
		List<CountDownLatch> done = new ArrayList<>();
		for (int i = 0; i <= walked; i++) {
			done.add(new CountDownLatch(1));
		}
		done.get(walked).countDown();
		List<Integer> finished = Collections.synchronizedList(new ArrayList<>());
		List<String> items = List.of("a", "-", "b", "c", "-", "d");
		List<String> merged = new ArrayList<>();
		try (StructureWalks walks = new StructureWalks(walked, () -> false)) {
			AtomicInteger next = new AtomicInteger();
			boolean complete = walks.<String, String>walkInOrder(items, item -> {
				if (item.equals("-")) {
					return null;
				}
				int index = next.getAndIncrement();
				return () -> {
					try {
						assertTrue(done.get(index + 1).await(5, TimeUnit.SECONDS));
					} catch (InterruptedException e) {
						throw new AssertionError(e);
					}
					finished.add(index);
					done.get(index).countDown();
					return item.toUpperCase();
				};
			}, (item, result, failure) -> {
				assertNull(failure);
				merged.add(item + "=" + result);
			});
			assertTrue(complete);
		}
		assertEquals(List.of(3, 2, 1, 0), finished);
		assertEquals(List.of("a=A", "-=null", "b=B", "c=C", "-=null", "d=D"), merged);
	}

	@Test
	void aWalkThatThrowsIsMergedWithItsExceptionAndTheOthersGoOn() {
		IllegalStateException broken = new IllegalStateException("broken");
		List<String> merged = new ArrayList<>();
		try (StructureWalks walks = new StructureWalks(3, () -> false)) {
			boolean complete = walks.<Integer, Integer>walkInOrder(List.of(0, 1, 2, 3, 4), i -> () -> {
				if (i == 2) {
					throw broken;
				}
				return i * 10;
			}, (i, result, failure) -> {
				if (i == 2) {
					assertSame(broken, failure);
				}
				merged.add(i + ":" + result + ":" + (failure == null ? "" : failure.getMessage()));
			});
			assertTrue(complete);
		}
		assertEquals(List.of("0:0:", "1:10:", "2:null:broken", "3:30:", "4:40:"), merged);
	}

	@Test
	void aBrokenTemplateFailsEveryWalkThatReachesItLikeSequentially() {
		Map<String, Template> graph = graph(3, 600);
		List<Integer> starts = starts(3, 200, 600);
		// a template that many walks reach
		Map<String, AtomicInteger> reads = new ConcurrentHashMap<>();
		sequential(starts, reader(graph, reads, null));
		String failing = reads.entrySet().stream().max((a, b) -> Integer.compare(a.getValue().get(), b.getValue().get())).orElseThrow().getKey();
		Map<String, AtomicInteger> sequentialReads = new ConcurrentHashMap<>();
		Map<String, AtomicInteger> parallelReads = new ConcurrentHashMap<>();
		Merged expected = sequential(starts, reader(graph, sequentialReads, failing));
		Merged actual = parallel(starts, reader(graph, parallelReads, failing), 4);
		assertTrue(expected.errors().size() > 1);
		assertEquals(expected, actual);
		// a failed read is not kept: every walk that reaches the template reads it again, as the plain map did
		assertEquals(expected.errors().size(), sequentialReads.get(failing).get());
		assertEquals(sequentialReads.get(failing).get(), parallelReads.get(failing).get());
	}

	@Test
	void aStaleBuildStopsTheWalksAndEndsItsThreads() throws InterruptedException {
		Map<String, Template> graph = graph(11, 3000);
		List<Integer> starts = starts(11, 2000, 3000);
		AtomicBoolean stale = new AtomicBoolean();
		AtomicInteger reads = new AtomicInteger();
		AtomicBoolean interrupted = new AtomicBoolean();
		Function<String, Template> slow = id -> {
			if (reads.incrementAndGet() == 50) {
				stale.set(true); // a data pack reload while the build runs
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				interrupted.set(true);
			}
			return graph.getOrDefault(id, new Template(List.of(), List.of()));
		};
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < starts.size(); i++) {
			indices.add(i);
		}
		AtomicInteger merged = new AtomicInteger();
		StructureWalks walks = new StructureWalks(4, stale::get);
		boolean complete;
		try (walks) {
			StructureWalks.OnceCache<String, Template> cache = new StructureWalks.OnceCache<>(slow);
			complete = walks.<Integer, Set<String>>walkInOrder(indices, i -> () -> walk(starts.get(i), cache::get, walks::aborted), (i, tables, failure) -> merged.incrementAndGet());
		}
		assertFalse(complete);
		assertTrue(walks.aborted());
		assertTrue(merged.get() < starts.size());
		assertTrue(walks.awaitTermination(5_000), "the workers did not end");
		assertTrue(reads.get() < 200, "the walks went on after the build became stale: " + reads.get() + " reads");
		assertFalse(interrupted.get(), "a worker was interrupted");
	}

	@Test
	void aWalkCutShortByTheAbortIsNotMerged() {
		AtomicBoolean stale = new AtomicBoolean();
		CountDownLatch firstMerged = new CountDownLatch(1);
		List<Integer> merged = new ArrayList<>();
		boolean complete;
		try (StructureWalks walks = new StructureWalks(1, stale::get)) {
			complete = walks.<Integer, Integer>walkInOrder(List.of(0, 1, 2), i -> () -> {
				if (i == 1) {
					try {
						assertTrue(firstMerged.await(5, TimeUnit.SECONDS));
					} catch (InterruptedException e) {
						throw new AssertionError(e);
					}
					// the data changes while this walk runs, it returns what it has so far
					stale.set(true);
				}
				return i;
			}, (i, result, failure) -> {
				merged.add(i);
				firstMerged.countDown();
			});
		}
		assertFalse(complete);
		assertEquals(List.of(0), merged);
	}

	@Test
	void closingCancelsTheQueuedWalksAndEndsTheThreads() throws InterruptedException {
		AtomicInteger started = new AtomicInteger();
		StructureWalks walks = new StructureWalks(2, () -> false);
		List<CompletableFuture<Set<String>>> futures = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			futures.add(walks.submit(() -> {
				// like a walk, which asks before its first read: one that starts while close() cancels does nothing
				if (walks.aborted()) {
					return Set.of();
				}
				started.incrementAndGet();
				while (!walks.aborted()) {
					Thread.onSpinWait();
				}
				return Set.of();
			}));
		}
		// a failure on the owner's thread leaves the try block, which closes the walks
		walks.close();
		assertTrue(walks.awaitTermination(5_000), "the workers did not end");
		assertTrue(started.get() <= 2, "queued walks ran after close: " + started.get());
		assertTrue(futures.stream().allMatch(CompletableFuture::isDone));
		assertTrue(futures.stream().anyMatch(CompletableFuture::isCancelled));
	}

	@Test
	void theWorkersAreNamedDaemons() {
		try (StructureWalks walks = new StructureWalks(2, () -> false)) {
			Thread thread = walks.submit(Thread::currentThread).join();
			assertTrue(thread.getName().startsWith(StructureWalks.THREAD_NAME));
			assertTrue(thread.isDaemon());
		}
	}

	@Test
	void defaultThreadsStayBetweenOneAndFour() {
		int threads = StructureWalks.defaultThreads();
		assertTrue(threads >= 1 && threads <= 4);
	}

	@Test
	void theCacheComputesAKeyOnceWhileOthersWait() throws InterruptedException {
		AtomicInteger computed = new AtomicInteger();
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		StructureWalks.OnceCache<String, String> cache = new StructureWalks.OnceCache<>(key -> {
			if (!key.equals("a")) {
				return key + "?";
			}
			computed.incrementAndGet();
			inside.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			return key + "!";
		});
		List<Thread> threads = new ArrayList<>();
		List<String> results = Collections.synchronizedList(new ArrayList<>());
		for (int i = 0; i < 8; i++) {
			Thread thread = new Thread(() -> results.add(cache.get("a")));
			threads.add(thread);
			thread.start();
		}
		inside.await();
		// another key is not held up by the slow one
		assertEquals("b?", cache.get("b"));
		release.countDown();
		for (Thread thread : threads) {
			thread.join(5_000);
		}
		assertEquals(1, computed.get());
		assertEquals(Collections.nCopies(8, "a!"), results);
		assertEquals(2, cache.size());
	}

	@Test
	void aFailedComputationIsNotKept() {
		AtomicInteger computed = new AtomicInteger();
		StructureWalks.OnceCache<String, String> cache = new StructureWalks.OnceCache<>(key -> {
			if (computed.incrementAndGet() == 1) {
				throw new IllegalArgumentException("bad " + key);
			}
			return key + "!";
		});
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> cache.get("x"));
		assertEquals("bad x", failure.getMessage());
		assertEquals(0, cache.size());
		// asked again, it is computed again, like computeIfAbsent of a plain map that stored nothing
		assertEquals("x!", cache.get("x"));
		assertEquals("x!", cache.get("x"));
		assertEquals(2, computed.get());
	}

	@Test
	void whoWaitedForAFailedComputationComputesItAgain() throws InterruptedException {
		AtomicInteger computed = new AtomicInteger();
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		StructureWalks.OnceCache<String, String> cache = new StructureWalks.OnceCache<>(key -> {
			if (computed.incrementAndGet() == 1) {
				inside.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				throw new IllegalStateException("first read failed");
			}
			return key + "!";
		});
		List<Object> outcomes = Collections.synchronizedList(new ArrayList<>());
		Supplier<Runnable> asker = () -> () -> {
			try {
				outcomes.add(cache.get("a"));
			} catch (IllegalStateException e) {
				outcomes.add(e.getMessage());
			}
		};
		Thread first = new Thread(asker.get());
		first.start();
		inside.await();
		List<Thread> waiters = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Thread waiter = new Thread(asker.get());
			waiters.add(waiter);
			waiter.start();
		}
		// give the waiters time to find the running computation
		Thread.sleep(100);
		release.countDown();
		first.join(5_000);
		for (Thread waiter : waiters) {
			waiter.join(5_000);
		}
		// the first asker gets its failure, the waiters a value read again, by one of them
		assertEquals(1, Collections.frequency(outcomes, "first read failed"));
		assertEquals(4, Collections.frequency(outcomes, "a!"));
		assertEquals(2, computed.get());
	}
}
