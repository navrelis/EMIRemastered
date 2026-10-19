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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.rule.BlockMatchRuleTest;
import net.minecraft.structure.rule.BlockStateMatchRuleTest;
import net.minecraft.structure.rule.RandomBlockMatchRuleTest;
import net.minecraft.structure.rule.RandomBlockStateMatchRuleTest;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.ScatteredOreFeature;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;

public class OreGenEmiRecipe extends AbstractPlacedFeatureEmiRecipe {

	private final Identifier id;
	private final List<EmiIngredient> inputs;
	private final List<EmiStack> outputs;
	private final HeightProvider heightProvider;
	private final float discardChanceOnAirExposure;
	private final int countMin;
	private final int countMax;
	private final int rarityChance;

	public OreGenEmiRecipe(PlacedFeature feature, Identifier id, OresSnapshot<PlacedFeature> snapshot) {
		super(id, snapshot);
		this.id = id;

		List<EmiIngredient> inputs = new ArrayList<>();
		List<EmiStack> outputs = new ArrayList<>();
		OreFeatureConfig oreConfig = (OreFeatureConfig) feature.feature().value().config();
		this.discardChanceOnAirExposure = oreConfig.discardOnAirChance;

		oreConfig.targets.forEach(targetBlockState -> {
			RuleTest target = targetBlockState.target;
			if (target instanceof TagMatchRuleTest tagMatchTest) {
				TagKey<Block> tag = ((TagMatchTestAccessor) tagMatchTest).emi_ores$getTag();
				List<EmiIngredient> l = Registries.BLOCK.getOrCreateEntryList(tag).stream()
						.map(RegistryEntry::value)
						.map(EmiStack::of)
						.collect(Collectors.toList());
				inputs.add(EmiIngredient.of(l));
			} else if (target instanceof BlockMatchRuleTest blockMatchTest) {
				Block block = ((BlockMatchTestAccessor) blockMatchTest).emi_ores$getBlock();
				inputs.add(EmiStack.of(block));
			} else if (target instanceof BlockStateMatchRuleTest blockStateMatchTest) {
				BlockState state = ((BlockStateMatchTestAccessor) blockStateMatchTest).emi_ores$getBlockState();
				inputs.add(EmiStack.of(state.getBlock()));
			} else if (target instanceof RandomBlockMatchRuleTest randomBlockMatchTest) {
				RandomBlockMatchTestAccessor accessor = (RandomBlockMatchTestAccessor) randomBlockMatchTest;
				Block block = accessor.emi_ores$getBlock();
				float probability = accessor.emi_ores$getProbability();
				inputs.add(EmiStack.of(block).setChance(probability));
			} else if (target instanceof RandomBlockStateMatchRuleTest randomBlockStateMatchTest) {
				RandomBlockStateMatchTestAccessor accessor = (RandomBlockStateMatchTestAccessor) randomBlockStateMatchTest;
				Block block = accessor.emi_ores$getBlockState().getBlock();
				float probability = accessor.emi_ores$getProbability();
				inputs.add(EmiStack.of(block).setChance(probability));
			} else {
				inputs.add(EmiStack.EMPTY);
			}

			outputs.add(EmiStack.of(targetBlockState.state.getBlock()).setAmount(oreConfig.size));
		});
		this.inputs = Collections.unmodifiableList(inputs);
		this.outputs = Collections.unmodifiableList(outputs);

		HeightProvider heightProvider = null;
		int countMin = -1;
		int countMax = -1;
		int rarityChance = -1;
		for (PlacementModifier modifier : feature.placementModifiers()) {
			if (modifier instanceof HeightRangePlacementModifier heightRange) {
				heightProvider = ((HeightRangePlacementAccessor) heightRange).emi_ores$getHeight();
			} else if (modifier instanceof CountPlacementModifier countPlacement) {
				IntProvider countIntProvider = ((CountPlacementAccessor) countPlacement).emi_ores$getCount();
				countMin = countIntProvider.getMin();
				countMax = countIntProvider.getMax();
			} else if (modifier instanceof RarityFilterPlacementModifier rarityFilter) {
				rarityChance = ((RarityFilterAccessor) rarityFilter).emi_ores$getChance();
			}
		}

		if (feature.feature().value().feature() instanceof ScatteredOreFeature) {
			countMin = countMax = 1; // special handling, only used by ancient debris
		}

		this.countMin = countMin;
		this.countMax = countMax;
		this.rarityChance = rarityChance;
		this.heightProvider = heightProvider;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return OresRecipeCategories.OREGEN;
	}

	@Override
	public Identifier getId() {
		return Ores.id("/ore/" + id.getNamespace() + "/" + id.getPath());
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return Stream.concat(inputs.stream(), Stream.of(allBiomes)).toList();
	}

	@Override
	public List<EmiStack> getOutputs() {
		return outputs;
	}

	@Override
	public int getDisplayWidth() {
		return 160;
	}

	@Override
	public int getDisplayHeight() {
		return bodyHeight() + biomeBandHeight();
	}

	private int bodyHeight() {
		return Math.max(54, 18 + 18 * inputs.size());
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		for (int i = 0; i < inputs.size(); i++) {
			widgets.addSlot(inputs.get(i), 0, 18 + i * 18);
			widgets.addTexture(EmiTexture.EMPTY_ARROW, 20, 18 + i * 18);
			widgets.addSlot(outputs.get(i), 46, 18 + i * 18).recipeContext(this);
		}

		addDistributionGraph(widgets, 64, 0, heightProvider);

		addBiomeBand(widgets, bodyHeight());

		if (discardChanceOnAirExposure > 0)
			widgets.addSlot(EmiStack.of(Items.BARRIER).setChance(discardChanceOnAirExposure), 142, 18)
					.appendTooltip(Text.translatable("emi_ores.discard_on_air_chance"));

		Text veinFreq = getVeinFreqComponent(countMin, countMax, rarityChance);
		if (veinFreq != null) {
			widgets.addText(veinFreq, 160, 45, 0, false)
					.horizontalAlign(TextWidget.Alignment.END)
					.verticalAlign(TextWidget.Alignment.CENTER);
		}
	}

}
