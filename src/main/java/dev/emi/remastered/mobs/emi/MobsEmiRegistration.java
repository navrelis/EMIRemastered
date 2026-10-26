package dev.emi.remastered.mobs.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.MobsSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** The EMI side of the mobs module: the category and one info page per mob. */
public final class MobsEmiRegistration {
	public static final EmiRecipeCategory MOB_INFO = new EmiRecipeCategory(Mobs.id("mob_info"), EmiStack.of(Items.CREEPER_SPAWN_EGG));

	private MobsEmiRegistration() {
	}

	public static void initialize(EmiInitRegistry registry) {
		registry.addIngredientSerializer(MobEmiStack.class, new MobEmiStack.Serializer());
	}

	public static void register(EmiRegistry registry) {
		registry.addCategory(MOB_INFO);
		List<EntityType<?>> types = new ArrayList<>();
		for (EntityType<?> type : Registries.ENTITY_TYPE) {
			// living entities with attributes; the player type has them but cannot be created
			if (MobEmiStack.hasPage(type)) {
				types.add(type);
			}
		}
		types.sort(Comparator.comparing((EntityType<?> type) -> Registries.ENTITY_TYPE.getId(type).toString()));
		// the index of items is complete when the plugins register; the spawn eggs of the dragon, wither and golems are in no creative tab
		Set<Object> indexed = new HashSet<>();
		for (EmiStack stack : EmiStackList.stacks) {
			indexed.add(stack.getKey());
		}
		int count = 0;
		int eggless = 0;
		for (EntityType<?> type : types) {
			Identifier id = Registries.ENTITY_TYPE.getId(type);
			try {
				registry.addRecipe(new MobInfoRecipe(type, id));
				SpawnEggItem egg = SpawnEggItem.forEntity(type);
				if (MobsSettings.egglessInIndex && (egg == null || !indexed.contains(egg))) {
					registry.addEmiStack(MobEmiStack.of(type));
					eggless++;
				}
				count++;
			} catch (RuntimeException e) {
				Mobs.LOGGER.error("Could not create the mob page of {}, skipping it", id, e);
			}
		}
		Mobs.LOGGER.info("Registered {} mob pages, {} mobs whose spawn egg is missing or not in the index added to the index", count, eggless);
	}
}
