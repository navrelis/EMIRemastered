package dev.emi.remastered.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import dev.emi.remastered.distractionfree.DistractionFreeModule;
import dev.emi.remastered.effects.EffectsModule;
import dev.emi.remastered.enchanting.EnchantingModule;
import dev.emi.remastered.info.InfoModule;
import dev.emi.remastered.letsdo.LetsDoModule;
import dev.emi.remastered.loot.LootModule;
import dev.emi.remastered.mobs.MobsModule;
import dev.emi.remastered.ores.OresModule;
import dev.emi.remastered.professions.ProfessionsModule;

/**
 * Registry of all EMI Remastered modules. The order here is the order of the module switches and module config
 * groups in EMI's config screen and in {@code emi.css}.
 * <p>
 * Adding a module: one {@code register(...)} line here plus the module's own package
 * (see {@code docs/MODULES.md}).
 */
public final class Modules {
	private static final List<Module> ALL = new ArrayList<>();

	public static final LootModule LOOT = register(new LootModule());
	public static final OresModule ORES = register(new OresModule());
	public static final EffectsModule EFFECTS = register(new EffectsModule());
	public static final EnchantingModule ENCHANTING = register(new EnchantingModule());
	public static final ProfessionsModule PROFESSIONS = register(new ProfessionsModule());
	public static final LetsDoModule LETSDO = register(new LetsDoModule());
	public static final DistractionFreeModule DISTRACTION_FREE = register(new DistractionFreeModule());
	public static final InfoModule INFO = register(new InfoModule());
	public static final MobsModule MOBS = register(new MobsModule());

	private Modules() {
	}

	private static <T extends Module> T register(T module) {
		for (Module other : ALL) {
			if (other.getId().equals(module.getId())) {
				throw new IllegalStateException("Duplicate EMI Remastered module id " + module.getId());
			}
		}
		if (!module.getId().matches("[a-z0-9_]+")) {
			throw new IllegalStateException("Invalid EMI Remastered module id " + module.getId());
		}
		ALL.add(module);
		return module;
	}

	/** All modules in registry order. */
	public static List<Module> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static Optional<Module> byId(String id) {
		for (Module module : ALL) {
			if (module.getId().equals(id)) {
				return Optional.of(module);
			}
		}
		return Optional.empty();
	}
}
