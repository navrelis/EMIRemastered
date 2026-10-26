package dev.emi.remastered.loot.structure;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * The file of the structure lookup cache: for every jigsaw structure (in registry order) the loot tables its walk found
 * (in the order of the walk). Only builds whose walks all succeeded are written, so it holds no failures. Pure Java.
 * <p>
 * Format (big endian): magic, format version, the 32 byte fingerprint, a string table, the walks as indices into it,
 * then the CRC32 of everything before. A file with another magic, version or fingerprint, a bad checksum, bad indices,
 * a cut-off end or trailing bytes is a miss, never an exception. It is written to a temporary file next to it and
 * moved over the old one, atomically where the file system can, so a reader never sees half a file.
 */
final class StructureCache {
	static final int MAGIC = 0x454D534C; // "EMSL"
	/** 2: no failed walks in the file (1 could hold them). */
	static final int FORMAT = 2;
	static final int FINGERPRINT_BYTES = 32;
	private static final int MAX_COUNT = 10_000_000;

	/**
	 * What {@link #read} found: on a hit the loot tables per structure, else null and why not. {@code ioError} for an
	 * unexpected IO error.
	 */
	record Loaded(@Nullable Map<String, List<String>> walks, String reason, boolean ioError) {
		boolean hit() {
			return walks != null;
		}
	}

	private StructureCache() {
	}

	/**
	 * Why a build must not be saved, or null when it may: only a complete build whose walks all succeeded is saved. A
	 * failed walk may be a passing problem (an IO error, a read racing something), and a saved build would show it on
	 * every later start; without saving, the next start walks again.
	 */
	static @Nullable String notSavedReason(boolean complete, int failedWalks) {
		if (!complete) {
			return "the lookup did not finish";
		}
		if (failedWalks > 0) {
			return failedWalks + (failedWalks == 1 ? " structure" : " structures") + " could not be read";
		}
		return null;
	}

	/** Writes the loot tables per structure with their fingerprint, replacing the file at once. */
	static void write(Path file, byte[] fingerprint, Map<String, List<String>> walks) throws IOException {
		if (fingerprint.length != FINGERPRINT_BYTES) {
			throw new IllegalArgumentException("fingerprint must have " + FINGERPRINT_BYTES + " bytes");
		}
		Path dir = file.toAbsolutePath().getParent();
		Files.createDirectories(dir);
		Path temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
		try {
			try (OutputStream raw = new BufferedOutputStream(Files.newOutputStream(temp))) {
				CRC32 crc = new CRC32();
				DataOutputStream out = new DataOutputStream(new CheckedOutputStream(raw, crc));
				writePayload(out, fingerprint, walks);
				out.flush();
				new DataOutputStream(raw).writeLong(crc.getValue());
			}
			try {
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private static void writePayload(DataOutputStream out, byte[] fingerprint, Map<String, List<String>> walks) throws IOException {
		out.writeInt(MAGIC);
		out.writeInt(FORMAT);
		out.write(fingerprint);
		Map<String, Integer> indices = new HashMap<>();
		List<String> strings = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : walks.entrySet()) {
			index(entry.getKey(), indices, strings);
			for (String table : entry.getValue()) {
				index(table, indices, strings);
			}
		}
		out.writeInt(strings.size());
		for (String string : strings) {
			writeString(out, string);
		}
		out.writeInt(walks.size());
		for (Map.Entry<String, List<String>> entry : walks.entrySet()) {
			out.writeInt(indices.get(entry.getKey()));
			out.writeInt(entry.getValue().size());
			for (String table : entry.getValue()) {
				out.writeInt(indices.get(table));
			}
		}
	}

	private static void index(String string, Map<String, Integer> indices, List<String> strings) {
		if (!indices.containsKey(string)) {
			indices.put(string, strings.size());
			strings.add(string);
		}
	}

	private static void writeString(DataOutputStream out, String string) throws IOException {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	/** Reads the walks if the file exists, is intact and has this fingerprint. Never throws. */
	static Loaded read(Path file, byte[] fingerprint) {
		try (InputStream raw = new BufferedInputStream(Files.newInputStream(file))) {
			CRC32 crc = new CRC32();
			DataInputStream in = new DataInputStream(new CheckedInputStream(raw, crc));
			if (in.readInt() != MAGIC) {
				return miss("the cache file is not a structure lookup cache");
			}
			if (in.readInt() != FORMAT) {
				return miss("the cache file has another format version");
			}
			byte[] stored = new byte[FINGERPRINT_BYTES];
			in.readFully(stored);
			if (!Arrays.equals(stored, fingerprint)) {
				return miss("the mods, data packs or structures changed since the cache was written");
			}
			Map<String, List<String>> walks = readPayload(in);
			if (walks == null) {
				return miss("the cache file is damaged");
			}
			long expected = crc.getValue();
			if (new DataInputStream(raw).readLong() != expected || raw.read() != -1) {
				return miss("the cache file is damaged");
			}
			return new Loaded(walks, "", false);
		} catch (NoSuchFileException e) {
			return miss("there is no cache file yet");
		} catch (EOFException e) {
			return miss("the cache file is damaged");
		} catch (IOException e) {
			return new Loaded(null, "the cache file could not be read: " + e, true);
		} catch (RuntimeException e) {
			return miss("the cache file is damaged");
		}
	}

	private static @Nullable Map<String, List<String>> readPayload(DataInputStream in) throws IOException {
		int stringCount = in.readInt();
		if (stringCount < 0 || stringCount > MAX_COUNT) {
			return null;
		}
		String[] strings = new String[stringCount];
		for (int i = 0; i < stringCount; i++) {
			strings[i] = readString(in);
			if (strings[i] == null) {
				return null;
			}
		}
		int walkCount = in.readInt();
		if (walkCount < 0 || walkCount > stringCount) {
			return null;
		}
		Map<String, List<String>> walks = new LinkedHashMap<>();
		for (int i = 0; i < walkCount; i++) {
			int structure = in.readInt();
			if (structure < 0 || structure >= stringCount) {
				return null;
			}
			int tableCount = in.readInt();
			if (tableCount < 0 || tableCount > stringCount) {
				return null;
			}
			List<String> tables = new ArrayList<>(tableCount);
			for (int t = 0; t < tableCount; t++) {
				int table = in.readInt();
				if (table < 0 || table >= stringCount) {
					return null;
				}
				tables.add(strings[table]);
			}
			if (walks.put(strings[structure], List.copyOf(tables)) != null) {
				return null;
			}
		}
		return walks;
	}

	private static @Nullable String readString(DataInputStream in) throws IOException {
		int length = in.readInt();
		if (length < 0 || length > MAX_COUNT) {
			return null;
		}
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static Loaded miss(String reason) {
		return new Loaded(null, reason, false);
	}
}
