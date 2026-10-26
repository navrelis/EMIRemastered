package dev.emi.remastered.loot.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Reads only the parts of a structure template file that can name loot tables and jigsaw pools: the block entity data
 * ("nbt") of every entry of "blocks" and the entity data ("nbt") of every entry of "entities".
 * <p>
 * Reading a whole template with {@link net.minecraft.nbt.NbtIo#readCompressed} builds an object for every block
 * position and state of the template and its palette, which is most of the structure lookup's time: the templates of
 * a large pack inflate to about a gigabyte of NBT. This reader steps over everything else in a small window of the
 * inflated bytes without building it, and reads each "nbt" compound with the game's own compound reader, so those
 * compounds are the very same as in the full read (same keys in the same order).
 * <p>
 * It fails where the full read fails: a file that is not gzip compressed, keys and skipped strings that are no modified
 * UTF-8, lists without an element type, negative lengths, unknown tag types, a truncated file and a depth over 512
 * throw, as they do there; like the full read it inflates only as far as the data goes, so what follows the data (the
 * gzip trailer) is not looked at. When a key repeats, the last one counts, as in a compound. (Only a corrupt array or
 * list length too large for memory outside the "nbt" compounds differs: the full read runs out of memory, which the
 * lookup did not catch, this one finds the file too short and the template has no loot tables.)
 */
final class TemplateNbt {
	private static final int END = NbtElement.END_TYPE;
	private static final int COMPOUND = NbtElement.COMPOUND_TYPE;
	private static final int LIST = NbtElement.LIST_TYPE;
	private static final byte[] NBT = "nbt".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] BLOCKS = "blocks".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] ENTITIES = "entities".getBytes(StandardCharsets.US_ASCII);

	private final InputStream inflated;
	private final NbtSizeTracker tracker = NbtSizeTracker.ofUnlimitedBytes();
	/** The window of inflated bytes: {@code [pos, limit)} is read but not used yet. */
	private byte[] buffer = new byte[64 * 1024];
	private int pos;
	private int limit;
	/** The key (or string) last read, when it was not plain ASCII; else null and it is {@code buffer[keyStart, keyEnd)}. */
	private String decoded;
	private int keyStart;
	private int keyEnd;

	private TemplateNbt(InputStream inflated) {
		this.inflated = inflated;
	}

	/**
	 * The "nbt" compounds of the blocks (in order) and then of the entities (in order) of a gzip compressed template:
	 * the compounds {@link #containerNbt(NbtCompound)} returns for the same file read in full.
	 *
	 * @throws IOException (or a runtime exception) whenever reading the file in full would throw
	 */
	static List<NbtCompound> read(InputStream compressed) throws IOException {
		try (InputStream inflated = new GZIPInputStream(compressed)) {
			return new TemplateNbt(inflated).readRoot();
		}
	}

	/** The same compounds taken from a template that was read in full. */
	static List<NbtCompound> containerNbt(NbtCompound root) {
		List<NbtCompound> result = new ArrayList<>();
		for (String key : new String[] {"blocks", "entities"}) {
			NbtList list = root.getList(key, NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < list.size(); i++) {
				NbtCompound element = list.getCompound(i);
				if (element.contains("nbt", NbtElement.COMPOUND_TYPE)) {
					result.add(element.getCompound("nbt"));
				}
			}
		}
		return result;
	}

	private List<NbtCompound> readRoot() throws IOException {
		int type = readByte();
		if (type != COMPOUND) {
			throw new IOException("Root tag must be a named compound tag");
		}
		// the root's name, which NbtIo skips without checking it
		skipBytes(readUnsignedShort());
		List<NbtCompound> blocks = List.of();
		List<NbtCompound> entities = List.of();
		tracker.pushStack();
		while ((type = readByte()) != END) {
			readString();
			boolean blocksKey = isKey(BLOCKS, "blocks");
			boolean entitiesKey = !blocksKey && isKey(ENTITIES, "entities");
			if (type == LIST && (blocksKey || entitiesKey)) {
				List<NbtCompound> nbt = readElementNbt();
				if (blocksKey) {
					blocks = nbt;
				} else {
					entities = nbt;
				}
			} else {
				skip(type);
				// a later "blocks" that is no list replaces the earlier one: getList then finds nothing
				if (blocksKey) {
					blocks = List.of();
				} else if (entitiesKey) {
					entities = List.of();
				}
			}
		}
		tracker.popStack();
		List<NbtCompound> result = new ArrayList<>(blocks.size() + entities.size());
		for (NbtCompound nbt : blocks) {
			if (nbt != null) {
				result.add(nbt);
			}
		}
		for (NbtCompound nbt : entities) {
			if (nbt != null) {
				result.add(nbt);
			}
		}
		return result;
	}

	/** A list of "blocks" or "entities": the "nbt" compound of each compound element (null when it has none). */
	private List<NbtCompound> readElementNbt() throws IOException {
		tracker.pushStack();
		int type = readByte();
		int size = readInt();
		checkList(type, size);
		List<NbtCompound> result = List.of();
		if (type == COMPOUND) {
			result = new ArrayList<>(Math.min(size, 4096));
			for (int i = 0; i < size; i++) {
				result.add(readElement());
			}
		} else {
			// getList finds no compound list here
			skipList(type, size);
		}
		tracker.popStack();
		return result;
	}

	private NbtCompound readElement() throws IOException {
		tracker.pushStack();
		NbtCompound nbt = null;
		int type;
		while ((type = readByte()) != END) {
			readString();
			if (isKey(NBT, "nbt")) {
				nbt = null;
				if (type == COMPOUND) {
					nbt = NbtCompound.TYPE.read(new DataInputStream(new Window()), tracker);
				} else {
					skip(type);
				}
			} else {
				skip(type);
			}
		}
		tracker.popStack();
		return nbt;
	}

	/** Reads past one value of {@code type}, checking it as reading it would. */
	private void skip(int type) throws IOException {
		switch (type) {
			case NbtElement.BYTE_TYPE -> skipBytes(1);
			case NbtElement.SHORT_TYPE -> skipBytes(2);
			case NbtElement.INT_TYPE, NbtElement.FLOAT_TYPE -> skipBytes(4);
			case NbtElement.LONG_TYPE, NbtElement.DOUBLE_TYPE -> skipBytes(8);
			case NbtElement.BYTE_ARRAY_TYPE -> skipBytes(arrayLength());
			case NbtElement.INT_ARRAY_TYPE -> skipBytes(4L * arrayLength());
			case NbtElement.LONG_ARRAY_TYPE -> skipBytes(8L * arrayLength());
			case NbtElement.STRING_TYPE -> readString();
			case LIST -> {
				tracker.pushStack();
				int elementType = readByte();
				int size = readInt();
				checkList(elementType, size);
				skipList(elementType, size);
				tracker.popStack();
			}
			case COMPOUND -> {
				tracker.pushStack();
				int entryType;
				while ((entryType = readByte()) != END) {
					readString();
					skip(entryType);
				}
				tracker.popStack();
			}
			default -> throw new IOException("Invalid tag id: " + type);
		}
	}

	private void skipList(int type, int size) throws IOException {
		int width = switch (type) {
			case NbtElement.BYTE_TYPE -> 1;
			case NbtElement.SHORT_TYPE -> 2;
			case NbtElement.INT_TYPE, NbtElement.FLOAT_TYPE -> 4;
			case NbtElement.LONG_TYPE, NbtElement.DOUBLE_TYPE -> 8;
			default -> 0;
		};
		if (width > 0) {
			// block positions and the like: a run of numbers
			skipBytes((long) width * size);
		} else if (type != END) {
			for (int i = 0; i < size; i++) {
				skip(type);
			}
		}
	}

	private int arrayLength() throws IOException {
		int length = readInt();
		if (length < 0) {
			throw new IOException("Negative array length " + length);
		}
		return length;
	}

	private static void checkList(int type, int size) throws IOException {
		if (type == END && size > 0) {
			throw new IOException("Missing type on ListTag");
		}
		if (size < 0) {
			throw new IOException("Negative list length " + size);
		}
	}

	/**
	 * Reads a modified UTF-8 string (a key or a string value). Plain ASCII, nearly every key, is checked in place
	 * without making a String; anything else is decoded as {@link DataInputStream#readUTF} does, so that it fails the
	 * same way and an unusual encoding of a key still matches.
	 */
	private void readString() throws IOException {
		int length = readUnsignedShort();
		require(length);
		keyStart = pos;
		keyEnd = pos + length;
		pos = keyEnd;
		decoded = null;
		for (int i = keyStart; i < keyEnd; i++) {
			if (buffer[i] < 0) {
				byte[] utf = new byte[length + 2];
				utf[0] = (byte) (length >>> 8);
				utf[1] = (byte) length;
				System.arraycopy(buffer, keyStart, utf, 2, length);
				decoded = new DataInputStream(new ByteArrayInputStream(utf)).readUTF();
				return;
			}
		}
	}

	private boolean isKey(byte[] ascii, String key) {
		if (decoded != null) {
			return decoded.equals(key);
		}
		if (keyEnd - keyStart != ascii.length) {
			return false;
		}
		for (int i = 0; i < ascii.length; i++) {
			if (buffer[keyStart + i] != ascii[i]) {
				return false;
			}
		}
		return true;
	}

	private int readByte() throws IOException {
		require(1);
		return buffer[pos++];
	}

	private int readUnsignedShort() throws IOException {
		require(2);
		int value = (buffer[pos] & 0xFF) << 8 | buffer[pos + 1] & 0xFF;
		pos += 2;
		return value;
	}

	private int readInt() throws IOException {
		require(4);
		int value = (buffer[pos] & 0xFF) << 24 | (buffer[pos + 1] & 0xFF) << 16 | (buffer[pos + 2] & 0xFF) << 8 | buffer[pos + 3] & 0xFF;
		pos += 4;
		return value;
	}

	private void skipBytes(long count) throws IOException {
		while (count > 0) {
			if (pos == limit && !fill()) {
				throw new EOFException();
			}
			int step = (int) Math.min(count, limit - pos);
			pos += step;
			count -= step;
		}
	}

	/** Makes the next {@code count} bytes available in the window. */
	private void require(int count) throws IOException {
		while (limit - pos < count) {
			if (!fill()) {
				throw new EOFException();
			}
		}
	}

	/** Inflates more bytes into the window, keeping the unused ones; false at the end of the data. */
	private boolean fill() throws IOException {
		if (pos > 0) {
			System.arraycopy(buffer, pos, buffer, 0, limit - pos);
			limit -= pos;
			keyStart -= pos;
			keyEnd -= pos;
			pos = 0;
		}
		if (limit == buffer.length) {
			buffer = Arrays.copyOf(buffer, buffer.length * 2);
		}
		int read = inflated.read(buffer, limit, buffer.length - limit);
		if (read < 0) {
			return false;
		}
		limit += read;
		return true;
	}

	/** The rest of the data as a stream, for the game's compound reader. */
	private final class Window extends InputStream {
		@Override
		public int read() throws IOException {
			if (pos == limit && !fill()) {
				return -1;
			}
			return buffer[pos++] & 0xFF;
		}

		@Override
		public int read(byte[] target, int offset, int length) throws IOException {
			if (length == 0) {
				return 0;
			}
			if (pos == limit && !fill()) {
				return -1;
			}
			int count = Math.min(length, limit - pos);
			System.arraycopy(buffer, pos, target, offset, count);
			pos += count;
			return count;
		}
	}
}
