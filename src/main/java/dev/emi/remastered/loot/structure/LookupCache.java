package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.EMILoot;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The structure lookup cache (setting {@code loot.structure-cache}): one file in the game directory,
 * {@code emi_remastered/cache/structure-lookup.dat}, shared by every world and server of this game directory and valid
 * while its fingerprint matches ({@link LookupCacheInputs} says what that covers). Everything here runs on the lookup's
 * build thread; the server thread never waits for the file. A start whose fingerprint differs walks the structures as
 * usual and replaces the file; a start with an uncacheable fingerprint neither reads nor writes it.
 */
final class LookupCache {
	private static volatile @Nullable String lastUncacheable;
	private static volatile @Nullable List<String> lastContentPacks;

	/** The fingerprint of this build and, on a hit, the walks read from the file. */
	record Prepared(@Nullable byte[] fingerprint, @Nullable Map<String, List<String>> walks, String timings) {
		boolean writable() {
			return fingerprint != null;
		}
	}

	private LookupCache() {
	}

	static Path file() {
		return FabricLoader.getInstance().getGameDir().resolve("emi_remastered").resolve("cache").resolve("structure-lookup.dat");
	}

	/** Computes the fingerprint and reads the file. Never throws; a problem means the structures are walked. */
	static Prepared prepare(Registry<Structure> structures, Registry<StructurePool> pools, ResourceManager resources) {
		long start = System.nanoTime();
		LookupFingerprint fingerprint = new LookupFingerprint(ManagementFactory.getRuntimeMXBean().getStartTime());
		LookupCacheInputs.Timings timings;
		byte[] hash;
		try {
			timings = new LookupCacheInputs(fingerprint).feed(StructureCache.FORMAT, structures, pools, resources);
			hash = fingerprint.finish();
		} catch (Exception e) {
			EMILoot.LOGGER.warn("Could not compute the fingerprint of the structure lookup cache, the cache is not used: {}", e.toString());
			return new Prepared(null, null, "");
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		String timing = "fingerprint " + millis + "ms: mods " + timings.modsMillis() + "ms, packs " + timings.packsMillis() + "ms, structures and pools "
			+ timings.registriesMillis() + "ms";
		if (!timings.contentPacks().isEmpty() && !timings.contentPacks().equals(lastContentPacks)) {
			lastContentPacks = timings.contentPacks();
			EMILoot.LOGGER.info("Structure lookup cache: data packs identified by the content of their structure templates: {}", timings.contentPacks());
		}
		String uncacheable = fingerprint.uncacheableReason();
		if (uncacheable != null) {
			if (!Objects.equals(uncacheable, lastUncacheable)) {
				lastUncacheable = uncacheable;
				EMILoot.LOGGER.info("Structure lookup cache not used: {}", uncacheable);
			}
			return new Prepared(null, null, timing);
		}
		StructureCache.Loaded loaded = StructureCache.read(file(), hash);
		if (loaded.ioError()) {
			EMILoot.LOGGER.warn("Structure lookup cache: {}", loaded.reason());
		} else if (!loaded.hit()) {
			EMILoot.LOGGER.info("Structure lookup cache: {}, looking the structures up ({})", loaded.reason(), timing);
		}
		return new Prepared(hash, loaded.walks(), timing);
	}

	/** Replaces the file with the walks of a complete build. Never throws. */
	static void write(Prepared prepared, Map<String, List<String>> walks) {
		if (!prepared.writable() || walks.isEmpty()) {
			return;
		}
		long start = System.nanoTime();
		Path file = file().toAbsolutePath().normalize();
		try {
			StructureCache.write(file, prepared.fingerprint(), walks);
			EMILoot.LOGGER.info("Saved the structures of chest loot tables to the cache {} ({} bytes) in {}ms", file, Files.size(file), (System.nanoTime() - start) / 1_000_000);
		} catch (Exception e) {
			EMILoot.LOGGER.warn("Could not save the structure lookup cache {}: {}", file, e.toString());
		}
	}
}
