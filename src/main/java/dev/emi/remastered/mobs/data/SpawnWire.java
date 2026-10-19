package dev.emi.remastered.mobs.data;

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
 * Binary format of the {@code emi_mobs:spawns} payload data. A batch has a string table (every id and group name once)
 * followed by the rows as table indices, so a few thousand spawn entries stay far below the packet size limit.
 */
public final class SpawnWire {
	/** Rows per payload; keeps every packet well below the 1 MiB limit of custom payloads. */
	public static final int ROWS_PER_BATCH = 600;
	private static final int VERSION = 1;

	private SpawnWire() {
	}

	/** Splits the rows into batches of at most {@code perBatch} rows; an empty list gives one empty batch. */
	public static List<byte[]> encode(List<SpawnRow> rows, int perBatch) {
		List<byte[]> batches = new ArrayList<>();
		if (rows.isEmpty()) {
			batches.add(encodeBatch(List.of()));
			return batches;
		}
		for (int i = 0; i < rows.size(); i += perBatch) {
			batches.add(encodeBatch(rows.subList(i, Math.min(rows.size(), i + perBatch))));
		}
		return batches;
	}

	public static byte[] encodeBatch(List<SpawnRow> rows) {
		Map<String, Integer> strings = new LinkedHashMap<>();
		for (SpawnRow row : rows) {
			strings.putIfAbsent(row.entity(), strings.size());
			strings.putIfAbsent(row.biome(), strings.size());
			strings.putIfAbsent(row.group(), strings.size());
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(VERSION);
			out.writeShort(strings.size());
			for (String s : strings.keySet()) {
				out.writeUTF(s);
			}
			out.writeInt(rows.size());
			for (SpawnRow row : rows) {
				out.writeShort(strings.get(row.entity()));
				out.writeShort(strings.get(row.biome()));
				out.writeShort(strings.get(row.group()));
				out.writeShort(Math.min(row.weight(), 0xFFFF));
				out.writeByte(Math.min(row.minGroup(), 0xFF));
				out.writeByte(Math.min(row.maxGroup(), 0xFF));
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot encode spawn rows", e);
		}
	}

	/** @throws IllegalArgumentException if the data is malformed or of an unknown version */
	public static List<SpawnRow> decode(byte[] data) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			int version = in.readUnsignedByte();
			if (version != VERSION) {
				throw new IllegalArgumentException("Unknown spawn data version " + version);
			}
			String[] strings = new String[in.readUnsignedShort()];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = in.readUTF();
			}
			int count = in.readInt();
			List<SpawnRow> rows = new ArrayList<>(Math.min(count, ROWS_PER_BATCH * 4));
			for (int i = 0; i < count; i++) {
				String entity = strings[in.readUnsignedShort()];
				String biome = strings[in.readUnsignedShort()];
				String group = strings[in.readUnsignedShort()];
				int weight = in.readUnsignedShort();
				int min = in.readUnsignedByte();
				int max = in.readUnsignedByte();
				rows.add(new SpawnRow(entity, biome, group, weight, min, max));
			}
			return rows;
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Malformed spawn data", e);
		}
	}
}
