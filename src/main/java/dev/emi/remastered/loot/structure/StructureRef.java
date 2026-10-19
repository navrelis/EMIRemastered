package dev.emi.remastered.loot.structure;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A structure a chest loot table is found in, with the dimensions whose biomes the structure can generate in.
 */
public record StructureRef(Identifier structure, List<Identifier> dimensions) {

	/** Marker byte that precedes the structure block at the end of a chest loot payload. */
	public static final byte BLOCK_VERSION = 1;
	private static final int MAX_STRUCTURES = 64;
	private static final int MAX_DIMENSIONS = 16;

	public static void write(PacketByteBuf buf, List<StructureRef> refs) {
		if (refs.isEmpty()) {
			return;
		}
		int count = Math.min(refs.size(), MAX_STRUCTURES);
		buf.writeByte(BLOCK_VERSION);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			StructureRef ref = refs.get(i);
			buf.writeIdentifier(ref.structure);
			int dims = Math.min(ref.dimensions.size(), MAX_DIMENSIONS);
			buf.writeVarInt(dims);
			for (int j = 0; j < dims; j++) {
				buf.writeIdentifier(ref.dimensions.get(j));
			}
		}
	}

	/** Reads the optional block that follows the items. A payload from an older server simply ends before it. */
	public static List<StructureRef> read(PacketByteBuf buf) {
		if (!buf.isReadable()) {
			return List.of();
		}
		byte version = buf.readByte();
		if (version != BLOCK_VERSION) {
			return List.of();
		}
		int count = Math.min(buf.readVarInt(), MAX_STRUCTURES);
		List<StructureRef> refs = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Identifier structure = buf.readIdentifier();
			int dims = Math.min(buf.readVarInt(), MAX_DIMENSIONS);
			List<Identifier> dimensions = new ArrayList<>(dims);
			for (int j = 0; j < dims; j++) {
				dimensions.add(buf.readIdentifier());
			}
			refs.add(new StructureRef(structure, List.copyOf(dimensions)));
		}
		return List.copyOf(refs);
	}
}
