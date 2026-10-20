package dev.emi.remastered.info.search;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.remastered.info.InfoModule;
import net.minecraft.client.MinecraftClient;

/**
 * Source labels of every baked stack, for the {@code ~} search query. Built once per EMI reload at search bake time
 * ({@link #bake}) and published as an immutable snapshot through a volatile field, so the search thread only reads.
 * Labels that arrive later (the structures of chest loot) are added by {@link #refreshAsync}, which rebuilds the
 * snapshot on a background thread. With the {@code info} module off the snapshot is inactive and {@code ~} queries match
 * nothing. Client only.
 */
public final class SourceIndex {
	private static final Snapshot INACTIVE = new Snapshot(false, Map.of(), Set.of(), Set.of());
	private static volatile Snapshot current = INACTIVE;
	private static final AtomicLong generation = new AtomicLong();

	private SourceIndex() {
	}

	/**
	 * Immutable once published.
	 *
	 * @param labels   joined lower case labels per stack (identity of the baked stack); stacks without labels are absent
	 * @param noSource baked item and fluid stacks that nothing produces
	 * @param stacks   the baked stacks the snapshot was built for, to rebuild it
	 */
	public record Snapshot(boolean active, Map<EmiStack, String> labels, Set<EmiStack> noSource, Set<EmiStack> stacks) {

		public String labelsOf(EmiStack stack) {
			String joined = labels.get(stack);
			return joined == null ? "" : joined;
		}

		public boolean isNoSource(EmiStack stack) {
			return noSource.contains(stack);
		}
	}

	public static Snapshot current() {
		return current;
	}

	/** Matches a stack that is not in the snapshot: its labels are computed now (one lookup in EMI's recipe index). */
	static boolean matchesUnbaked(EmiStack stack, String needle, boolean noSourceKeyword) {
		if (!current.active()) {
			return false;
		}
		SourceIndexBuilder builder = new SourceIndexBuilder();
		String labels = builder.compute(stack);
		return SourceMatch.matches(labels, builder.noSource(), needle, noSourceKeyword);
	}

	/** Called by {@code EmiSearch.bake} on the reload thread with the stacks it baked. Never throws. */
	public static void bake(Set<EmiStack> bakedStacks) {
		long id = generation.incrementAndGet();
		if (!InfoModule.enabled) {
			current = INACTIVE;
			return;
		}
		try {
			publish(id, build(bakedStacks));
		} catch (Throwable e) {
			EmiLog.error("EMI could not build the source index for the search", e);
			current = INACTIVE;
		}
	}

	/**
	 * Rebuilds the snapshot for the stacks of the current one on a background thread, then repeats a search that uses
	 * {@code ~}. For labels that arrive after the reload (the structures of chests). Client thread.
	 */
	public static void refreshAsync() {
		Snapshot base = current;
		if (!base.active()) {
			return;
		}
		long id = generation.incrementAndGet();
		Thread thread = new Thread(() -> {
			try {
				if (publish(id, build(base.stacks()))) {
					MinecraftClient.getInstance().execute(SourceIndex::searchAgain);
				}
			} catch (Throwable e) {
				EmiLog.error("EMI could not refresh the source index for the search", e);
			}
		}, "EMI Source Index");
		thread.setDaemon(true);
		thread.start();
	}

	private static void searchAgain() {
		if (EmiScreenManager.search != null && EmiScreenManager.search.getText().indexOf('~') >= 0) {
			EmiScreenManager.search.update();
		}
	}

	/** Publishes unless a newer build was started meanwhile. */
	private static synchronized boolean publish(long id, Snapshot snapshot) {
		if (generation.get() != id) {
			return false;
		}
		current = snapshot;
		return true;
	}

	private static Snapshot build(Set<EmiStack> stacks) {
		long start = System.nanoTime();
		SourceIndexBuilder builder = new SourceIndexBuilder();
		Map<EmiStack, String> labels = new IdentityHashMap<>(stacks.size() * 2);
		Set<EmiStack> noSource = Collections.newSetFromMap(new IdentityHashMap<>());
		for (EmiStack stack : stacks) {
			String joined;
			try {
				joined = builder.compute(stack);
			} catch (RuntimeException e) {
				EmiLog.error("EMI caught an exception while indexing the source of " + stack, e);
				continue;
			}
			if (!joined.isEmpty()) {
				labels.put(stack, joined);
			}
			if (builder.noSource()) {
				noSource.add(stack);
			}
		}
		Snapshot snapshot = new Snapshot(true, labels, noSource, stacks);
		EmiLog.info("Source search index: " + stacks.size() + " stacks, " + labels.size() + " with sources, "
			+ builder.labelCount() + " labels, " + noSource.size() + " without source, "
			+ (System.nanoTime() - start) / 1_000_000 + "ms");
		return snapshot;
	}
}
