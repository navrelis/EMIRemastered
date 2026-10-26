package dev.emi.remastered.loot.structure;

import dev.emi.remastered.loot.mixin.DirectoryResourcePackAccessor;
import dev.emi.remastered.loot.mixin.JigsawStructureAccessor;
import dev.emi.remastered.loot.mixin.ListPoolElementAccessor;
import dev.emi.remastered.loot.mixin.OverlayResourcePackAccessor;
import dev.emi.remastered.loot.mixin.SinglePoolElementAccessor;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ZipResourcePack;
import net.minecraft.structure.pool.ListPoolElement;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Feeds everything the cached part of the structure lookup depends on into a {@link LookupFingerprint}. Runs on the
 * lookup's build thread and reads the same frozen registries and resource packs as the lookup's walks.
 * <p>
 * The cache holds only the walks of the jigsaw structures (which loot tables the templates reachable from a
 * structure's start pool name, {@link StructureLookup}), the part that reads thousands of template files. Everything
 * else (dimensions, chests placed in code, tables reached through other tables) is cheap and computed again from the
 * live data on every build, so loot tables, biome tags, dimensions and the list of chest tables are not inputs. A walk
 * depends on, and the fingerprint covers:
 * <ul>
 * <li>the code that walks and reads templates: the cache format version, and every loaded mod's id, version and origin
 * (path, size and last-modified time of its jar; a directory origin, as in a development environment, with every file
 * below it; a nested jar by its parent mod and its location in it). This also covers the files of the vanilla pack
 * (the game jar is the {@code minecraft} mod's origin) and of the mods' own packs;</li>
 * <li>the structures, sorted by id (the registry's own order differs from run to run): id, structure type, class, and
 * for a jigsaw structure its start pool;</li>
 * <li>the template pools, sorted by id: fallback, and every element in list order with its class, template id and
 * nested elements;</li>
 * <li>the template files: the server's resource packs in their order (id and class), each identified by its files.
 * Fabric's mod packs by their mod, vanilla by the game jar, a zip pack by its file, a directory pack by its
 * {@code pack.mcmeta} and every file below its {@code data/<namespace>/structure} folders, an overlay pack by its parts.
 * Any other pack (generated in memory by a mod, or of a class this does not know) by content: the ids and bytes of
 * every file it lists under {@code structure/}; a pack that cannot list them makes the fingerprint uncacheable.</li>
 * </ul>
 * Accepted residual risks: a file replaced or edited in place keeping its size and last-modified time (to the
 * millisecond); a pack of an unknown class that serves templates it does not list; a mod that changes which template a
 * resource id gives from outside any pack (from a config folder through a mixin into the resource manager) without its
 * own jar changing. A file modified after the game started makes the fingerprint uncacheable ({@link LookupFingerprint}),
 * so a jar or pack replaced while the game runs never stores a result of its old content under its new identity.
 */
final class LookupCacheInputs {
	static final String MOD_PACK_CLASS = "net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack";

	/** How long each part took, and the packs with templates that were identified by their content. */
	record Timings(long modsMillis, long packsMillis, long registriesMillis, List<String> contentPacks) {
	}

	private final LookupFingerprint fingerprint;
	private final Map<String, Boolean> knownMods = new HashMap<>();
	private final List<String> contentPacks = new ArrayList<>();

	LookupCacheInputs(LookupFingerprint fingerprint) {
		this.fingerprint = fingerprint;
	}

	/** Feeds all inputs, in a fixed order. */
	Timings feed(int formatVersion, Registry<Structure> structures, Registry<StructurePool> pools, ResourceManager resources) throws IOException {
		fingerprint.format(formatVersion);
		long start = System.nanoTime();
		mods();
		long modsDone = System.nanoTime();
		for (ResourcePack pack : resources.streamResourcePacks().toList()) {
			pack(pack, 0);
		}
		long packsDone = System.nanoTime();
		structures(structures);
		pools(pools);
		long end = System.nanoTime();
		return new Timings((modsDone - start) / 1_000_000, (packsDone - modsDone) / 1_000_000, (end - packsDone) / 1_000_000, List.copyOf(contentPacks));
	}

	private void mods() throws IOException {
		List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
		mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));
		for (ModContainer mod : mods) {
			ModOrigin origin = mod.getOrigin();
			ModOrigin.Kind kind = origin.getKind();
			fingerprint.mod(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString(), kind.name(),
				kind == ModOrigin.Kind.PATH ? origin.getPaths() : List.of(),
				kind == ModOrigin.Kind.NESTED ? origin.getParentModId() : null,
				kind == ModOrigin.Kind.NESTED ? origin.getParentSubLocation() : null);
		}
	}

	/** Whether a mod's files are in the fingerprint: its own paths, or the jar it is nested in, all the way up. */
	private boolean known(String modId) {
		Boolean known = knownMods.get(modId);
		if (known == null) {
			knownMods.put(modId, false);
			known = FabricLoader.getInstance().getModContainer(modId).map(mod -> switch (mod.getOrigin().getKind()) {
				case PATH -> true;
				case NESTED -> known(mod.getOrigin().getParentModId());
				default -> false;
			}).orElse(false);
			knownMods.put(modId, known);
		}
		return known;
	}

	private void pack(ResourcePack pack, int depth) throws IOException {
		String id = pack.getId();
		String className = pack.getClass().getName();
		if (pack instanceof ModResourcePack mod && className.equals(MOD_PACK_CLASS) && known(mod.getFabricModMetadata().getId())) {
			fingerprint.modPack(depth, id, className, mod.getFabricModMetadata().getId());
		} else if (pack.getClass() == DefaultResourcePack.class && known("minecraft")) {
			fingerprint.vanillaPack(depth, id, className);
		} else if (pack instanceof ZipResourcePack && ((ZipPackFile) pack).emi_loot$getFile() != null) {
			File file = ((ZipPackFile) pack).emi_loot$getFile();
			fingerprint.zipPack(depth, id, className, file.toPath());
		} else if (pack instanceof DirectoryResourcePack) {
			fingerprint.directoryPack(depth, id, className, ((DirectoryResourcePackAccessor) pack).emi_loot$getRoot());
		} else if (pack instanceof OverlayResourcePack) {
			List<ResourcePack> parts = ((OverlayResourcePackAccessor) pack).emi_loot$getOverlaysAndBase();
			fingerprint.overlayPack(depth, id, className, parts.size());
			for (ResourcePack part : parts) {
				pack(part, depth + 1);
			}
		} else {
			content(pack, depth, id, className);
		}
	}

	/** A pack whose files are not known: the ids and bytes of every structure file it lists. */
	private void content(ResourcePack pack, int depth, String id, String className) {
		SortedMap<String, byte[]> files = new TreeMap<>();
		try {
			Map<Identifier, InputSupplier<InputStream>> listed = new TreeMap<>();
			for (String namespace : pack.getNamespaces(ResourceType.SERVER_DATA)) {
				pack.findResources(ResourceType.SERVER_DATA, namespace, "structure", listed::put);
			}
			for (Map.Entry<Identifier, InputSupplier<InputStream>> file : listed.entrySet()) {
				try (InputStream in = file.getValue().get()) {
					files.put(file.getKey().toString(), in.readAllBytes());
				}
			}
		} catch (Exception e) {
			fingerprint.unidentifiedPack(depth, id, className, e.toString());
			return;
		}
		fingerprint.contentPack(depth, id, className, files);
		if (!files.isEmpty()) {
			contentPacks.add(id + " (" + files.size() + " templates)");
		}
	}

	private void structures(Registry<Structure> registry) {
		List<Map.Entry<RegistryKey<Structure>, Structure>> entries = new ArrayList<>(registry.getEntrySet());
		entries.sort(Comparator.comparing(entry -> entry.getKey().getValue()));
		for (Map.Entry<RegistryKey<Structure>, Structure> entry : entries) {
			Structure structure = entry.getValue();
			String startPool = null;
			if (structure instanceof JigsawStructure jigsaw) {
				startPool = ((JigsawStructureAccessor) (Object) jigsaw).emi_loot$getStartPool().getKey().map(key -> key.getValue().toString()).orElse("direct");
			}
			fingerprint.structure(entry.getKey().getValue().toString(), String.valueOf(Registries.STRUCTURE_TYPE.getId(structure.getType())),
				structure.getClass().getName(), startPool);
		}
	}

	/**
	 * Every pool with its fallback and its elements in list order. The list is private; a shuffle with a fresh random
	 * of a fixed seed is a fixed permutation for a list of a given size, so its result stands for the list exactly.
	 */
	private void pools(Registry<StructurePool> registry) {
		List<Map.Entry<RegistryKey<StructurePool>, StructurePool>> entries = new ArrayList<>(registry.getEntrySet());
		entries.sort(Comparator.comparing(entry -> entry.getKey().getValue()));
		Map<StructurePoolElement, String> described = new IdentityHashMap<>();
		for (Map.Entry<RegistryKey<StructurePool>, StructurePool> entry : entries) {
			StructurePool pool = entry.getValue();
			List<String> elements = new ArrayList<>();
			for (StructurePoolElement element : pool.getElementIndicesInRandomOrder(Random.create(0))) {
				elements.add(described.computeIfAbsent(element, LookupCacheInputs::describe));
			}
			fingerprint.pool(entry.getKey().getValue().toString(), pool.getFallback().getKey().map(key -> key.getValue().toString()).orElse("direct"), elements);
		}
	}

	private static String describe(StructurePoolElement element) {
		StringBuilder text = new StringBuilder(element.getClass().getName());
		if (element instanceof SinglePoolElement single) {
			text.append(' ').append(((SinglePoolElementAccessor) single).emi_loot$getLocation().left().map(Identifier::toString).orElse("inline"));
		} else if (element instanceof ListPoolElement list) {
			text.append(" [");
			for (StructurePoolElement nested : ((ListPoolElementAccessor) list).emi_loot$getElements()) {
				text.append(describe(nested)).append(", ");
			}
			text.append(']');
		}
		return text.toString();
	}
}
