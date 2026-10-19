package dev.emi.remastered.info.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.emi.remastered.info.core.ProviderOrdering;
import net.minecraft.util.Identifier;

/**
 * Registry of {@link ItemInfoProvider}s.
 * <p>
 * Call {@link #register} from a module's {@code initClient()} (once) or from an EMI plugin's {@code register}
 * (every EMI reload; the same id simply replaces the previous instance). All methods are thread safe: EMI plugins run
 * on EMI's reload thread. Every change invalidates the cached pages of the Item Info screen.
 */
public final class ItemInfoRegistry {
	private static final Map<Identifier, ItemInfoProvider> PROVIDERS = new LinkedHashMap<>();
	private static volatile List<ItemInfoProvider> sorted = List.of();
	private static volatile int version = 0;

	private ItemInfoRegistry() {
	}

	/** Adds {@code provider}, replacing a provider with the same id. */
	public static void register(ItemInfoProvider provider) {
		Objects.requireNonNull(provider, "provider");
		Identifier id = Objects.requireNonNull(provider.getId(), "provider id");
		synchronized (PROVIDERS) {
			PROVIDERS.put(id, provider);
			update();
		}
	}

	/** Removes the provider with this id; returns whether there was one. */
	public static boolean unregister(Identifier id) {
		synchronized (PROVIDERS) {
			if (PROVIDERS.remove(id) == null) {
				return false;
			}
			update();
			return true;
		}
	}

	/** All providers, sorted by {@link ItemInfoProvider#getOrder()} and then by id. Immutable snapshot. */
	public static List<ItemInfoProvider> getProviders() {
		return sorted;
	}

	/** Counter that changes with every registration change (used to invalidate cached pages). */
	public static int getVersion() {
		return version;
	}

	private static void update() {
		sorted = List.copyOf(ProviderOrdering.sorted(new ArrayList<>(PROVIDERS.values()), ItemInfoProvider::getOrder,
			p -> p.getId().toString()));
		version++;
	}
}
