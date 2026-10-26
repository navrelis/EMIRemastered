package dev.emi.remastered.loot.structure;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Stream;

/**
 * The canonical fingerprint of everything the structure lookup cache depends on, as a SHA-256 hash, built from plain
 * data in a fixed order: format version, mods, packs, structures, pools ({@link LookupCacheInputs} feeds them and says
 * what is covered). Files are identified by path, size and last-modified time, a directory by those of every file below
 * it. Every record and field is written with its length, so different inputs never hash the same by shifting text from
 * one field to the next. Pure Java.
 * <p>
 * A fingerprint can be uncacheable: an input whose content cannot be identified, or a file that changed after the game
 * started (what the game has in memory may then be the old content while the file's identity is the new one, so a
 * result written now could be stored under a fingerprint it does not belong to). The first reason is kept; the cache
 * is then neither read nor written.
 */
final class LookupFingerprint {
	private final MessageDigest digest;
	private final long startedMillis;
	private @Nullable String uncacheable;
	private int records;

	/**
	 * @param startedMillis when the game started (epoch milliseconds); a file last modified at or after it makes the
	 * fingerprint uncacheable
	 */
	LookupFingerprint(long startedMillis) {
		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		this.startedMillis = startedMillis;
	}

	/** The cache's own format version, first. */
	LookupFingerprint format(int version) {
		return add("format", Integer.toString(version));
	}

	/**
	 * A loaded mod. {@code originKind} is the loader's origin kind: PATH with the mod's files ({@link #file}), NESTED
	 * with the parent mod and the location in it, anything else with neither.
	 */
	LookupFingerprint mod(String id, String version, String originKind, List<Path> paths, @Nullable String parentId, @Nullable String parentLocation) throws IOException {
		add("mod", id, version, originKind);
		for (Path path : paths) {
			file("mod-file", path);
		}
		if (parentId != null) {
			add("mod-nested", parentId, parentLocation);
		}
		return this;
	}

	/** Every pack starts with this record: its depth (parts of an overlay pack are one deeper), id and class. */
	private void pack(int depth, String id, String className) {
		add("pack", Integer.toString(depth), id, className);
	}

	/** A pack whose files are those of a mod, already in the fingerprint. */
	LookupFingerprint modPack(int depth, String id, String className, String modId) {
		pack(depth, id, className);
		return add("pack-mod", modId);
	}

	/** The vanilla pack, whose files are the game jar (the minecraft mod). */
	LookupFingerprint vanillaPack(int depth, String id, String className) {
		pack(depth, id, className);
		return add("pack-vanilla");
	}

	/** A zip pack: the identity of its file. */
	LookupFingerprint zipPack(int depth, String id, String className, Path file) throws IOException {
		pack(depth, id, className);
		return file("pack-zip", file);
	}

	/**
	 * A directory pack: its {@code pack.mcmeta} (filters and overlays) and every file below its
	 * {@code data/<namespace>/structure} folders, the only files of a pack the cached walks read.
	 */
	LookupFingerprint directoryPack(int depth, String id, String className, Path root) throws IOException {
		pack(depth, id, className);
		Path absolute = root.toAbsolutePath().normalize();
		add("pack-directory", absolute.toString());
		file("pack-meta", absolute.resolve("pack.mcmeta"));
		Path data = absolute.resolve("data");
		if (Files.isDirectory(data)) {
			List<Path> namespaces = new ArrayList<>();
			try (Stream<Path> list = Files.list(data)) {
				list.forEach(namespaces::add);
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
			namespaces.sort(Comparator.comparing(Path::toString));
			for (Path namespace : namespaces) {
				tree("pack-directory-file", absolute, namespace.resolve("structure"));
			}
		}
		return this;
	}

	/** A pack made of overlays and a base; its parts follow as packs one deeper. */
	LookupFingerprint overlayPack(int depth, String id, String className, int parts) {
		pack(depth, id, className);
		return add("pack-overlay", Integer.toString(parts));
	}

	/** A pack whose files are not known, by content: the ids and bytes of all its structure files. */
	LookupFingerprint contentPack(int depth, String id, String className, SortedMap<String, byte[]> files) {
		pack(depth, id, className);
		add("pack-content", Integer.toString(files.size()));
		for (Map.Entry<String, byte[]> file : files.entrySet()) {
			add("pack-content-file", file.getKey());
			writeInt(file.getValue().length);
			digest.update(file.getValue());
		}
		return this;
	}

	/** A pack whose structure files cannot be identified: nothing may be cached. */
	LookupFingerprint unidentifiedPack(int depth, String id, String className, String why) {
		pack(depth, id, className);
		uncacheable("the structure files of data pack " + id + " (" + className + ") cannot be identified: " + why);
		return this;
	}

	/** A structure: id, structure type, class, and for a jigsaw structure its start pool (else null). */
	LookupFingerprint structure(String id, String type, String className, @Nullable String startPool) {
		return add("structure", id, type, className, startPool);
	}

	/** A template pool: id, fallback, and its elements (class, template id, nested elements) in list order. */
	LookupFingerprint pool(String id, String fallback, List<String> elements) {
		add("pool", id, fallback, Integer.toString(elements.size()));
		for (String element : elements) {
			add("element", element);
		}
		return this;
	}

	/** Adds one record: a kind and its fields, in order. A null field is written as a marker distinct from any text. */
	private LookupFingerprint add(String kind, String... fields) {
		records++;
		field(kind);
		writeInt(fields.length);
		for (String field : fields) {
			if (field == null) {
				writeInt(-1);
			} else {
				field(field);
			}
		}
		return this;
	}

	/**
	 * Adds the identity of a file: absolute path, size, last-modified time. A directory adds every regular file below it
	 * ({@link #tree}). A path that does not exist is recorded as missing.
	 */
	private LookupFingerprint file(String kind, Path path) throws IOException {
		Path absolute = path.toAbsolutePath().normalize();
		BasicFileAttributes attributes;
		try {
			attributes = Files.readAttributes(absolute, BasicFileAttributes.class);
		} catch (NoSuchFileException e) {
			return add(kind, absolute.toString(), "missing");
		}
		if (attributes.isDirectory()) {
			add(kind, absolute.toString(), "directory");
			return tree(kind, absolute, absolute);
		}
		return add(kind, absolute.toString(), Long.toString(attributes.size()), Long.toString(modified(absolute, attributes)));
	}

	/**
	 * Adds every regular file below {@code dir}: its path relative to {@code base}, size and last-modified time, sorted
	 * by path. A directory that does not exist adds nothing. Symbolic links are followed.
	 */
	private LookupFingerprint tree(String kind, Path base, Path dir) throws IOException {
		if (!Files.isDirectory(dir)) {
			return this;
		}
		List<Map.Entry<String, Path>> files = new ArrayList<>();
		try (Stream<Path> walk = Files.walk(dir, FileVisitOption.FOLLOW_LINKS)) {
			walk.forEach(file -> {
				if (Files.isRegularFile(file)) {
					files.add(Map.entry(base.relativize(file).toString().replace('\\', '/'), file));
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		files.sort(Map.Entry.comparingByKey());
		for (Map.Entry<String, Path> file : files) {
			BasicFileAttributes attributes = Files.readAttributes(file.getValue(), BasicFileAttributes.class);
			add(kind, file.getKey(), Long.toString(attributes.size()), Long.toString(modified(file.getValue(), attributes)));
		}
		return this;
	}

	private long modified(Path file, BasicFileAttributes attributes) {
		long millis = attributes.lastModifiedTime().toMillis();
		if (millis >= startedMillis) {
			uncacheable(file + " changed after the game started");
		}
		return millis;
	}

	/** Marks the fingerprint as uncacheable; the first reason is kept. */
	void uncacheable(String reason) {
		if (uncacheable == null) {
			uncacheable = reason;
		}
	}

	/** Why the cache must not be used, or null. */
	@Nullable String uncacheableReason() {
		return uncacheable;
	}

	/** The SHA-256 hash of all records so far. Ends this fingerprint. */
	byte[] finish() {
		writeInt(records);
		return digest.digest();
	}

	private void field(String text) {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		writeInt(bytes.length);
		digest.update(bytes);
	}

	private void writeInt(int value) {
		digest.update((byte) (value >>> 24));
		digest.update((byte) (value >>> 16));
		digest.update((byte) (value >>> 8));
		digest.update((byte) value);
	}
}
