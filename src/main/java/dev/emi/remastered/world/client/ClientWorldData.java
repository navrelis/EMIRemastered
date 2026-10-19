package dev.emi.remastered.world.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	/** Chest loot tables by structure: from the last complete sync, replaced by a later {@link #acceptLoot}. */
	private static Map<String, List<String>> lootTables = new HashMap<>();
	private static int lootVersion;

	private ClientWorldData() {
	}

	/** Changes whenever the chest loot tables of the structures change, for the loot links of the structure pages. */
	public static int lootVersion() {
		synchronized (LOCK) {
			return lootVersion;
		}
	}

	/** The chest loot tables of a structure: from the last complete sync, or from the follow-up that came after it. */
	public static List<String> lootTablesOf(String structure) {
		synchronized (LOCK) {
			return lootTables.getOrDefault(structure, List.of());
		}
	}

	/**
	 * The chest loot tables the server looked up after it sent the world data. The structure pages read them when a
	 * page is built, and the snapshot's fingerprint leaves them out, so they never cause an EMI reload.
	 */
	public static void acceptLoot(boolean first, Map<String, List<String>> tables) {
		synchronized (LOCK) {
			if (first) {
				lootTables = new HashMap<>();
			}
			lootTables.putAll(tables);
			lootVersion++;
		}
		World.LOGGER.info("Received the chest loot tables of {} structures from the server", tables.size());
	}

	private static Map<String, List<String>> lootTablesOf(WorldSnapshot snapshot) {
		Map<String, List<String>> tables = new HashMap<>();
		for (StructureRow row : snapshot.structures()) {
			if (!row.lootTables().isEmpty()) {
				tables.put(row.id(), row.lootTables());
			}
		}
		return tables;
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
				lootTables = lootTablesOf(snapshot);
				lootVersion++;
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
			lootTables = new HashMap<>();
			lootVersion++;
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
