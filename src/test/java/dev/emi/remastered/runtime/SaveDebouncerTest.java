package dev.emi.remastered.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SaveDebouncerTest {

	/** Timer that only records; the test fires the latest task by hand. */
	private static final class ManualTimer implements SaveDebouncer.Timer {
		final List<Runnable> tasks = new ArrayList<>();
		final List<CompletableFuture<Void>> futures = new ArrayList<>();
		long lastDelay;

		@Override
		public Future<?> schedule(Runnable task, long delayMs) {
			lastDelay = delayMs;
			tasks.add(task);
			CompletableFuture<Void> future = new CompletableFuture<>();
			futures.add(future);
			return future;
		}

		int live() {
			return (int) futures.stream().filter(f -> !f.isCancelled()).count();
		}

		void fireLive() {
			for (int i = 0; i < tasks.size(); i++) {
				if (!futures.get(i).isCancelled()) {
					tasks.get(i).run();
				}
			}
		}
	}

	@Test
	void burstOfRequestsSavesOnce() {
		AtomicInteger saves = new AtomicInteger();
		ManualTimer timer = new ManualTimer();
		SaveDebouncer debouncer = new SaveDebouncer(500, saves::incrementAndGet, Runnable::run, timer);
		for (int i = 0; i < 50; i++) {
			debouncer.request();
		}
		assertEquals(500, timer.lastDelay);
		assertEquals(0, saves.get());
		assertEquals(1, timer.live(), "every new request cancels the previous timer");
		assertTrue(debouncer.isDirty());
		timer.fireLive();
		assertEquals(1, saves.get());
		assertFalse(debouncer.isDirty());
	}

	@Test
	void flushSavesNowAndCancelsTheTimer() {
		AtomicInteger saves = new AtomicInteger();
		ManualTimer timer = new ManualTimer();
		SaveDebouncer debouncer = new SaveDebouncer(500, saves::incrementAndGet, Runnable::run, timer);
		debouncer.request();
		debouncer.request();
		debouncer.flush();
		assertEquals(1, saves.get());
		assertEquals(0, timer.live());
		timer.fireLive();
		assertEquals(1, saves.get());
	}

	@Test
	void flushWithoutChangesDoesNothing() {
		AtomicInteger saves = new AtomicInteger();
		SaveDebouncer debouncer = new SaveDebouncer(500, saves::incrementAndGet, Runnable::run, new ManualTimer());
		debouncer.flush();
		assertEquals(0, saves.get());
	}

	@Test
	void timerThatFiresAfterAFlushDoesNotSaveTwice() {
		AtomicInteger saves = new AtomicInteger();
		ManualTimer timer = new ManualTimer();
		SaveDebouncer debouncer = new SaveDebouncer(500, saves::incrementAndGet, Runnable::run, timer);
		debouncer.request();
		Runnable lateTask = timer.tasks.get(0);
		debouncer.flush();
		lateTask.run();
		assertEquals(1, saves.get());
	}

	@Test
	void timerOnlyDispatchesTheSave() {
		AtomicInteger saves = new AtomicInteger();
		List<Runnable> clientQueue = new ArrayList<>();
		ManualTimer timer = new ManualTimer();
		SaveDebouncer debouncer = new SaveDebouncer(500, saves::incrementAndGet, clientQueue::add, timer);
		debouncer.request();
		timer.fireLive();
		assertEquals(0, saves.get(), "the save runs on the dispatcher, not on the timer thread");
		assertEquals(1, clientQueue.size());
		debouncer.flush();
		assertEquals(1, saves.get(), "a dropped dispatcher task is covered by flush");
		clientQueue.get(0).run();
		assertEquals(1, saves.get());
	}

	@Test
	void changeDuringSaveSchedulesAnotherSave() {
		AtomicInteger saves = new AtomicInteger();
		ManualTimer timer = new ManualTimer();
		SaveDebouncer[] ref = new SaveDebouncer[1];
		ref[0] = new SaveDebouncer(500, () -> {
			if (saves.incrementAndGet() == 1) {
				ref[0].request();
			}
		}, Runnable::run, timer);
		ref[0].request();
		ref[0].flush();
		assertEquals(1, saves.get());
		assertTrue(ref[0].isDirty());
		ref[0].flush();
		assertEquals(2, saves.get());
	}

	@Test
	void daemonTimerRunsOnADaemonThread() throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		boolean[] daemon = new boolean[1];
		SaveDebouncer debouncer = new SaveDebouncer(10, () -> {
			daemon[0] = Thread.currentThread().isDaemon();
			done.countDown();
		}, Runnable::run, SaveDebouncer.daemonTimer());
		debouncer.request();
		assertTrue(done.await(5, TimeUnit.SECONDS));
		assertTrue(daemon[0]);
	}
}
