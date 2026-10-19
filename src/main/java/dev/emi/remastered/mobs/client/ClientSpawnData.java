package dev.emi.remastered.mobs.client;

import java.util.List;

import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.data.SpawnRow;
import dev.emi.remastered.mobs.data.SpawnTable;

/**
 * The spawn entries the server sent. Mob pages read it when they are opened, so data that arrives after the EMI
 * reload is picked up without another reload.
 */
public final class ClientSpawnData {
	private static final Object LOCK = new Object();
	private static final SpawnTable TABLE = new SpawnTable();
	private static boolean complete;

	private ClientSpawnData() {
	}

	public static void accept(boolean first, boolean last, List<SpawnRow> rows) {
		synchronized (LOCK) {
			if (first) {
				TABLE.clear();
				complete = false;
			}
			TABLE.addAll(rows);
			if (last) {
				complete = true;
				Mobs.LOGGER.info("Received {} spawn entries for {} mobs in {} biomes", TABLE.rowCount(), TABLE.entityCount(), TABLE.biomeCount());
			}
		}
	}

	public static void clear() {
		synchronized (LOCK) {
			TABLE.clear();
			complete = false;
		}
	}

	/** Whether a complete sync of the server arrived. False for servers without the module. */
	public static boolean isComplete() {
		synchronized (LOCK) {
			return complete;
		}
	}

	/** Spawn entries of one entity type id, heaviest first. */
	public static List<SpawnRow> rowsOf(String entityId) {
		synchronized (LOCK) {
			return TABLE.rowsOf(entityId);
		}
	}
}
