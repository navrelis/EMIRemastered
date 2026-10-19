/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.client;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiRegistryAdapter;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.OresSettings;
import dev.emi.remastered.ores.emi.OresRecipeCategories;
import dev.emi.remastered.ores.emi.recipe.GeodeGenEmiRecipe;
import dev.emi.remastered.ores.emi.recipe.OreGenEmiRecipe;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.ores.sync.OresSnapshot;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;

/** What the old EMI Ores EMI plugin did. Client only. */
public class OresEmiRegistration {

	public static void initialize(EmiInitRegistry registry) {
		Registry<Biome> biomeRegistry = getBiomeRegistry();
		if (biomeRegistry == null) {
			return;
		}
		registry.addRegistryAdapter(EmiRegistryAdapter.simple(Biome.class, biomeRegistry, BiomeEmiStack::of));
		registry.addIngredientSerializer(BiomeEmiStack.class, new BiomeEmiStack.Serializer());
	}

	public static void register(EmiRegistry registry) {
		Registry<Biome> biomeRegistry = getBiomeRegistry();
		if (biomeRegistry != null && OresSettings.addBiomesToIndex) {
			biomeRegistry.stream().map(BiomeEmiStack::of).forEach(registry::addEmiStack);
		}

		registry.addCategory(OresRecipeCategories.OREGEN);
		registry.addCategory(OresRecipeCategories.GEODE);

		registry.addDeferredRecipes(consumer -> {
			OresSnapshot<PlacedFeature> snapshot = OresFeatures.consume();
			Map<Identifier, PlacedFeature> features = snapshot.features();
			if (features.isEmpty()) {
				Ores.LOG.info("No ore or geode features received from the server");
				return;
			}

			AtomicInteger ores = new AtomicInteger();
			AtomicInteger geodes = new AtomicInteger();
			features.forEach((id, placedFeature) -> {
				try {
					FeatureConfig fc = placedFeature.feature().value().config();
					if (fc instanceof OreFeatureConfig) {
						consumer.accept(new OreGenEmiRecipe(placedFeature, id, snapshot));
						ores.incrementAndGet();
					} else if (fc instanceof GeodeFeatureConfig) {
						consumer.accept(new GeodeGenEmiRecipe(placedFeature, id, snapshot));
						geodes.incrementAndGet();
					}
				} catch (Exception e) {
					Ores.LOG.warn("Failed to create a recipe for placed feature {}", id, e);
				}
			});
			Ores.LOG.info("Registered {} ore generation and {} geode generation recipes from ore sync {}", ores.get(), geodes.get(), snapshot.syncId());
		});
	}

	private static Registry<Biome> getBiomeRegistry() {
		ClientWorld world = MinecraftClient.getInstance().world;
		return world == null ? null : world.getRegistryManager().get(RegistryKeys.BIOME);
	}
}
