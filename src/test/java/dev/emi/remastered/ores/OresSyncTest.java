package dev.emi.remastered.ores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.emi.remastered.ores.sync.OresSnapshot;
import dev.emi.remastered.ores.sync.OresSyncPlan;
import dev.emi.remastered.ores.sync.OresSyncState;
import dev.emi.remastered.ores.sync.OresSyncState.Status;
import dev.emi.remastered.ores.sync.SyncBatch;
import dev.emi.remastered.ores.sync.SyncComplete;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class OresSyncTest {
	private static final Identifier TWILIGHT = Identifier.of("twilightforest", "twilight_forest");
	private static final Identifier TWILIGHT_FOREST = Identifier.of("twilightforest", "forest");

	private static Identifier id(String path) {
		return Identifier.of("test", path);
	}

	private static Identifier mc(String path) {
		return Identifier.of("minecraft", path);
	}

	private static Map<Identifier, OresSyncPlan.Feature<String>> features(int count) {
		Map<Identifier, OresSyncPlan.Feature<String>> map = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			map.put(id("ore_" + i), new OresSyncPlan.Feature<>("feature " + i, 100, Set.of(mc("plains"), mc("desert"))));
		}
		return map;
	}

	private static List<OresSyncPlan.DimensionSource> dimensions() {
		return List.of(
			new OresSyncPlan.DimensionSource(mc("overworld"), -64, 320, List.of(mc("plains"), mc("desert"), mc("forest"))),
			new OresSyncPlan.DimensionSource(mc("the_nether"), 0, 128, List.of(mc("nether_wastes"))),
			new OresSyncPlan.DimensionSource(TWILIGHT, 0, 256, List.of(TWILIGHT_FOREST)));
	}

	private static OresSyncPlan<String> plan(int syncId, int count) {
		return OresSyncPlan.build(syncId, features(count), dimensions());
	}

	/** Feeds a whole sync and returns the status of the complete marker. */
	private static Status feed(OresSyncState<String> state, OresSyncPlan<String> plan) {
		state.begin(plan.header());
		for (SyncBatch<String> batch : plan.batches()) {
			state.accept(batch);
		}
		return state.complete(plan.complete());
	}

	@Test
	void planNumbersTheBatchesAndAnnouncesTheirCount() {
		OresSyncPlan<String> plan = plan(1, 250);
		assertEquals(3, plan.batches().size());
		assertEquals(3, plan.header().batchCount());
		assertEquals(250, plan.header().featureCount());
		assertEquals(100, plan.batches().get(0).entries().size());
		assertEquals(100, plan.batches().get(1).entries().size());
		assertEquals(50, plan.batches().get(2).entries().size());
		for (int i = 0; i < 3; i++) {
			assertEquals(i, plan.batches().get(i).index());
			assertEquals(1, plan.batches().get(i).syncId());
		}
		assertEquals(new SyncComplete(1, 3), plan.complete());
	}

	@Test
	void planBatchesBySize() {
		Map<Identifier, OresSyncPlan.Feature<String>> map = new LinkedHashMap<>();
		for (int i = 0; i < 10; i++) {
			map.put(id("big_" + i), new OresSyncPlan.Feature<>("f", 150_000, Set.of()));
		}
		OresSyncPlan<String> plan = OresSyncPlan.build(1, map, List.of());
		// 400 000 bytes fit two 150 000 byte features
		assertEquals(5, plan.batches().size());
		assertEquals(5, plan.header().batchCount());
		plan.batches().forEach(b -> assertEquals(2, b.entries().size()));
	}

	@Test
	void planWithoutFeaturesHasNoBatchesButStillEnds() {
		OresSyncPlan<String> plan = plan(1, 0);
		assertTrue(plan.batches().isEmpty());
		assertEquals(0, plan.header().batchCount());
		OresSyncState<String> state = new OresSyncState<>();
		assertEquals(Status.COMPLETE, feed(state, plan));
		assertEquals(1, state.snapshot().syncId());
		assertTrue(state.snapshot().features().isEmpty());
		assertEquals(3, state.snapshot().dimensions().size());
	}

	@Test
	void assemblesTheSnapshotOnlyWhenTheMarkerArrives() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSyncPlan<String> plan = plan(7, 250);
		state.begin(plan.header());

		assertEquals(Status.PENDING, state.accept(plan.batches().get(0)));
		assertTrue(state.isSyncing());
		assertTrue(state.snapshot().features().isEmpty(), "a partial list must not be visible");
		assertEquals(Status.PENDING, state.accept(plan.batches().get(1)));
		assertEquals(Status.PENDING, state.accept(plan.batches().get(2)));
		assertTrue(state.snapshot().features().isEmpty(), "all batches are in, but the marker is missing");
		assertEquals(Status.COMPLETE, state.complete(plan.complete()));
		assertFalse(state.isSyncing());

		OresSnapshot<String> snapshot = state.snapshot();
		assertEquals(7, snapshot.syncId());
		assertEquals(250, snapshot.features().size());
		assertEquals(250, snapshot.expectedFeatures());
		assertEquals("feature 249", snapshot.features().get(id("ore_249")));
		assertEquals(Set.of(mc("plains"), mc("desert")), snapshot.biomesOf(id("ore_0")));
	}

	@Test
	void batchesInAnyOrderGiveTheSameResult() {
		OresSyncPlan<String> plan = plan(3, 250);
		OresSyncState<String> ordered = new OresSyncState<>();
		feed(ordered, plan);

		OresSyncState<String> shuffled = new OresSyncState<>();
		shuffled.begin(plan.header());
		assertEquals(Status.PENDING, shuffled.accept(plan.batches().get(2)));
		assertEquals(Status.PENDING, shuffled.accept(plan.batches().get(0)));
		assertEquals(Status.PENDING, shuffled.accept(plan.batches().get(1)));
		assertEquals(Status.COMPLETE, shuffled.complete(plan.complete()));

		assertEquals(List.copyOf(ordered.snapshot().features().keySet()), List.copyOf(shuffled.snapshot().features().keySet()));
	}

	@Test
	void aRepeatedBatchIsIgnored() {
		OresSyncPlan<String> plan = plan(1, 250);
		OresSyncState<String> state = new OresSyncState<>();
		state.begin(plan.header());
		assertEquals(Status.PENDING, state.accept(plan.batches().get(0)));
		assertEquals(Status.DUPLICATE, state.accept(plan.batches().get(0)));
		assertEquals(Status.PENDING, state.accept(plan.batches().get(1)));
		assertEquals(Status.DUPLICATE, state.accept(plan.batches().get(1)));
		// the repeats did not count as the missing third batch
		assertEquals(Status.INCOMPLETE, state.complete(plan.complete()));
		assertTrue(state.snapshot().features().isEmpty());
	}

	@Test
	void aMissingBatchKeepsThePreviousData() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 250));
		OresSnapshot<String> first = state.snapshot();

		OresSyncPlan<String> second = plan(2, 250);
		state.begin(second.header());
		state.accept(second.batches().get(0));
		state.accept(second.batches().get(2));
		assertEquals(1, state.missingBatches());
		assertEquals(Status.INCOMPLETE, state.complete(second.complete()));
		assertSame(first, state.snapshot(), "an incomplete sync must not replace the complete one");
		assertFalse(state.isSyncing(), "the broken sync is dropped");
		// its late batch is not used for anything
		assertEquals(Status.IGNORED, state.accept(second.batches().get(1)));
	}

	@Test
	void aMissingBatchBeforeTheFirstCompleteSyncLeavesNothing() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSyncPlan<String> plan = plan(1, 250);
		state.begin(plan.header());
		state.accept(plan.batches().get(0));
		state.accept(plan.batches().get(1));
		assertEquals(Status.INCOMPLETE, state.complete(plan.complete()));
		assertTrue(state.snapshot().features().isEmpty());
		assertEquals(OresSnapshot.EMPTY_ID, state.snapshot().syncId());
	}

	@Test
	void aMarkerWithAnotherBatchCountIsRejected() {
		OresSyncPlan<String> plan = plan(1, 250);
		OresSyncState<String> state = new OresSyncState<>();
		state.begin(plan.header());
		plan.batches().forEach(state::accept);
		assertEquals(Status.INCOMPLETE, state.complete(new SyncComplete(1, 4)));
		assertTrue(state.snapshot().features().isEmpty());
	}

	@Test
	void aBatchOutsideTheAnnouncedRangeIsIgnored() {
		OresSyncPlan<String> plan = plan(1, 150);
		OresSyncState<String> state = new OresSyncState<>();
		state.begin(plan.header());
		assertEquals(Status.IGNORED, state.accept(new SyncBatch<>(1, 2, plan.batches().get(0).entries())));
		assertEquals(Status.IGNORED, state.accept(new SyncBatch<>(1, -1, plan.batches().get(0).entries())));
		plan.batches().forEach(state::accept);
		assertEquals(Status.COMPLETE, state.complete(plan.complete()));
		assertEquals(150, state.snapshot().features().size());
	}

	@Test
	void aMarkerOfAnotherSyncDoesNotTouchThePendingOne() {
		OresSyncPlan<String> plan = plan(5, 250);
		OresSyncState<String> state = new OresSyncState<>();
		state.begin(plan.header());
		plan.batches().forEach(state::accept);
		assertEquals(Status.IGNORED, state.complete(new SyncComplete(4, 3)));
		assertTrue(state.isSyncing());
		assertEquals(Status.COMPLETE, state.complete(plan.complete()));
	}

	@Test
	void ignoresPacketsWithoutStart() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSyncPlan<String> plan = plan(1, 5);
		assertEquals(Status.IGNORED, state.accept(plan.batches().get(0)));
		assertEquals(Status.IGNORED, state.complete(plan.complete()));
		assertTrue(state.snapshot().features().isEmpty());
	}

	@Test
	void aNewStartWhilePendingDropsTheUnfinishedSync() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSyncPlan<String> abandoned = plan(1, 250);
		state.begin(abandoned.header());
		state.accept(abandoned.batches().get(0));

		OresSyncPlan<String> next = plan(2, 5);
		state.begin(next.header());
		// late packets of the abandoned sync are ignored, in particular its marker
		assertEquals(Status.IGNORED, state.accept(abandoned.batches().get(1)));
		assertEquals(Status.IGNORED, state.complete(abandoned.complete()));
		assertTrue(state.isSyncing());
		assertEquals(Status.PENDING, state.accept(next.batches().get(0)));
		assertEquals(Status.COMPLETE, state.complete(next.complete()));
		assertEquals(2, state.snapshot().syncId());
		assertEquals(5, state.snapshot().features().size());
	}

	@Test
	void resolvesDimensionsIncludingModdedOnes() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 3));
		OresSnapshot<String> snapshot = state.snapshot();
		assertEquals(3, snapshot.dimensions().size());
		assertEquals(-64, snapshot.dimension(mc("overworld")).bottomY());
		assertEquals(320, snapshot.dimension(mc("overworld")).topY());
		assertEquals(3, snapshot.dimension(mc("overworld")).biomes().size());
		assertEquals(List.of(mc("overworld")), snapshot.dimensionsOf(id("ore_0")).stream().map(OresSnapshot.Dimension::id).toList());
		assertEquals(List.of(snapshot.dimension(mc("overworld"))), snapshot.dimensionsOfBiome(mc("forest")));
		assertTrue(snapshot.dimensionsOfBiome(mc("the_end")).isEmpty());
		assertEquals(List.of(TWILIGHT), snapshot.dimensionsOfBiome(TWILIGHT_FOREST).stream().map(OresSnapshot.Dimension::id).toList());
	}

	@Test
	void featureInSeveralDimensionsIsListedInOrder() {
		Map<Identifier, OresSyncPlan.Feature<String>> map = new LinkedHashMap<>();
		map.put(id("everywhere"), new OresSyncPlan.Feature<>("f", 10, Set.of(mc("forest"), mc("nether_wastes"), TWILIGHT_FOREST)));
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, OresSyncPlan.build(1, map, dimensions()));
		assertEquals(List.of(mc("overworld"), mc("the_nether"), TWILIGHT),
			state.snapshot().dimensionsOf(id("everywhere")).stream().map(OresSnapshot.Dimension::id).toList());
	}

	@Test
	void keepsThePreviousSnapshotWhileTheNextSyncIsIncomplete() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 250));
		OresSnapshot<String> first = state.snapshot();

		OresSyncPlan<String> second = plan(2, 120);
		state.begin(second.header());
		state.accept(second.batches().get(0));
		assertSame(first, state.snapshot(), "the complete snapshot stays until the new one is complete");
		assertEquals(250, state.snapshot().features().size());

		state.accept(second.batches().get(1));
		assertSame(first, state.snapshot(), "also after the last batch, until the marker");
		assertEquals(Status.COMPLETE, state.complete(second.complete()));
		assertEquals(2, state.snapshot().syncId());
		assertEquals(120, state.snapshot().features().size());
	}

	@Test
	void skipsFeaturesThatCouldNotBeDecoded() {
		OresSyncPlan<String> plan = plan(1, 3);
		SyncBatch<String> batch = plan.batches().get(0);
		SyncBatch.Entry<String> first = batch.entries().get(0);
		SyncBatch<String> damaged = new SyncBatch<>(1, 0,
			List.of(new SyncBatch.Entry<>(first.id(), null, first.biomeIndices()), batch.entries().get(1), batch.entries().get(2)));
		OresSyncState<String> state = new OresSyncState<>();
		state.begin(plan.header());
		state.accept(damaged);
		assertEquals(Status.COMPLETE, state.complete(plan.complete()));
		assertEquals(2, state.snapshot().features().size());
		assertEquals(3, state.snapshot().expectedFeatures(), "the client can tell that features are missing");
	}

	@Test
	void noReloadWhenEmiHasNotLoadedYet() {
		OresSyncState<String> state = new OresSyncState<>();
		assertEquals(Status.COMPLETE, feed(state, plan(1, 10)));
	}

	@Test
	void reloadsWhenEmiLoadedBeforeAnyDataArrived() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSnapshot<String> seen = state.consume();
		assertEquals(OresSnapshot.EMPTY_ID, seen.syncId());
		assertTrue(seen.features().isEmpty());
		assertEquals(Status.COMPLETE_RELOAD, feed(state, plan(1, 10)));
		// the reload then reads the new data and the next sync does not ask for one again
		assertEquals(1, state.consume().syncId());
	}

	@Test
	void reloadDuringTheSyncSeesOnlyCompleteDataAndIsRepeatedAfterwards() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 250));
		state.consume();

		// the server resyncs (for example /reload), EMI reloads while batches are still arriving
		OresSyncPlan<String> second = plan(2, 250);
		state.begin(second.header());
		state.accept(second.batches().get(0));
		OresSnapshot<String> duringSync = state.consume();
		assertEquals(1, duringSync.syncId());
		assertEquals(250, duringSync.features().size());

		state.accept(second.batches().get(1));
		state.accept(second.batches().get(2));
		// EMI used sync 1, so completing sync 2 asks for a reload
		assertEquals(Status.COMPLETE_RELOAD, state.complete(second.complete()));
		assertEquals(2, state.consume().syncId());
	}

	@Test
	void reloadDuringTheFirstSyncSeesNothingAndIsRepeatedAfterwards() {
		OresSyncState<String> state = new OresSyncState<>();
		OresSyncPlan<String> plan = plan(1, 250);
		state.begin(plan.header());
		state.accept(plan.batches().get(0));
		assertTrue(state.consume().features().isEmpty());
		state.accept(plan.batches().get(1));
		state.accept(plan.batches().get(2));
		assertEquals(Status.COMPLETE_RELOAD, state.complete(plan.complete()));
	}

	@Test
	void noReloadWhenTheReloadAlreadyUsedTheSameSync() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 10));
		state.consume();
		assertEquals(Status.COMPLETE, feed(state, plan(1, 10)));
	}

	@Test
	void anIncompleteSyncNeverAsksForAReload() {
		OresSyncState<String> state = new OresSyncState<>();
		state.consume();
		OresSyncPlan<String> plan = plan(1, 250);
		state.begin(plan.header());
		state.accept(plan.batches().get(0));
		assertEquals(Status.INCOMPLETE, state.complete(plan.complete()));
	}

	@Test
	void clearOnDisconnectForgetsEverythingAlsoAPendingSync() {
		OresSyncState<String> state = new OresSyncState<>();
		feed(state, plan(1, 10));
		state.consume();
		OresSyncPlan<String> pending = plan(2, 250);
		state.begin(pending.header());
		state.accept(pending.batches().get(0));

		state.clear();
		assertTrue(state.snapshot().features().isEmpty());
		assertTrue(state.snapshot().dimensions().isEmpty());
		assertFalse(state.isSyncing());
		// packets that were still on their way after the disconnect are ignored
		assertEquals(Status.IGNORED, state.accept(pending.batches().get(1)));
		assertEquals(Status.IGNORED, state.complete(pending.complete()));
		// after a reconnect EMI has not loaded yet, so no reload is requested
		assertEquals(Status.COMPLETE, feed(state, plan(1, 10)));
	}
}
