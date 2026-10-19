/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi.recipe;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.emi.OresRecipeCategories;
import dev.emi.remastered.ores.sync.OresSnapshot;
import dev.emi.remastered.ores.mixin.accessor.*;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Weighted;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.GeodeLayerConfig;
import net.minecraft.world.gen.feature.GeodeLayerThicknessConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.NoiseBlockStateProvider;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider;

public class GeodeGenEmiRecipe extends AbstractPlacedFeatureEmiRecipe {
	private final HeightProvider heightProvider;
	private final int rarityChance;
	private final EmiIngredient alternateInner;
	private final EmiIngredient inner;
	private final EmiIngredient middle;
	private final EmiIngredient outer;
	private final EmiIngredient filling;
	private final EmiIngredient innerPlacements;
	private final Identifier id;

	public GeodeGenEmiRecipe(PlacedFeature feature, Identifier id, OresSnapshot<PlacedFeature> snapshot) {
		super(id, snapshot);
		this.id = id;
		HeightProvider heightProvider = null;
		int rarityChance = -1;
		for (PlacementModifier modifier : feature.placementModifiers()) {
			if (modifier instanceof HeightRangePlacementModifier heightRangePlacement) {
				heightProvider = ((HeightRangePlacementAccessor) heightRangePlacement).emi_ores$getHeight();
			} else if (modifier instanceof RarityFilterPlacementModifier rarityFilter) {
				rarityChance = ((RarityFilterAccessor) rarityFilter).emi_ores$getChance();
			}
		}
		this.heightProvider = heightProvider;
		this.rarityChance = rarityChance;

		GeodeFeatureConfig config = (GeodeFeatureConfig) feature.feature().value().config();

		GeodeLayerConfig blockSettings = config.layerConfig;
		GeodeLayerThicknessConfig layerSettings = config.layerThicknessConfig;

		this.alternateInner = ingredientForStateProvider(blockSettings.alternateInnerLayerProvider)
				.setChance((float) config.useAlternateLayer0Chance);

		this.filling = ingredientForStateProvider(blockSettings.fillingProvider)
				.setAmount((long) Math.ceil(layerSettings.filling));

		this.inner = ingredientForStateProvider(blockSettings.innerLayerProvider)
				.setAmount((long) Math.ceil(layerSettings.innerLayer - layerSettings.filling));

		this.middle = ingredientForStateProvider(blockSettings.middleLayerProvider)
				.setAmount((long) Math.ceil(layerSettings.middleLayer - layerSettings.innerLayer));

		this.outer = ingredientForStateProvider(blockSettings.outerLayerProvider)
				.setAmount((long) Math.ceil(layerSettings.outerLayer - layerSettings.middleLayer));

		this.innerPlacements = EmiIngredient.of(blockSettings.innerBlocks.stream()
				.map(BlockState::getBlock)
				.map(EmiStack::of)
				.collect(Collectors.toList())
		).setChance((float) config.usePotentialPlacementsChance);
	}

	private static EmiIngredient ingredientForStateProvider(BlockStateProvider provider) {
		if (provider instanceof SimpleBlockStateProvider simple) {
			return EmiStack.of(((SimpleStateProviderAccessor) simple).emi_ores$getState().getBlock());
		} else if (provider instanceof WeightedBlockStateProvider weighted) {
			// ignore the weights
			return EmiIngredient.of(((WeightedStateProviderAccessor) weighted).emi_ores$getWeightedList()
					.getEntries()
					.stream()
					.map(Weighted.Present::data)
					.map(BlockState::getBlock)
					.map(EmiStack::of)
					.toList());
		} else if (provider instanceof NoiseBlockStateProvider noise) {
			return EmiIngredient.of(((NoiseProviderAccessor) noise).emi_ores$getStates()
					.stream()
					.map(BlockState::getBlock)
					.distinct()
					.map(EmiStack::of)
					.toList());
		}

		return EmiStack.EMPTY;
	}

	@Override
	protected HeightProvider heightProvider() {
		return heightProvider;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return OresRecipeCategories.GEODE;
	}

	@Override
	public Identifier getId() {
		return Ores.id("/geode/" + id.getNamespace() + "/" + id.getPath());
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(allBiomes);
	}

	@Override
	public List<EmiStack> getOutputs() {
		return Stream.of(innerPlacements, filling, alternateInner, inner, middle, outer)
				.map(EmiIngredient::getEmiStacks)
				.flatMap(Collection::stream)
				.toList();
	}

	@Override
	public int getDisplayWidth() {
		return 160;
	}

	@Override
	public int getDisplayHeight() {
		return 90 + biomeBandHeight();
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addSlot(filling, 0, 18)
				.recipeContext(this);
		widgets.addSlot(innerPlacements, 46, 18)
				.recipeContext(this);
		widgets.addSlot(inner, 0, 36)
				.recipeContext(this);
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 20, 36);
		widgets.addSlot(alternateInner, 46, 36)
				.recipeContext(this);
		widgets.addSlot(middle, 0, 54)
				.recipeContext(this);
		widgets.addSlot(outer, 0, 72)
				.recipeContext(this);

		addDistributionGraph(widgets, 64, 0, heightProvider);

		addBiomeBand(widgets, 90);

		Text veinFreq = getVeinFreqComponent(-1, -1, rarityChance);
		if (veinFreq != null) {
			widgets.addText(veinFreq, 160, 45, 0, false)
					.horizontalAlign(TextWidget.Alignment.END)
					.verticalAlign(TextWidget.Alignment.CENTER);
		}

	}

}
