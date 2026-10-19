package dev.emi.remastered.professions;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.professions.trade.TradeCollector;
import dev.emi.remastered.professions.trade.TradeEntry;
import dev.emi.remastered.professions.trade.TradeRecipe;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

/**
 * Port of EMI Professions: one EMI page per villager profession showing its workstations.
 */
public class ProfessionsContent {

	public static void init() {
	}

	public static void initClient() {
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> VillagerPreview.clear());
	}

	public static void register(EmiRegistry registry) {
		registry.addCategory(ProfessionsCategories.PROFESSION);
		registry.addWorkstation(ProfessionsCategories.PROFESSION, EmiStack.of(Items.EMERALD));
		registry.addWorkstation(ProfessionsCategories.PROFESSION, EmiStack.of(Items.VILLAGER_SPAWN_EGG));

		int count = 0;
		List<ProfessionEntry> entries = new ArrayList<>();
		for (VillagerProfession profession : Registries.VILLAGER_PROFESSION) {
			if (profession == VillagerProfession.NONE) {
				continue;
			}
			Identifier id = Registries.VILLAGER_PROFESSION.getId(profession);
			ProfessionEntry entry = new ProfessionEntry(profession, id, findWorkstations(profession, id));
			entries.add(entry);
			if (entry.workstations().isEmpty()) {
				continue;
			}
			registry.addRecipe(new ProfessionRecipe(entry));
			count++;
		}
		EmiLog.info("[Professions] Registered " + count + " profession pages");
		if (ProfessionsSettings.showTrades) {
			registerTrades(registry, entries);
		}
	}

	private static void registerTrades(EmiRegistry registry, List<ProfessionEntry> professions) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			EmiLog.info("[Professions] No client world, skipping villager trades");
			return;
		}
		long start = System.currentTimeMillis();
		TradeCollector.Result result = TradeCollector.collect(world, professions, ProfessionsSettings.tradeRolls);
		registry.addCategory(ProfessionsCategories.VILLAGER_TRADES);
		registry.addCategory(ProfessionsCategories.WANDERING_TRADER);
		for (TradeEntry trade : result.villager()) {
			registry.addRecipe(new TradeRecipe(trade));
		}
		for (TradeEntry trade : result.wanderer()) {
			registry.addRecipe(new TradeRecipe(trade));
		}
		TradeCollector.Stats stats = result.stats();
		stats.tradesPerOwner.forEach((owner, trades) -> EmiLog.info("[Professions] Trades " + owner + ": " + trades));
		EmiLog.info("[Professions] Registered " + result.villager().size() + " villager trades and " + result.wanderer().size()
			+ " wandering trader trades (" + (result.rebalanced() ? "rebalanced" : "vanilla") + " trade maps, " + stats.factories
			+ " factories, " + stats.rolls + " rolls, " + stats.failures + " failed factories, " + stats.placeholderMaps
			+ " placeholder maps, " + (System.currentTimeMillis() - start) + "ms)");
	}

	private static List<EmiStack> findWorkstations(VillagerProfession profession, Identifier id) {
		List<EmiStack> stacks = new ArrayList<>();
		for (RegistryEntry.Reference<PointOfInterestType> poi : Registries.POINT_OF_INTEREST_TYPE.streamEntries().toList()) {
			boolean acquirable;
			try {
				acquirable = profession.acquirableWorkstation().test(poi);
			} catch (RuntimeException e) {
				EmiLog.warn("[Professions] Workstation check of profession " + id + " failed, skipping it: " + e);
				return List.of();
			}
			if (!acquirable) {
				continue;
			}
			poi.value().blockStates().forEach(state -> {
				EmiStack stack = EmiStack.of(state.getBlock());
				if (!stack.isEmpty() && !stacks.contains(stack)) {
					stacks.add(stack);
				}
			});
		}
		return stacks;
	}
}
