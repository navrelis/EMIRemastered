package dev.emi.remastered.world.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary format of the {@code emi_world:structure_loot} payload: the chest loot tables of every structure, sent after
 * the world data when the loot module's structure lookup finished later than the world data. A batch is a string
 * table followed by (structure, tables) entries as table indices; big maps are split into batches by an estimated size.
 */
public final class LootLinksWire {
	private static final int VERSION = 1;
	private static final int MAX_LIST = 1_000_000;

	private LootLinksWire() {
	}

	/** Splits the map (structure id to chest loot tables) into batches of at most {@code budget} estimated bytes. */
	public static List<byte[]> encode(Map<String, List<String>> tablesByStructure, int budget) {
		List<byte[]> batches = new ArrayList<>();
		Map<String, List<String>> batch = new LinkedHashMap<>();
		long size = 0;
		for (Map.Entry<String, List<String>> entry : tablesByStructure.entrySet()) {
			long itemSize = 8 + entry.getKey().length();
			for (String table : entry.getValue()) {
				itemSize += table.length() + 4;
			}
			if (size > 0 && size + itemSize > budget) {
				batches.add(encodeBatch(batch));
				batch = new LinkedHashMap<>();
				size = 0;
			}
			batch.put(entry.getKey(), entry.getValue());
			size += itemSize;
		}
		if (!batch.isEmpty() || batches.isEmpty()) {
			batches.add(encodeBatch(batch));
		}
		return batches;
	}

	private static byte[] encodeBatch(Map<String, List<String>> batch) {
		Map<String, Integer> table = new LinkedHashMap<>();
		batch.forEach((structure, tables) -> {
			table.putIfAbsent(structure, table.size());
			tables.forEach(t -> table.putIfAbsent(t, table.size()));
		});
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(VERSION);
			WorldWire.writeVarInt(out, table.size());
			for (String string : table.keySet()) {
				out.writeUTF(string);
			}
			WorldWire.writeVarInt(out, batch.size());
			for (Map.Entry<String, List<String>> entry : batch.entrySet()) {
				WorldWire.writeVarInt(out, table.get(entry.getKey()));
				WorldWire.writeVarInt(out, entry.getValue().size());
				for (String t : entry.getValue()) {
					WorldWire.writeVarInt(out, table.get(t));
				}
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot encode the loot links", e);
		}
	}

	/** @throws IllegalArgumentException if the data is malformed or of an unknown version */
	public static Map<String, List<String>> decode(byte[] data) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			int version = in.readUnsignedByte();
			if (version != VERSION) {
				throw new IllegalArgumentException("Unknown loot links version " + version);
			}
			String[] strings = new String[count(in)];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = in.readUTF();
			}
			Map<String, List<String>> result = new LinkedHashMap<>();
			int entries = count(in);
			for (int i = 0; i < entries; i++) {
				String structure = strings[WorldWire.readVarInt(in)];
				int n = count(in);
				List<String> tables = new ArrayList<>(Math.min(n, 1024));
				for (int j = 0; j < n; j++) {
					tables.add(strings[WorldWire.readVarInt(in)]);
				}
				result.put(structure, List.copyOf(tables));
			}
			return result;
		} catch (IOException | ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
			throw new IllegalArgumentException("Malformed loot links", e);
		}
	}

	private static int count(DataInputStream in) throws IOException {
		int count = WorldWire.readVarInt(in);
		if (count < 0 || count > MAX_LIST) {
			throw new IllegalArgumentException("Bad count " + count);
		}
		return count;
	}
}
