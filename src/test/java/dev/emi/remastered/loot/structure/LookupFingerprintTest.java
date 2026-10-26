package dev.emi.remastered.loot.structure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The fingerprint is the same for the same inputs and changes when any input that a cached walk depends on changes:
 * a mod's version or jar, the order or files of the packs, a structure, a start pool, a pool's elements. A pack whose
 * files cannot be identified, and a file changed after the game started, make it uncacheable.
 */
class LookupFingerprintTest {
	private static final long OLD = 1_600_000_000_000L;
	private static final long STARTED = 1_700_000_000_000L;

	@TempDir
	Path dir;

	private Path jar;
	private Path zip;
	private Path pack;

	/** The inputs of one "start"; a test changes one thing and checks the hash. */
	private final class Inputs {
		String modVersion = "1.0.0";
		List<String> packOrder = List.of("vanilla", "file/a.zip", "file/b");
		String structureId = "minecraft:village_plains";
		String startPool = "minecraft:village/plains/town_centers";
		List<String> elements = List.of("net.minecraft.structure.pool.SinglePoolElement minecraft:village/plains/houses/plains_small_house_1",
			"net.minecraft.structure.pool.EmptyPoolElement");
		SortedMap<String, byte[]> generated = new TreeMap<>(Map.of("gen:structure/hut.nbt", new byte[] {1, 2, 3}));
		boolean unknownPack;

		LookupFingerprint feed() throws IOException {
			LookupFingerprint fingerprint = new LookupFingerprint(STARTED);
			fingerprint.format(StructureCache.FORMAT);
			fingerprint.mod("emi", "2.0.0", "PATH", List.of(jar), null, null);
			fingerprint.mod("lithostitched", modVersion, "NESTED", List.of(), "emi", "META-INF/jars/x.jar");
			for (String id : packOrder) {
				switch (id) {
					case "vanilla" -> fingerprint.vanillaPack(0, id, "net.minecraft.resource.DefaultResourcePack");
					case "file/a.zip" -> fingerprint.zipPack(0, id, "net.minecraft.resource.ZipResourcePack", zip);
					default -> fingerprint.directoryPack(0, id, "net.minecraft.resource.DirectoryResourcePack", pack);
				}
			}
			fingerprint.contentPack(0, "generated", "some.mod.MemoryPack", generated);
			if (unknownPack) {
				fingerprint.unidentifiedPack(0, "odd", "some.mod.OddPack", "java.lang.UnsupportedOperationException");
			}
			fingerprint.structure(structureId, "minecraft:jigsaw", "net.minecraft.world.gen.structure.JigsawStructure", startPool);
			fingerprint.structure("minecraft:desert_pyramid", "minecraft:desert_pyramid", "net.minecraft.world.gen.structure.DesertPyramidStructure", null);
			fingerprint.pool("minecraft:village/plains/town_centers", "minecraft:empty", elements);
			return fingerprint;
		}

		byte[] hash() throws IOException {
			LookupFingerprint fingerprint = feed();
			assertNull(fingerprint.uncacheableReason());
			return fingerprint.finish();
		}
	}

	@BeforeEach
	void files() throws IOException {
		jar = write(dir.resolve("mods").resolve("emi.jar"), "jar");
		zip = write(dir.resolve("datapacks").resolve("a.zip"), "zip");
		pack = dir.resolve("datapacks").resolve("b");
		write(pack.resolve("pack.mcmeta"), "{}");
		write(pack.resolve("data/b/structure/house.nbt"), "house");
		write(pack.resolve("data/b/structure/deep/tower.nbt"), "tower");
		write(pack.resolve("data/b/loot_table/chests/house.json"), "{}");
	}

	private static Path write(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
		Files.setLastModifiedTime(file, FileTime.fromMillis(OLD));
		return file;
	}

	private byte[] changed(Consumer<Inputs> change) throws IOException {
		Inputs inputs = new Inputs();
		change.accept(inputs);
		return inputs.hash();
	}

	private void assertChanges(Consumer<Inputs> change) throws IOException {
		byte[] before = new Inputs().hash();
		byte[] after = changed(change);
		assertFalse(Arrays.equals(before, after));
	}

	@Test
	void sameInputsSameHash() throws IOException {
		assertArrayEquals(new Inputs().hash(), new Inputs().hash());
		assertEquals(StructureCache.FINGERPRINT_BYTES, new Inputs().hash().length);
	}

	@Test
	void modVersion() throws IOException {
		assertChanges(inputs -> inputs.modVersion = "1.0.1");
	}

	@Test
	void jarModifiedTime() throws IOException {
		byte[] before = new Inputs().hash();
		Files.setLastModifiedTime(jar, FileTime.fromMillis(OLD + 1));
		assertFalse(Arrays.equals(before, new Inputs().hash()));
	}

	@Test
	void jarSize() throws IOException {
		byte[] before = new Inputs().hash();
		write(jar, "jar!");
		assertFalse(Arrays.equals(before, new Inputs().hash()));
	}

	@Test
	void packOrder() throws IOException {
		assertChanges(inputs -> inputs.packOrder = List.of("vanilla", "file/b", "file/a.zip"));
	}

	@Test
	void zipPackModifiedTime() throws IOException {
		byte[] before = new Inputs().hash();
		Files.setLastModifiedTime(zip, FileTime.fromMillis(OLD + 1000));
		assertFalse(Arrays.equals(before, new Inputs().hash()));
	}

	@Test
	void directoryPackTemplateModifiedTime() throws IOException {
		byte[] before = new Inputs().hash();
		Files.setLastModifiedTime(pack.resolve("data/b/structure/deep/tower.nbt"), FileTime.fromMillis(OLD + 1));
		assertFalse(Arrays.equals(before, new Inputs().hash()));
	}

	@Test
	void directoryPackNewTemplateAndMeta() throws IOException {
		byte[] before = new Inputs().hash();
		write(pack.resolve("data/c/structure/new.nbt"), "new");
		byte[] added = new Inputs().hash();
		assertFalse(Arrays.equals(before, added));
		write(pack.resolve("pack.mcmeta"), "{\"filter\":{}}");
		assertFalse(Arrays.equals(added, new Inputs().hash()));
	}

	@Test
	void directoryPackFilesOutsideStructuresDoNotMatter() throws IOException {
		byte[] before = new Inputs().hash();
		write(pack.resolve("data/b/loot_table/chests/house.json"), "{\"pools\":[]}");
		assertArrayEquals(before, new Inputs().hash());
	}

	@Test
	void generatedPackContent() throws IOException {
		assertChanges(inputs -> inputs.generated.put("gen:structure/hut.nbt", new byte[] {1, 2, 4}));
		assertChanges(inputs -> inputs.generated.put("gen:structure/other.nbt", new byte[0]));
	}

	@Test
	void structureId() throws IOException {
		assertChanges(inputs -> inputs.structureId = "minecraft:village_desert");
	}

	@Test
	void startPool() throws IOException {
		assertChanges(inputs -> inputs.startPool = "minecraft:village/desert/town_centers");
		assertChanges(inputs -> inputs.startPool = null);
	}

	@Test
	void poolElements() throws IOException {
		assertChanges(inputs -> inputs.elements = List.of(inputs.elements.get(1), inputs.elements.get(0)));
		assertChanges(inputs -> inputs.elements = List.of(inputs.elements.get(0)));
	}

	@Test
	void fieldsCannotShift() {
		byte[] a = new LookupFingerprint(STARTED).structure("a:b", "c", "d", "e").finish();
		byte[] b = new LookupFingerprint(STARTED).structure("a:bc", "", "d", "e").finish();
		byte[] c = new LookupFingerprint(STARTED).structure("a:b", "c", "d", null).finish();
		byte[] d = new LookupFingerprint(STARTED).structure("a:b", "c", "d", "null").finish();
		assertFalse(Arrays.equals(a, b));
		assertFalse(Arrays.equals(c, d));
	}

	@Test
	void unknownPackIsUncacheable() throws IOException {
		Inputs inputs = new Inputs();
		inputs.unknownPack = true;
		LookupFingerprint fingerprint = inputs.feed();
		assertNotNull(fingerprint.uncacheableReason());
		assertTrue(fingerprint.uncacheableReason().contains("odd"));
	}

	@Test
	void fileChangedAfterStartIsUncacheable() throws IOException {
		Files.setLastModifiedTime(pack.resolve("data/b/structure/house.nbt"), FileTime.fromMillis(STARTED + 5));
		LookupFingerprint fingerprint = new Inputs().feed();
		assertNotNull(fingerprint.uncacheableReason());
		assertTrue(fingerprint.uncacheableReason().contains("house.nbt"));
	}

	@Test
	void missingFileIsRecorded() throws IOException {
		byte[] before = new Inputs().hash();
		Files.delete(zip);
		assertFalse(Arrays.equals(before, new Inputs().hash()));
	}
}
