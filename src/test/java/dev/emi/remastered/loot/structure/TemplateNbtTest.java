package dev.emi.remastered.loot.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;

/**
 * The structure lookup reads only the block entity and entity data of a template ({@link TemplateNbt}); it must find
 * exactly what reading the whole template finds: the same loot tables and pools in the same order, and fail where the
 * full read fails.
 */
class TemplateNbtTest {

	/** What the lookup gets from a template file: the loot tables and pools in order, or FAIL (the lookup then uses none). */
	private static String full(byte[] file) {
		try {
			return describe(StructureLookup.TemplateInfo.of(NbtIo.readCompressed(new ByteArrayInputStream(file), NbtSizeTracker.ofUnlimitedBytes())));
		} catch (Throwable e) {
			return "FAIL";
		}
	}

	private static String partial(byte[] file) {
		try {
			return describe(StructureLookup.TemplateInfo.of(TemplateNbt.read(new ByteArrayInputStream(file))));
		} catch (Throwable e) {
			return "FAIL";
		}
	}

	private static String describe(StructureLookup.TemplateInfo info) {
		return List.copyOf(info.lootTables) + " " + List.copyOf(info.pools);
	}

	private static byte[] compress(NbtCompound root) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		NbtIo.writeCompressed(root, out);
		return out.toByteArray();
	}

	private static NbtList ints(int... values) {
		NbtList list = new NbtList();
		for (int value : values) {
			list.add(NbtInt.of(value));
		}
		return list;
	}

	/** A template like the game writes it, with chests, barrels, jigsaws, nested data and loot chest minecarts. */
	private static NbtCompound template(Random random) {
		NbtCompound root = new NbtCompound();
		root.putInt("DataVersion", 3955);
		root.put("size", ints(16, 12, 16));
		NbtList palette = new NbtList();
		for (int i = 0; i < 8; i++) {
			NbtCompound state = new NbtCompound();
			state.putString("Name", "minecraft:block_" + i);
			NbtCompound properties = new NbtCompound();
			properties.putString("facing", "north");
			properties.putString("waterlogged", "false");
			state.put("Properties", properties);
			palette.add(state);
		}
		root.put("palette", palette);
		NbtList blocks = new NbtList();
		int count = 50 + random.nextInt(400);
		for (int i = 0; i < count; i++) {
			NbtCompound block = new NbtCompound();
			block.put("pos", ints(random.nextInt(16), random.nextInt(12), random.nextInt(16)));
			block.putInt("state", random.nextInt(8));
			int kind = random.nextInt(20);
			if (kind == 0) {
				NbtCompound nbt = new NbtCompound();
				nbt.putString("id", "minecraft:chest");
				nbt.putString("LootTable", "mod:chests/room_" + random.nextInt(30));
				nbt.putLong("LootTableSeed", random.nextLong());
				block.put("nbt", nbt);
			} else if (kind == 1) {
				NbtCompound nbt = new NbtCompound();
				nbt.putString("id", "minecraft:jigsaw");
				nbt.putString("pool", "mod:pool_" + random.nextInt(25));
				nbt.putString("target", "mod:connector");
				nbt.putString("name", "mod:start");
				nbt.putString("final_state", "minecraft:air");
				nbt.putString("joint", "rollable");
				block.put("nbt", nbt);
			} else if (kind == 2) {
				NbtCompound nbt = new NbtCompound();
				nbt.putString("id", "minecraft:vault");
				NbtCompound config = new NbtCompound();
				config.putString("loot_table", "minecraft:chests/trial_chambers/reward_" + random.nextInt(4));
				nbt.put("config", config);
				NbtList items = new NbtList();
				NbtCompound item = new NbtCompound();
				item.putString("id", "minecraft:stone");
				item.putString("LootTable", "mod:chests/nested_" + random.nextInt(5));
				items.add(item);
				nbt.put("Items", items);
				block.put("nbt", nbt);
			} else if (kind == 3) {
				// a "pool" without "target" is no jigsaw
				NbtCompound nbt = new NbtCompound();
				nbt.putString("pool", "mod:not_a_jigsaw");
				nbt.putIntArray("data", new int[] {1, 2, 3});
				nbt.putByteArray("bytes", new byte[] {4, 5});
				nbt.putLongArray("longs", new long[] {6L});
				block.put("nbt", nbt);
			}
			blocks.add(block);
		}
		root.put("blocks", blocks);
		NbtList entities = new NbtList();
		for (int i = random.nextInt(4); i > 0; i--) {
			NbtCompound entity = new NbtCompound();
			NbtList pos = new NbtList();
			pos.add(NbtDouble.of(random.nextDouble()));
			pos.add(NbtDouble.of(random.nextDouble()));
			pos.add(NbtDouble.of(random.nextDouble()));
			entity.put("pos", pos);
			entity.put("blockPos", ints(1, 2, 3));
			NbtCompound nbt = new NbtCompound();
			nbt.putString("id", "minecraft:chest_minecart");
			nbt.putString("LootTable", "mod:chests/cart_" + random.nextInt(3));
			entity.put("nbt", nbt);
			entities.add(entity);
		}
		root.put("entities", entities);
		return root;
	}

	@Test
	void findsWhatTheFullReadFindsInTheSameOrder() throws IOException {
		Random random = new Random(1);
		int withLoot = 0;
		for (int i = 0; i < 200; i++) {
			NbtCompound root = template(random);
			byte[] file = compress(root);
			String expected = full(file);
			assertNotEquals("FAIL", expected);
			assertEquals(expected, partial(file));
			// the compounds themselves are the same, keys in the same order
			List<NbtCompound> read = TemplateNbt.read(new ByteArrayInputStream(file));
			List<NbtCompound> fromFull = TemplateNbt.containerNbt(root);
			assertEquals(fromFull, read);
			for (int j = 0; j < read.size(); j++) {
				assertEquals(List.copyOf(fromFull.get(j).getKeys()), List.copyOf(read.get(j).getKeys()));
			}
			if (!expected.equals("[] []")) {
				withLoot++;
			}
		}
		assertNotEquals(0, withLoot);
	}

	@Test
	void anEmptyTemplateHasNothing() throws IOException {
		byte[] file = compress(new NbtCompound());
		assertEquals("[] []", full(file));
		assertEquals("[] []", partial(file));
	}

	// Hand written files for what the game never writes but a broken pack might contain

	private interface Body {
		void write(DataOutputStream out) throws IOException;
	}

	private static byte[] file(Body body) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
			body.write(out);
		}
		return bytes.toByteArray();
	}

	private static void key(DataOutputStream out, int type, String key) throws IOException {
		out.writeByte(type);
		out.writeUTF(key);
	}

	private static void chestNbt(DataOutputStream out, String table) throws IOException {
		key(out, NbtElement.COMPOUND_TYPE, "nbt");
		key(out, NbtElement.STRING_TYPE, "LootTable");
		out.writeUTF(table);
		out.writeByte(0);
	}

	/** A root compound with the given entries and its end. */
	private static byte[] root(Body entries) throws IOException {
		return file(out -> {
			key(out, NbtElement.COMPOUND_TYPE, "");
			entries.write(out);
			out.writeByte(0);
		});
	}

	/** A "blocks" list of {@code count} compound elements written by {@code element}. */
	private static Body blocks(String name, int count, Body element) {
		return out -> {
			key(out, NbtElement.LIST_TYPE, name);
			out.writeByte(NbtElement.COMPOUND_TYPE);
			out.writeInt(count);
			for (int i = 0; i < count; i++) {
				element.write(out);
				out.writeByte(0);
			}
		};
	}

	private static void assertSame(byte[] file, String expected) {
		assertEquals(expected, full(file));
		assertEquals(expected, partial(file));
	}

	@Test
	void entitiesWrittenBeforeBlocksStillComeAfterThem() throws IOException {
		assertSame(root(out -> {
			blocks("entities", 1, o -> chestNbt(o, "a:chests/cart")).write(out);
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/block")).write(out);
		}), "[a:chests/block, a:chests/cart] []");
	}

	@Test
	void theLastOfARepeatedKeyCounts() throws IOException {
		// two "nbt" compounds in one block: the second one
		assertSame(root(blocks("blocks", 1, out -> {
			chestNbt(out, "a:chests/first");
			chestNbt(out, "a:chests/second");
		})), "[a:chests/second] []");
		// a compound "nbt" replaced by a string "nbt": none
		assertSame(root(blocks("blocks", 1, out -> {
			chestNbt(out, "a:chests/first");
			key(out, NbtElement.STRING_TYPE, "nbt");
			out.writeUTF("text");
		})), "[] []");
		// two "blocks" lists: the second one
		assertSame(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/old")).write(out);
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/new")).write(out);
		}), "[a:chests/new] []");
		// a "blocks" list replaced by an int: none
		assertSame(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/old")).write(out);
			key(out, NbtElement.INT_TYPE, "blocks");
			out.writeInt(3);
		}), "[] []");
	}

	@Test
	void blocksThatAreNoCompoundListHaveNothing() throws IOException {
		assertSame(root(out -> {
			key(out, NbtElement.LIST_TYPE, "blocks");
			out.writeByte(NbtElement.INT_TYPE);
			out.writeInt(2);
			out.writeInt(1);
			out.writeInt(2);
		}), "[] []");
	}

	@Test
	void anOddlyEncodedKeyIsStillThatKey() throws IOException {
		// "nbt" with the n written as two bytes, which readUTF accepts
		assertSame(root(blocks("blocks", 1, out -> {
			out.writeByte(NbtElement.COMPOUND_TYPE);
			out.writeShort(4);
			out.write(new byte[] {(byte) 0xC1, (byte) 0xAE, 'b', 't'});
			key(out, NbtElement.STRING_TYPE, "LootTable");
			out.writeUTF("a:chests/odd");
			out.writeByte(0);
		})), "[a:chests/odd] []");
	}

	@Test
	void brokenFilesFailBothWays() throws IOException {
		List<byte[]> broken = new ArrayList<>();
		// the root is no compound
		broken.add(file(out -> {
			key(out, NbtElement.STRING_TYPE, "");
			out.writeUTF("text");
		}));
		// cut off in the middle
		byte[] whole = root(blocks("blocks", 2, o -> chestNbt(o, "a:chests/x")));
		byte[] raw = new byte[0];
		try (var in = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(whole))) {
			raw = in.readAllBytes();
		}
		byte[] cut = java.util.Arrays.copyOf(raw, raw.length - 6);
		broken.add(file(out -> out.write(cut)));
		// a list without element type in the skipped palette
		broken.add(root(out -> {
			key(out, NbtElement.LIST_TYPE, "palette");
			out.writeByte(0);
			out.writeInt(2);
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
		}));
		// an unknown tag type in a skipped place
		broken.add(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
			key(out, 42, "strange");
		}));
		// a negative array length
		broken.add(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
			key(out, NbtElement.INT_ARRAY_TYPE, "data");
			out.writeInt(-1);
		}));
		// a string that is no modified UTF-8 in a skipped place
		broken.add(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
			key(out, NbtElement.STRING_TYPE, "name");
			out.writeShort(2);
			out.write(new byte[] {(byte) 0xE0, 'x'});
		}));
		// deeper than 512 in a skipped place
		broken.add(root(out -> {
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
			key(out, NbtElement.LIST_TYPE, "deep");
			for (int i = 0; i < 600; i++) {
				out.writeByte(NbtElement.LIST_TYPE);
				out.writeInt(1);
			}
			out.writeByte(NbtElement.INT_TYPE);
			out.writeInt(0);
		}));
		for (int i = 0; i < broken.size(); i++) {
			assertEquals("FAIL", full(broken.get(i)), "case " + i);
			assertEquals("FAIL", partial(broken.get(i)), "case " + i);
		}
	}

	@Test
	void theGzipTrailerAfterTheDataIsNotLookedAt() throws IOException {
		byte[] file = compress(template(new Random(5)));
		// a wrong checksum and size: the full read stops at the end of the data and never notices
		for (int i = file.length - 8; i < file.length; i++) {
			file[i] ^= 0x5A;
		}
		String expected = full(file);
		assertNotEquals("FAIL", expected);
		assertEquals(expected, partial(file));
	}

	@Test
	void largeTemplatesGoThroughTheWindow() throws IOException {
		// far more than the 64 KiB window, with long keys and strings and big arrays in between
		NbtCompound root = template(new Random(9));
		NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < 20_000; i++) {
			NbtCompound block = new NbtCompound();
			block.put("pos", ints(i, i, i));
			block.putInt("state", 1);
			block.putString("k".repeat(1 + i % 300), "v".repeat(i % 2000));
			if (i % 997 == 0) {
				NbtCompound nbt = new NbtCompound();
				nbt.putString("LootTable", "mod:chests/large_" + i);
				nbt.putIntArray("big", new int[70_000]);
				nbt.putString("text", "ä中".repeat(5_000));
				block.put("nbt", nbt);
			}
			blocks.add(block);
		}
		byte[] file = compress(root);
		String expected = full(file);
		assertNotEquals("FAIL", expected);
		assertEquals(expected, partial(file));
		assertEquals(TemplateNbt.containerNbt(root), TemplateNbt.read(new ByteArrayInputStream(file)));
	}

	/** One value of every tag type, with non-ASCII text, long arrays, nested and empty lists. */
	private static void everyType(NbtCompound nbt, String suffix) {
		nbt.putByte("byte" + suffix, (byte) -3);
		nbt.putShort("short" + suffix, (short) -300);
		nbt.putInt("int" + suffix, -70_000);
		nbt.putLong("long" + suffix, Long.MIN_VALUE);
		nbt.putFloat("float" + suffix, 1.5f);
		nbt.putDouble("double" + suffix, -2.25);
		nbt.putByteArray("bytes" + suffix, new byte[] {1, -2, 3});
		nbt.putIntArray("ints" + suffix, new int[] {Integer.MAX_VALUE, -1});
		nbt.putLongArray("longs" + suffix, new long[] {Long.MAX_VALUE, -1L, 0L});
		nbt.putString("text" + suffix, "Grüße 中 \u0000 😀");
		nbt.putString("schlüssel_ä" + suffix, "x");
		nbt.put("empty" + suffix, new NbtList());
		NbtList lists = new NbtList();
		lists.add(ints(1, 2));
		lists.add(ints(3));
		nbt.put("lists" + suffix, lists);
		NbtList longs = new NbtList();
		longs.add(net.minecraft.nbt.NbtLong.of(7L));
		nbt.put("longList" + suffix, longs);
		NbtCompound inner = new NbtCompound();
		inner.putString("LootTable", "mod:chests/inner" + suffix);
		NbtList compounds = new NbtList();
		compounds.add(inner);
		nbt.put("compounds" + suffix, compounds);
	}

	@Test
	void everyTagTypeIsSkippedAndReadLikeTheFullRead() throws IOException {
		NbtCompound root = template(new Random(3));
		everyType(root, "_root");
		NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < blocks.size(); i += 7) {
			NbtCompound block = blocks.getCompound(i);
			everyType(block, "_block");
			NbtCompound nbt = block.contains("nbt", NbtElement.COMPOUND_TYPE) ? block.getCompound("nbt") : new NbtCompound();
			everyType(nbt, "_" + i);
			block.put("nbt", nbt);
		}
		byte[] file = compress(root);
		String expected = full(file);
		assertNotEquals("FAIL", expected);
		assertEquals(expected, partial(file));
		assertEquals(TemplateNbt.containerNbt(root), TemplateNbt.read(new ByteArrayInputStream(file)));
	}

	@Test
	void oddListHeadersInSkippedPlacesAreJudgedLikeTheFullRead() throws IOException {
		// an empty list of an unknown element type is fine, a non-empty one and a negative length are not
		assertSame(root(out -> {
			key(out, NbtElement.LIST_TYPE, "palette");
			out.writeByte(42);
			out.writeInt(0);
			blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
		}), "[a:chests/x] []");
		for (int[] header : new int[][] {{42, 1}, {NbtElement.INT_TYPE, -1}, {0, -1}}) {
			byte[] file = root(out -> {
				blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
				key(out, NbtElement.LIST_TYPE, "palette");
				out.writeByte(header[0]);
				out.writeInt(header[1]);
				out.writeInt(0);
			});
			assertSame(file, "FAIL");
		}
		// "blocks" itself: an empty list of an unknown type has nothing, a negative length fails
		assertSame(root(out -> {
			key(out, NbtElement.LIST_TYPE, "blocks");
			out.writeByte(42);
			out.writeInt(0);
		}), "[] []");
		assertSame(root(out -> {
			key(out, NbtElement.LIST_TYPE, "blocks");
			out.writeByte(NbtElement.COMPOUND_TYPE);
			out.writeInt(-5);
		}), "FAIL");
	}

	@Test
	void theDepthLimitIsWhereTheFullReadHasIt() throws IOException {
		// nested lists in a skipped place and inside a block's "nbt": both readers agree at every depth around 512
		for (boolean insideNbt : new boolean[] {false, true}) {
			boolean passed = false;
			boolean failed = false;
			for (int depth = 500; depth <= 515; depth++) {
				int levels = depth;
				Body deep = out -> {
					key(out, NbtElement.LIST_TYPE, "deep");
					for (int i = 0; i < levels; i++) {
						out.writeByte(NbtElement.LIST_TYPE);
						out.writeInt(1);
					}
					out.writeByte(NbtElement.INT_TYPE);
					out.writeInt(0);
				};
				byte[] file = insideNbt
					? root(blocks("blocks", 1, out -> {
						key(out, NbtElement.COMPOUND_TYPE, "nbt");
						key(out, NbtElement.STRING_TYPE, "LootTable");
						out.writeUTF("a:chests/x");
						deep.write(out);
						out.writeByte(0);
					}))
					: root(out -> {
						blocks("blocks", 1, o -> chestNbt(o, "a:chests/x")).write(out);
						deep.write(out);
					});
				String expected = full(file);
				assertEquals(expected, partial(file), "depth " + depth + (insideNbt ? " in nbt" : ""));
				passed |= !expected.equals("FAIL");
				failed |= expected.equals("FAIL");
			}
			assertTrue(passed && failed, "the limit was not between 500 and 515");
		}
	}

	@Test
	void aFileThatIsNotGzipFailsBothWays() throws IOException {
		NbtCompound root = template(new Random(4));
		ByteArrayOutputStream plain = new ByteArrayOutputStream();
		NbtIo.writeCompound(root, new DataOutputStream(plain));
		assertSame(plain.toByteArray(), "FAIL");
		assertSame(new byte[0], "FAIL");
		assertSame(new byte[] {0x1F}, "FAIL");
		assertSame(file(out -> { }), "FAIL");
	}

	@Test
	void aFileCutOffAnywhereFailsBothWays() throws IOException {
		byte[] whole = compress(template(new Random(6)));
		byte[] raw;
		try (var in = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(whole))) {
			raw = in.readAllBytes();
		}
		Random random = new Random(6);
		for (int i = 0; i < 150; i++) {
			int length = i < 20 ? i : random.nextInt(raw.length);
			byte[] cut = java.util.Arrays.copyOf(raw, length);
			byte[] file = file(out -> out.write(cut));
			assertEquals("FAIL", full(file), "cut at " + length);
			assertEquals("FAIL", partial(file), "cut at " + length);
		}
	}

	@Test
	void deepButAllowedDataIsRead() throws IOException {
		// 500 levels below the block's "nbt" is still inside the limit of 512
		assertSame(root(blocks("blocks", 1, out -> {
			chestNbt(out, "a:chests/x");
			key(out, NbtElement.LIST_TYPE, "deep");
			for (int i = 0; i < 500; i++) {
				out.writeByte(NbtElement.LIST_TYPE);
				out.writeInt(1);
			}
			out.writeByte(NbtElement.INT_TYPE);
			out.writeInt(0);
		})), "[a:chests/x] []");
	}
}
