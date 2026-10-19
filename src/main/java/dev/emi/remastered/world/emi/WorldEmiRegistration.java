package dev.emi.remastered.world.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.WorldSettings;
import dev.emi.remastered.world.client.ClientWorldData;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.WorldSnapshot;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * The EMI side of the world module: the Biome and Structure categories and their pages. Biome pages come from the
 * client's biome registry and read the server's data when they are opened; structure pages are created from the
 * server's data (see {@link ClientWorldData} for the reload when it arrives late). Client only.
 */
public final class WorldEmiRegistration {
	public static final EmiRecipeCategory BIOME = new EmiRecipeCategory(World.id("biome"), EmiStack.of(Items.GRASS_BLOCK));
	public static final EmiRecipeCategory STRUCTURE = new EmiRecipeCategory(World.id("structure"), EmiStack.of(Items.COMPASS));

	private WorldEmiRegistration() {
	}

	public static void initialize(EmiInitRegistry registry) {
		registry.addIngredientSerializer(StructureEmiStack.class, new StructureEmiStack.Serializer());
		if (!Modules.ORES.isEnabled()) {
			// the ores module registers the same serializer when it is on; biome stacks are saved as "biome:<id>" in favourites
			registry.addIngredientSerializer(BiomeEmiStack.class, new BiomeEmiStack.Serializer());
		}
	}

	public static void register(EmiRegistry registry) {
		long start = System.nanoTime();
		Registry<Biome> biomes = StructureInfoRecipe.biomeRegistry();
		if (biomes == null) {
			return;
		}
		int biomePages = 0;
		int structurePages = 0;
		if (WorldSettings.biomePages) {
			registry.addCategory(BIOME);
			List<Map.Entry<Identifier, Biome>> sorted = new ArrayList<>();
			biomes.getEntrySet().forEach(entry -> sorted.add(Map.entry(entry.getKey().getValue(), entry.getValue())));
			sorted.sort(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)));
			for (Map.Entry<Identifier, Biome> entry : sorted) {
				try {
					registry.addRecipe(BiomeInfoRecipe.of(biomes, entry.getKey(), entry.getValue()));
					biomePages++;
				} catch (RuntimeException e) {
					World.LOGGER.error("Could not create the biome page of {}, skipping it", entry.getKey(), e);
				}
			}
			if (!Modules.ORES.isEnabled()) {
				// the ores module adds the biomes to the index otherwise (setting ores.add-biomes-to-index)
				sorted.forEach(entry -> registry.addEmiStack(BiomeEmiStack.of(entry.getValue())));
			}
		}
		if (WorldSettings.structurePages) {
			registry.addCategory(STRUCTURE);
			WorldSnapshot data = ClientWorldData.consume();
			for (StructureRow row : data.structures()) {
				try {
					registry.addRecipe(new StructureInfoRecipe(row, biomes));
					if (WorldSettings.structuresInIndex) {
						registry.addEmiStack(StructureEmiStack.of(row.id()));
					}
					structurePages++;
				} catch (RuntimeException e) {
					World.LOGGER.error("Could not create the structure page of {}, skipping it", row.id(), e);
				}
			}
			if (!data.isComplete()) {
				World.LOGGER.info("No structure data from the server yet, the structure pages are added when it arrives");
			}
		}
		EmiLog.info("world: " + biomePages + " biome pages and " + structurePages + " structure pages in " + (System.nanoTime() - start) / 1_000_000 + " ms");
		World.LOGGER.info("Registered {} biome pages and {} structure pages", biomePages, structurePages);
	}
}
