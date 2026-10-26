package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.EMILoot;
import net.minecraft.util.Identifier;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** TEMP-XPAR: temporary measurement dump, removed before finishing. */
final class TempDump {
	static final Map<Identifier, Long> WALKS = new ConcurrentHashMap<>();
	static final java.util.concurrent.atomic.AtomicLong RAW = new java.util.concurrent.atomic.AtomicLong();

	private static String info(StructureLookup.TemplateInfo info) {
		return List.copyOf(info.lootTables) + " | " + List.copyOf(info.pools);
	}

	/** Every template: full read (old) vs TemplateNbt (new), ordered TemplateInfo equality, and timings. */
	static void phases(net.minecraft.resource.ResourceManager resources, List<Identifier> ids) {
		for (int round = 0; round < 2; round++) {
			long tOld = 0, tNew = 0;
			int n = 0, same = 0, differ = 0, bothFail = 0, nonEmpty = 0;
			for (Identifier id : ids) {
				Identifier file = Identifier.of(id.getNamespace(), "structure/" + id.getPath() + ".nbt");
				java.util.Optional<net.minecraft.resource.Resource> res = resources.getResource(file);
				if (res.isEmpty()) continue;
				byte[] raw;
				try (java.io.InputStream in = res.get().getInputStream()) {
					raw = in.readAllBytes();
				} catch (Exception e) {
					continue;
				}
				n++;
				String oldResult, newResult;
				long a = System.nanoTime();
				try {
					oldResult = info(StructureLookup.TemplateInfo.of(net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(raw), net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes())));
				} catch (Throwable e) {
					oldResult = "FAIL";
				}
				long b = System.nanoTime();
				try {
					newResult = info(StructureLookup.TemplateInfo.of(TemplateNbt.read(new java.io.ByteArrayInputStream(raw))));
				} catch (Throwable e) {
					newResult = "FAIL";
				}
				long c = System.nanoTime();
				tOld += b - a;
				tNew += c - b;
				if (oldResult.equals(newResult)) {
					same++;
					if (oldResult.equals("FAIL")) bothFail++;
					else if (!oldResult.equals("[] | []")) nonEmpty++;
				} else {
					differ++;
					if (differ <= 5) EMILoot.LOGGER.warn("TEMP-XPAR template {} differs: old {} new {}", id, oldResult, newResult);
				}
			}
			EMILoot.LOGGER.info("TEMP-XPAR template reader round {}: {} templates, identical {} (non-empty {}, both failing {}), different {}; full read + of {}ms, TemplateNbt + of {}ms",
				round, n, same, nonEmpty, bothFail, differ, tOld / 1_000_000, tNew / 1_000_000);
		}
	}

	static void walk(Identifier id, long nanos) {
		WALKS.put(id, nanos);
	}

	static void dump(StructureLookup lookup, List<Identifier> chestTables, Map<Identifier, List<Identifier>> dims, Map<Identifier, Set<Identifier>> byTable) {
		List<Map.Entry<Identifier, Long>> slow = new ArrayList<>(WALKS.entrySet());
		slow.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(10, slow.size()); i++) {
			sb.append(slow.get(i).getKey()).append('=').append(slow.get(i).getValue() / 1_000_000).append("ms ");
		}
		EMILoot.LOGGER.info("TEMP-XPAR slowest walks: {} raw-read {}ms prio {} preread {}", sb, RAW.getAndSet(0) / 1_000_000, Integer.getInteger("emi.xpar.prio", -1), Boolean.getBoolean("emi.xpar.preread"));
		WALKS.clear();
		String file = System.getProperty("emi.xpar.dump", net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment() ? "xpar-dump.txt" : null);
		if (file == null) {
			return;
		}
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(file)))) {
			w.println("structures " + dims.size());
			dims.forEach((k, v) -> w.println("dim " + k + " " + v));
			List<Identifier> keys = new ArrayList<>(byTable.keySet());
			keys.sort(null);
			for (Identifier k : keys) {
				w.println("raw " + k + " -> " + byTable.get(k));
			}
			for (Identifier table : chestTables) {
				w.println("chest structures " + table + " -> " + lookup.structuresOf(table).stream().map(r -> r.structure() + " " + r.dimensions()).toList());
			}
		} catch (Exception e) {
			EMILoot.LOGGER.error("TEMP-XPAR dump", e);
		}
	}
}
