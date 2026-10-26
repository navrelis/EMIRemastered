package dev.emi.remastered.loot.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * The parallel structure walks of the structure lookup must give exactly the result of walking the structures one after
 * another. A fake template graph stands in for the structure templates: every template names loot tables and the
 * templates it leads to, and a walk is a breadth-first search with its own seeded random and a template limit, like
 * the lookup's walk through pools.
 */
class StructureWalksTest {
	private static final int TEMPLATE_LIMIT = 60;

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
		while (!queue.isEmpty() && visited.size() < TEMPLATE_LIMIT) {
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

	private static Merged parallel(List<Integer> starts, Function<String, Template> read, int threads) {
		Merged merged = new Merged(new LinkedHashMap<>(), new LinkedHashMap<>());
		try (StructureWalks walks = new StructureWalks(threads, () -> false)) {
			StructureWalks.OnceCache<String, Template> cache = new StructureWalks.OnceCache<>(read);
			List<CompletableFuture<Set<String>>> walked = new ArrayList<>();
			for (int start : starts) {
				walked.add(walks.submit(() -> walk(start, cache::get, walks::aborted)));
			}
			for (int i = 0; i < starts.size(); i++) {
				try {
					add(merged, i, walked.get(i).join());
				} catch (CompletionException e) {
					merged.errors().put(i, e.getCause().toString());
				}
			}
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
	void aBrokenTemplateFailsEveryWalkThatReachesItLikeSequentially() {
		Map<String, Template> graph = graph(3, 600);
		List<Integer> starts = starts(3, 200, 600);
		// a template that many walks reach
		Map<String, AtomicInteger> reads = new ConcurrentHashMap<>();
		sequential(starts, reader(graph, reads, null));
		String failing = reads.entrySet().stream().max((a, b) -> Integer.compare(a.getValue().get(), b.getValue().get())).orElseThrow().getKey();
		Merged expected = sequential(starts, reader(graph, new ConcurrentHashMap<>(), failing));
		Merged actual = parallel(starts, reader(graph, new ConcurrentHashMap<>(), failing), 4);
		assertFalse(expected.errors().isEmpty());
		assertEquals(expected, actual);
	}

	@Test
	void aStaleBuildStopsTheWalksAndEndsItsThreads() throws InterruptedException {
		Map<String, Template> graph = graph(11, 3000);
		List<Integer> starts = starts(11, 2000, 3000);
		AtomicBoolean stale = new AtomicBoolean();
		AtomicInteger reads = new AtomicInteger();
		Function<String, Template> slow = id -> {
			if (reads.incrementAndGet() == 50) {
				stale.set(true); // a data pack reload while the build runs
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				throw new AssertionError("a worker was interrupted");
			}
			return graph.getOrDefault(id, new Template(List.of(), List.of()));
		};
		StructureWalks walks = new StructureWalks(4, stale::get);
		StructureWalks.OnceCache<String, Template> cache = new StructureWalks.OnceCache<>(slow);
		List<CompletableFuture<Set<String>>> walked = new ArrayList<>();
		for (int start : starts) {
			walked.add(walks.submit(() -> walk(start, cache::get, walks::aborted)));
		}
		// the owner of the build sees the stale data and leaves
		while (!stale.get()) {
			Thread.onSpinWait();
		}
		assertTrue(walks.aborted());
		walks.close();
		assertTrue(walks.awaitTermination(5_000), "the workers did not end");
		assertTrue(reads.get() < 200, "the walks went on after the build became stale: " + reads.get() + " reads");
		assertTrue(walked.stream().anyMatch(CompletableFuture::isCancelled), "queued walks were not cancelled");
	}

	@Test
	void closingAfterAFailureCancelsTheRestWithoutStaleData() throws InterruptedException {
		AtomicInteger started = new AtomicInteger();
		StructureWalks walks = new StructureWalks(2, () -> false);
		for (int i = 0; i < 1000; i++) {
			walks.submit(() -> {
				if (walks.aborted()) {
					return Set.of();
				}
				started.incrementAndGet();
				while (!walks.aborted()) {
					Thread.onSpinWait();
				}
				return Set.of();
			});
		}
		walks.close();
		assertTrue(walks.awaitTermination(5_000), "the workers did not end");
		assertTrue(started.get() <= 2, "queued walks ran after close: " + started.get());
	}

	@Test
	void theWorkersAreNamedDaemonsOfNormalPriority() {
		try (StructureWalks walks = new StructureWalks(2, () -> false)) {
			Thread thread = walks.submit(Thread::currentThread).join();
			assertTrue(thread.getName().startsWith(StructureWalks.THREAD_NAME));
			assertTrue(thread.isDaemon());
			assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
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
	void aFailedComputationFailsForEveryoneWithTheSameError() {
		AtomicInteger computed = new AtomicInteger();
		StructureWalks.OnceCache<String, String> cache = new StructureWalks.OnceCache<>(key -> {
			computed.incrementAndGet();
			throw new IllegalArgumentException("bad " + key);
		});
		IllegalArgumentException first = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> cache.get("x"));
		IllegalArgumentException second = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> cache.get("x"));
		assertEquals(first.toString(), second.toString());
		assertEquals(1, computed.get());
	}
}
