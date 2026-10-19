package dev.emi.remastered.professions;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
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
		for (VillagerProfession profession : Registries.VILLAGER_PROFESSION) {
			if (profession == VillagerProfession.NONE) {
				continue;
			}
			Identifier id = Registries.VILLAGER_PROFESSION.getId(profession);
			ProfessionEntry entry = new ProfessionEntry(profession, id, findWorkstations(profession, id));
			if (entry.workstations().isEmpty()) {
				continue;
			}
			registry.addRecipe(new ProfessionRecipe(entry));
			count++;
		}
		EmiLog.info("[Professions] Registered " + count + " profession pages");
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
