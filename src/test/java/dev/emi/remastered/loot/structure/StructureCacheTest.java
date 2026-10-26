package dev.emi.remastered.loot.structure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The cache file: what is written is read back exactly (order included), and any damaged or foreign file is a miss. */
class StructureCacheTest {
	@TempDir
	Path dir;

	private static byte[] fingerprint(int seed) {
		byte[] bytes = new byte[StructureCache.FINGERPRINT_BYTES];
		Arrays.fill(bytes, (byte) seed);
		return bytes;
	}

	/** Several structures sharing tables, order not alphabetical, an empty walk, non-ASCII text. */
	private static Map<String, List<String>> walks() {
		Map<String, List<String>> walks = new LinkedHashMap<>();
		walks.put("minecraft:village_plains", List.of("minecraft:chests/village/village_weaponsmith", "minecraft:chests/village/village_cartographer"));
		walks.put("minecraft:ancient_city", List.of("minecraft:chests/ancient_city", "minecraft:chests/ancient_city_ice_box"));
		walks.put("mod:empty", List.of());
		walks.put("möd:hütte", List.of("möd:chests/hütte"));
		walks.put("minecraft:bastion_remnant", List.of("minecraft:chests/village/village_weaponsmith", "minecraft:chests/bastion_other"));
		return walks;
	}

	@Test
	void roundTripKeepsEverythingInOrder() throws IOException {
		Path file = dir.resolve("cache").resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(7), walks());
		StructureCache.Loaded loaded = StructureCache.read(file, fingerprint(7));
		assertTrue(loaded.hit(), loaded.reason());
		assertEquals(walks(), loaded.walks());
		assertEquals(new ArrayList<>(walks().keySet()), new ArrayList<>(loaded.walks().keySet()));
	}

	@Test
	void onlyCompleteBuildsWithoutFailedWalksAreSaved() {
		assertNull(StructureCache.notSavedReason(true, 0));
		assertEquals("1 structure could not be read", StructureCache.notSavedReason(true, 1));
		assertEquals("3 structures could not be read", StructureCache.notSavedReason(true, 3));
		assertNotNull(StructureCache.notSavedReason(false, 0));
	}

	@Test
	void formatOneFileIsAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(1), walks());
		byte[] bytes = Files.readAllBytes(file);
		bytes[7] = 1;
		Files.write(file, bytes);
		StructureCache.Loaded loaded = StructureCache.read(file, fingerprint(1));
		assertFalse(loaded.hit());
		assertFalse(loaded.ioError());
		assertEquals("the cache file has another format version", loaded.reason());
	}

	@Test
	void writeReplacesTheFileAndLeavesNoTemporaryFile() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(1), walks());
		Map<String, List<String>> other = new LinkedHashMap<>();
		other.put("a:b", List.of("a:chests/c"));
		StructureCache.write(file, fingerprint(2), other);
		assertFalse(StructureCache.read(file, fingerprint(1)).hit());
		assertEquals(other, StructureCache.read(file, fingerprint(2)).walks());
		try (Stream<Path> files = Files.list(dir)) {
			assertEquals(List.of(file), files.toList());
		}
	}

	@Test
	void otherFingerprintIsAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(3), walks());
		StructureCache.Loaded loaded = StructureCache.read(file, fingerprint(4));
		assertFalse(loaded.hit());
		assertFalse(loaded.ioError());
	}

	@Test
	void missingFileIsAMiss() {
		StructureCache.Loaded loaded = StructureCache.read(dir.resolve("none.dat"), fingerprint(1));
		assertFalse(loaded.hit());
		assertFalse(loaded.ioError());
	}

	@Test
	void everyTruncationIsAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(5), walks());
		byte[] bytes = Files.readAllBytes(file);
		Path cut = dir.resolve("cut.dat");
		for (int length = 0; length < bytes.length; length++) {
			Files.write(cut, Arrays.copyOf(bytes, length));
			StructureCache.Loaded loaded = StructureCache.read(cut, fingerprint(5));
			assertFalse(loaded.hit(), "cut at " + length);
			assertFalse(loaded.ioError(), "cut at " + length);
		}
	}

	@Test
	void everyFlippedByteIsAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(6), walks());
		byte[] bytes = Files.readAllBytes(file);
		Path damaged = dir.resolve("damaged.dat");
		for (int i = 0; i < bytes.length; i++) {
			byte[] copy = bytes.clone();
			copy[i] ^= 0x5A;
			Files.write(damaged, copy);
			assertFalse(StructureCache.read(damaged, fingerprint(6)).hit(), "byte " + i);
		}
	}

	@Test
	void trailingBytesAreAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(6), walks());
		byte[] bytes = Files.readAllBytes(file);
		Files.write(file, Arrays.copyOf(bytes, bytes.length + 1));
		assertFalse(StructureCache.read(file, fingerprint(6)).hit());
	}

	@Test
	void wrongMagicOrVersionIsAMiss() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeInt(0x12345678);
		out.writeInt(StructureCache.FORMAT);
		out.write(fingerprint(1));
		Files.write(file, bytes.toByteArray());
		assertFalse(StructureCache.read(file, fingerprint(1)).hit());

		bytes.reset();
		out.writeInt(StructureCache.MAGIC);
		out.writeInt(StructureCache.FORMAT + 1);
		out.write(fingerprint(1));
		Files.write(file, bytes.toByteArray());
		StructureCache.Loaded loaded = StructureCache.read(file, fingerprint(1));
		assertFalse(loaded.hit());
		assertFalse(loaded.ioError());

		Files.write(file, new byte[] {1, 2, 3});
		assertFalse(StructureCache.read(file, fingerprint(1)).hit());
		Files.write(file, new byte[0]);
		assertFalse(StructureCache.read(file, fingerprint(1)).hit());
	}

	@Test
	void hugeCountsAreAMissNotAnError() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeInt(StructureCache.MAGIC);
		out.writeInt(StructureCache.FORMAT);
		out.write(fingerprint(1));
		out.writeInt(Integer.MAX_VALUE);
		Files.write(file, bytes.toByteArray());
		StructureCache.Loaded loaded = StructureCache.read(file, fingerprint(1));
		assertFalse(loaded.hit());
		assertFalse(loaded.ioError());
	}

	@Test
	void aDirectoryInPlaceOfTheFileIsNoHit() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		Files.createDirectories(file);
		assertFalse(StructureCache.read(file, fingerprint(1)).hit());
	}

	@Test
	void fingerprintIsStoredAsGiven() throws IOException {
		Path file = dir.resolve("structure-lookup.dat");
		StructureCache.write(file, fingerprint(9), walks());
		byte[] bytes = Files.readAllBytes(file);
		assertArrayEquals(fingerprint(9), Arrays.copyOfRange(bytes, 8, 8 + StructureCache.FINGERPRINT_BYTES));
	}
}
