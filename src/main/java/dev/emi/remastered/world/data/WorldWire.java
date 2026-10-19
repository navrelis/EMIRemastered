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

import dev.emi.remastered.world.data.StructureRow.Kind;
import dev.emi.remastered.world.data.StructureRow.Placement;

/**
 * Binary format of the {@code emi_world:structures} payload data. A batch has a string table (every id once) followed
 * by dimensions and structures as table indices (variable length integers), so big packs stay far below the packet
 * size limit. A sync is split into batches by an estimated size; the dimensions come first, then the structures.
 */
public final class WorldWire {
	/** Estimated bytes per batch; custom payloads may carry 1 MiB, the estimate counts every string in full. */
	public static final int MAX_BATCH_BYTES = 200_000;
	private static final int VERSION = 1;
	private static final int MAX_LIST = 1_000_000;

	private WorldWire() {
	}

	/** Splits the data into batches of at most {@code budget} estimated bytes (an item bigger than that gets its own). */
	public static List<byte[]> encode(List<DimensionRow> dimensions, List<StructureRow> structures, int budget) {
		List<byte[]> batches = new ArrayList<>();
		List<DimensionRow> dims = new ArrayList<>();
		List<StructureRow> rows = new ArrayList<>();
		long size = 0;
		for (DimensionRow dimension : dimensions) {
			long itemSize = estimate(dimension);
			if (size > 0 && size + itemSize > budget) {
				batches.add(encodeBatch(dims, rows));
				dims = new ArrayList<>();
				size = 0;
			}
			dims.add(dimension);
			size += itemSize;
		}
		for (StructureRow structure : structures) {
			long itemSize = estimate(structure);
			if (size > 0 && size + itemSize > budget) {
				batches.add(encodeBatch(dims, rows));
				dims = new ArrayList<>();
				rows = new ArrayList<>();
				size = 0;
			}
			rows.add(structure);
			size += itemSize;
		}
		if (!dims.isEmpty() || !rows.isEmpty() || batches.isEmpty()) {
			batches.add(encodeBatch(dims, rows));
		}
		return batches;
	}

	private static long estimate(DimensionRow dimension) {
		return 8 + dimension.id().length() + strings(dimension.biomes());
	}

	private static long estimate(StructureRow structure) {
		return 40 + structure.id().length() + structure.step().length() + structure.biomeTag().length() + structure.set().length()
			+ strings(structure.dimensions()) + strings(structure.biomes()) + strings(structure.lootTables());
	}

	private static long strings(List<String> list) {
		long size = 0;
		for (String s : list) {
			size += s.length() + 4;
		}
		return size;
	}

	public static byte[] encodeBatch(List<DimensionRow> dimensions, List<StructureRow> structures) {
		Table table = new Table();
		for (DimensionRow dimension : dimensions) {
			table.add(dimension.id());
			dimension.biomes().forEach(table::add);
		}
		for (StructureRow s : structures) {
			table.add(s.id());
			table.add(s.step());
			s.dimensions().forEach(table::add);
			s.biomes().forEach(table::add);
			if (!s.biomeTag().isEmpty()) {
				table.add(s.biomeTag());
			}
			if (!s.set().isEmpty()) {
				table.add(s.set());
			}
			s.lootTables().forEach(table::add);
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(VERSION);
			writeVarInt(out, table.strings.size());
			for (String string : table.strings.keySet()) {
				out.writeUTF(string);
			}
			writeVarInt(out, dimensions.size());
			for (DimensionRow dimension : dimensions) {
				writeVarInt(out, table.get(dimension.id()));
				writeIndices(out, table, dimension.biomes());
			}
			writeVarInt(out, structures.size());
			for (StructureRow s : structures) {
				writeVarInt(out, table.get(s.id()));
				writeVarInt(out, table.get(s.step()));
				writeIndices(out, table, s.dimensions());
				writeIndices(out, table, s.biomes());
				writeVarInt(out, s.biomeTag().isEmpty() ? 0 : table.get(s.biomeTag()) + 1);
				writeVarInt(out, s.set().isEmpty() ? 0 : table.get(s.set()) + 1);
				writeVarInt(out, s.weight());
				writeVarInt(out, s.totalWeight());
				Placement placement = s.placement();
				out.writeByte(placement.kind().ordinal());
				writeVarInt(out, placement.a());
				writeVarInt(out, placement.b());
				writeVarInt(out, placement.c());
				writeIndices(out, table, s.lootTables());
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot encode world data", e);
		}
	}

	/** @throws IllegalArgumentException if the data is malformed or of an unknown version */
	public static WorldBatch decode(byte[] data) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
			int version = in.readUnsignedByte();
			if (version != VERSION) {
				throw new IllegalArgumentException("Unknown world data version " + version);
			}
			String[] strings = new String[readCount(in)];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = in.readUTF();
			}
			List<DimensionRow> dimensions = new ArrayList<>();
			int dimensionCount = readCount(in);
			for (int i = 0; i < dimensionCount; i++) {
				dimensions.add(new DimensionRow(strings[readVarInt(in)], readStrings(in, strings)));
			}
			List<StructureRow> structures = new ArrayList<>();
			int structureCount = readCount(in);
			for (int i = 0; i < structureCount; i++) {
				String id = strings[readVarInt(in)];
				String step = strings[readVarInt(in)];
				List<String> dims = readStrings(in, strings);
				List<String> biomes = readStrings(in, strings);
				int tag = readVarInt(in);
				int set = readVarInt(in);
				int weight = readVarInt(in);
				int total = readVarInt(in);
				int kind = in.readUnsignedByte();
				if (kind >= Kind.values().length) {
					throw new IllegalArgumentException("Unknown placement kind " + kind);
				}
				Placement placement = new Placement(Kind.values()[kind], readVarInt(in), readVarInt(in), readVarInt(in));
				List<String> tables = readStrings(in, strings);
				structures.add(new StructureRow(id, step, dims, biomes, tag == 0 ? "" : strings[tag - 1], set == 0 ? "" : strings[set - 1],
					weight, total, placement, tables));
			}
			return new WorldBatch(dimensions, structures);
		} catch (IOException | ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
			throw new IllegalArgumentException("Malformed world data", e);
		}
	}

	private static final class Table {
		final Map<String, Integer> strings = new LinkedHashMap<>();

		void add(String string) {
			strings.putIfAbsent(string, strings.size());
		}

		int get(String string) {
			return strings.get(string);
		}
	}

	private static void writeIndices(DataOutputStream out, Table table, List<String> list) throws IOException {
		writeVarInt(out, list.size());
		for (String s : list) {
			writeVarInt(out, table.get(s));
		}
	}

	private static List<String> readStrings(DataInputStream in, String[] strings) throws IOException {
		int count = readCount(in);
		List<String> list = new ArrayList<>(Math.min(count, 1024));
		for (int i = 0; i < count; i++) {
			list.add(strings[readVarInt(in)]);
		}
		return list;
	}

	private static int readCount(DataInputStream in) throws IOException {
		int count = readVarInt(in);
		if (count < 0 || count > MAX_LIST) {
			throw new IllegalArgumentException("Bad count " + count);
		}
		return count;
	}

	static void writeVarInt(DataOutputStream out, int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			out.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.writeByte(value);
	}

	static int readVarInt(DataInputStream in) throws IOException {
		int value = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			int b = in.readUnsignedByte();
			value |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				return value;
			}
		}
		throw new IllegalArgumentException("VarInt too big");
	}
}
