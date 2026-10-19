package dev.emi.remastered.runtime;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coalesces bursts of save requests: {@link #request()} marks the data dirty and (re)starts a timer; when the timer
 * fires, the action is handed to the dispatcher and runs once. {@link #flush()} runs a pending save right away on the
 * calling thread (disconnect, client stop, before reading the file again).
 */
public final class SaveDebouncer {
	private static ScheduledThreadPoolExecutor scheduler;

	private final long delayMs;
	private final Runnable action;
	private final Executor dispatcher;
	private final Timer timer;
	private final AtomicBoolean dirty = new AtomicBoolean();
	private Future<?> pending;

	/**
	 * @param dispatcher where the action runs once the delay is over (EMI uses the client thread)
	 * @param timer schedules the delayed hand-off; {@link #daemonTimer()} in production
	 */
	public SaveDebouncer(long delayMs, Runnable action, Executor dispatcher, Timer timer) {
		this.delayMs = delayMs;
		this.action = action;
		this.dispatcher = dispatcher;
		this.timer = timer;
	}

	public synchronized void request() {
		dirty.set(true);
		if (pending != null) {
			pending.cancel(false);
		}
		pending = timer.schedule(() -> dispatcher.execute(this::runIfDirty), delayMs);
	}

	public void flush() {
		synchronized (this) {
			if (pending != null) {
				pending.cancel(false);
				pending = null;
			}
		}
		runIfDirty();
	}

	public boolean isDirty() {
		return dirty.get();
	}

	private void runIfDirty() {
		if (dirty.compareAndSet(true, false)) {
			action.run();
		}
	}

	/** One shared daemon thread for all debounced saves; never keeps the game process alive. */
	public static synchronized Timer daemonTimer() {
		if (scheduler == null) {
			scheduler = new ScheduledThreadPoolExecutor(1, r -> {
				Thread thread = new Thread(r, "EMI Remastered Save Scheduler");
				thread.setDaemon(true);
				return thread;
			});
			scheduler.setRemoveOnCancelPolicy(true);
		}
		ScheduledThreadPoolExecutor exec = scheduler;
		return (task, delay) -> exec.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

	@FunctionalInterface
	public interface Timer {
		Future<?> schedule(Runnable task, long delayMs);
	}
}
