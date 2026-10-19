package dev.emi.remastered.info.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.config.ModuleConfigs;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.info.core.GenerationCache;
import dev.emi.remastered.info.core.ProviderOrdering;
import dev.emi.remastered.info.core.ReloadGeneration;
import dev.emi.remastered.info.providers.BuiltinProviders;
import dev.emi.remastered.module.Module;
import dev.emi.remastered.module.Modules;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Computes Item Info pages lazily (on open) and caches them per EMI reload. Client render thread only.
 */
public final class InfoService {
	private static final int CACHE_SIZE = 64;
	private static final GenerationCache<EmiStack, InfoPage> CACHE = new GenerationCache<>(CACHE_SIZE);
	private static final ReloadGeneration GENERATION = new ReloadGeneration();
	private static final Set<Identifier> FAILED = new HashSet<>();

	private InfoService() {
	}

	/** The page of {@code stack} (amount 1), from the cache when possible. */
	public static InfoPage getPage(EmiStack stack) {
		if (!EmiReloadManager.isLoaded()) {
			return InfoPage.loading(stack);
		}
		BuiltinProviders.ensureRegistered();
		long generation = GENERATION.update(EmiApi.getRecipeManager(), List.of(ItemInfoRegistry.getVersion(),
			ModuleConfigs.snapshotSwitches(), InfoSettings.showNoSource));
		if (GENERATION.wasReloaded()) {
			FAILED.clear();
			for (ItemInfoProvider provider : ItemInfoRegistry.getProviders()) {
				Throwable t = ProviderOrdering.isolate(provider::onReload);
				if (t != null) {
					EmiRemastered.LOGGER.error("Item info provider {} failed to reload", provider.getId(), t);
				}
			}
		}
		return CACHE.get(generation, stack, InfoService::compute);
	}

	public static InfoPage compute(EmiStack stack) {
		long start = System.nanoTime();
		CollectorImpl collector = new CollectorImpl(stack);
		for (ItemInfoProvider provider : ItemInfoRegistry.getProviders()) {
			if (!isModuleOn(provider)) {
				continue;
			}
			Identifier id = provider.getId();
			collector.begin(id);
			Throwable t = ProviderOrdering.isolate(() -> {
				if (provider.appliesTo(stack)) {
					provider.collect(stack, collector);
				}
			});
			if (t != null) {
				if (FAILED.add(id)) {
					EmiRemastered.LOGGER.error("Item info provider {} failed for {}", id, stack, t);
				}
				collector.addSection(id, InfoSection.Kind.DETAILS, Text.translatable("emi_info.section.error"))
					.text(Text.translatable("emi_info.error", id.toString(), String.valueOf(t.getMessage())).formatted(Formatting.RED));
			}
		}
		List<SectionImpl> sections = ProviderOrdering.byKind(collector.getSections(), s -> s.kind.ordinal());
		return new InfoPage(stack, sections, false, System.nanoTime() - start);
	}

	private static boolean isModuleOn(ItemInfoProvider provider) {
		String moduleId = provider.getModuleId();
		return moduleId == null || Modules.byId(moduleId).map(Module::isEnabled).orElse(true);
	}
}
