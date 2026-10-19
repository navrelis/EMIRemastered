package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.tools.tags.TagCatalog;
import dev.emi.remastered.tools.tags.TagEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

/**
 * Index of all tags of the client world's registries (static registries and the data-driven ones synced from the
 * server). Built lazily the first time the tag browser opens and dropped whenever the client receives tags or leaves
 * the world, so it is built once per world join / data pack reload.
 */
public final class TagIndex {
	private static TagCatalog catalog;
	private static DynamicRegistryManager builtFor;

	static {
		CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
			if (client) {
				invalidate();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> invalidate());
	}

	private TagIndex() {
	}

	public static synchronized void invalidate() {
		catalog = null;
		builtFor = null;
	}

	/** The tag catalog of the current world, or null when the client is not in a world. */
	public static synchronized @Nullable TagCatalog get() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return null;
		}
		DynamicRegistryManager manager = client.world.getRegistryManager();
		if (catalog == null || builtFor != manager) {
			long start = System.nanoTime();
			catalog = build(manager);
			builtFor = manager;
			EmiRemastered.LOGGER.info("[Tools] Indexed {} tags in {} registries in {} ms", catalog.entries().size(),
				catalog.registries().size(), (System.nanoTime() - start) / 1_000_000);
		}
		return catalog;
	}

	private static TagCatalog build(DynamicRegistryManager manager) {
		List<TagEntry> entries = new ArrayList<>();
		manager.streamAllRegistries().forEach(entry -> addRegistry(entries, entry.value()));
		return new TagCatalog(entries);
	}

	private static <T> void addRegistry(List<TagEntry> out, Registry<T> registry) {
		String registryId = registry.getKey().getValue().toString();
		registry.streamTagsAndEntries().forEach(pair -> {
			TagKey<T> tag = pair.getFirst();
			RegistryEntryList.Named<T> list = pair.getSecond();
			List<String> members = new ArrayList<>(list.size());
			for (RegistryEntry<T> member : list) {
				members.add(member.getKey().map(key -> key.getValue().toString()).orElse("<inline>"));
			}
			out.add(new TagEntry(registryId, tag.id().toString(), members));
		});
	}
}
