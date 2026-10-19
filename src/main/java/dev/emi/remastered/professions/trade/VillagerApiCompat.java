package dev.emi.remastered.professions.trade;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.runtime.EmiLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffers;

/**
 * Soft support for the Villager API mod (mod id {@code villagerapi}): its villager packs add trades through a mixin
 * into {@code VillagerEntity.fillRecipes}, so they are not part of the vanilla trade maps. Everything is reflection,
 * nothing happens unless the mod and its trade class are present.
 */
public final class VillagerApiCompat {
	private static final String MOD_ID = "villagerapi";
	private static final String TRADES_CLASS = "com.frikinjay.villagerapi.villagerpack.VillagerPackTrades";
	private static boolean resolved;
	private static Method getPlan;

	private VillagerApiCompat() {
	}

	/**
	 * Result for one profession level.
	 *
	 * @param includeOriginal Whether the vanilla trades of the level stay
	 * @param factories       Extra trade factories of the packs
	 */
	public record Plan(boolean includeOriginal, List<TradeOffers.Factory> factories) {
	}

	public static boolean available() {
		if (!resolved) {
			resolved = true;
			resolve();
		}
		return getPlan != null;
	}

	private static void resolve() {
		if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
			return;
		}
		try {
			Class<?> trades = Class.forName(TRADES_CLASS, false, VillagerApiCompat.class.getClassLoader());
			Method method = trades.getMethod("getPlan", Identifier.class, int.class);
			if (!Modifier.isStatic(method.getModifiers())) {
				return;
			}
			getPlan = method;
			EmiLog.info("[Professions] Villager API found, including its villager pack trades");
		} catch (ReflectiveOperationException | LinkageError e) {
			EmiLog.LOG.debug("[EMI] [Professions] Villager API found but its trade plans are not readable: {}", e.toString());
		}
	}

	/**
	 * @return The pack plan for a profession level, null if the mod is absent or has no plan for it
	 */
	@Nullable
	public static Plan plan(Identifier profession, int level) {
		if (!available()) {
			return null;
		}
		try {
			VillagerApiPlans.Plan raw = VillagerApiPlans.read(getPlan.invoke(null, profession, level));
			if (raw == null) {
				return null;
			}
			List<TradeOffers.Factory> factories = new ArrayList<>();
			for (Object listing : raw.listings()) {
				if (listing instanceof TradeOffers.Factory factory) {
					factories.add(factory);
				}
			}
			return new Plan(raw.includeOriginal(), factories);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			EmiLog.LOG.debug("[EMI] [Professions] Villager API plan of {} level {} failed: {}", profession, level, e.toString());
			return null;
		}
	}
}
