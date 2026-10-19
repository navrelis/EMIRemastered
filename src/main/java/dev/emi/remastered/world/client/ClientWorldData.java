package dev.emi.remastered.world.client;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.data.DimensionRow;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.WorldBatch;
import dev.emi.remastered.world.data.WorldSnapshot;
import net.minecraft.client.MinecraftClient;

/**
 * The world data the server sent. The biome pages read it when they are opened; the structure pages are created from it
 * during EMI's reload, so when a complete sync arrives that differs from what EMI registered (or EMI reloaded before
 * any data came) a new EMI reload is started.
 */
public final class ClientWorldData {
	private static final Object LOCK = new Object();
	private static final int NOTHING = 0;
	private static WorldSnapshot snapshot = WorldSnapshot.EMPTY;
	private static List<DimensionRow> pendingDimensions = new ArrayList<>();
	private static List<StructureRow> pendingStructures = new ArrayList<>();
	private static boolean assembling;
	/** Whether EMI created structure pages from the data of {@link #usedFingerprint} in this connection. */
	private static boolean used;
	private static int usedFingerprint = NOTHING;
	private static int version;

	private ClientWorldData() {
	}

	/** Changes whenever the data changes or is cleared, for caches that are built from it. */
	public static int version() {
		synchronized (LOCK) {
			return version;
		}
	}

	public static void accept(boolean first, boolean last, WorldBatch batch) {
		boolean reload = false;
		synchronized (LOCK) {
			if (first) {
				pendingDimensions = new ArrayList<>();
				pendingStructures = new ArrayList<>();
				assembling = true;
			}
			if (!assembling) {
				World.LOGGER.warn("Ignoring a world data batch without a start");
				return;
			}
			pendingDimensions.addAll(batch.dimensions());
			pendingStructures.addAll(batch.structures());
			if (last) {
				snapshot = new WorldSnapshot(true, pendingDimensions, pendingStructures);
				pendingDimensions = new ArrayList<>();
				pendingStructures = new ArrayList<>();
				assembling = false;
				version++;
				World.LOGGER.info("Received {} structures and {} dimensions from the server", snapshot.structures().size(), snapshot.dimensionCount());
				reload = used && usedFingerprint != snapshot.fingerprint();
			}
		}
		if (reload) {
			World.LOGGER.info("EMI registered older world data, reloading EMI");
			MinecraftClient.getInstance().execute(EmiReloadManager::reload);
		}
	}

	public static void clear() {
		synchronized (LOCK) {
			snapshot = WorldSnapshot.EMPTY;
			pendingDimensions = new ArrayList<>();
			pendingStructures = new ArrayList<>();
			assembling = false;
			version++;
			used = false;
			usedFingerprint = NOTHING;
		}
	}

	/** The last complete data, empty (and not complete) before the first sync. */
	public static WorldSnapshot snapshot() {
		synchronized (LOCK) {
			return snapshot;
		}
	}

	/** For EMI's registration: the data to build the structure pages from. Remembers what EMI used. */
	public static WorldSnapshot consume() {
		synchronized (LOCK) {
			used = true;
			usedFingerprint = snapshot.isComplete() ? snapshot.fingerprint() : NOTHING;
			return snapshot;
		}
	}
}
