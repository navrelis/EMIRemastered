package dev.emi.remastered.mobs.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.mobs.Mobs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** The EMI side of the mobs module: the category and one info page per mob. */
public final class MobsEmiRegistration {
	public static final EmiRecipeCategory MOB_INFO = new EmiRecipeCategory(Mobs.id("mob_info"), EmiStack.of(Items.CREEPER_SPAWN_EGG));

	private MobsEmiRegistration() {
	}

	public static void register(EmiRegistry registry) {
		registry.addCategory(MOB_INFO);
		List<EntityType<?>> types = new ArrayList<>();
		for (EntityType<?> type : Registries.ENTITY_TYPE) {
			// living entities with attributes; the player type has them but cannot be created
			if (type != EntityType.PLAYER && DefaultAttributeRegistry.hasDefinitionFor(type)) {
				types.add(type);
			}
		}
		types.sort(Comparator.comparing((EntityType<?> type) -> Registries.ENTITY_TYPE.getId(type).toString()));
		int count = 0;
		for (EntityType<?> type : types) {
			Identifier id = Registries.ENTITY_TYPE.getId(type);
			try {
				registry.addRecipe(new MobInfoRecipe(type, id));
				count++;
			} catch (RuntimeException e) {
				Mobs.LOGGER.error("Could not create the mob page of {}, skipping it", id, e);
			}
		}
		Mobs.LOGGER.info("Registered {} mob pages", count);
	}
}
