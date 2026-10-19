/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.OresSettings;
import dev.emi.remastered.ores.display.BiomeSummary;
import dev.emi.remastered.ores.display.EntryGroups;
import dev.emi.remastered.ores.display.HeightModel.Resolved;
import dev.emi.remastered.ores.display.HeightModel.Shape;
import dev.emi.remastered.ores.emi.OresRecipeCategories;
import dev.emi.remastered.ores.emi.OresTexts;
import dev.emi.remastered.ores.emi.recipe.AbstractPlacedFeatureEmiRecipe;
import dev.emi.remastered.ores.emi.recipe.AbstractPlacedFeatureEmiRecipe.DimensionHeight;
import dev.emi.remastered.ores.emi.recipe.AbstractPlacedFeatureEmiRecipe.Facts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Adds a "World generation" section to the Item Info page of ores and geode blocks: where the stack generates
 * (dimension), its best height and a compact biome summary, linking to the ore generation recipe.
 *
 * <p>The built-in recipes provider already lists the ore generation recipes under "Produced by" and counts them as a
 * source, so this section is not a source ({@code source(false)}) and only adds the compact facts.
 */
public final class OresInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Ores.id("world_generation");
	/** Rows per page, the rest is reachable through a link to the recipes. */
	private static final int MAX_ROWS = 6;
	private static final int MAX_NAMED_BIOMES = 3;
	private static final int NAMES_PER_LINE = 4;
	private static final int MAX_NAME_LINES = 4;
	private static final int MAX_TOOLTIP_IDS = 4;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "ores";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return OresSettings.showInfoSection && stack.getKey() instanceof Item;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		List<AbstractPlacedFeatureEmiRecipe> recipes = new ArrayList<>();
		for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
			if (recipe instanceof AbstractPlacedFeatureEmiRecipe generation) {
				recipes.add(generation);
			}
		}
		if (recipes.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ID, InfoSection.Kind.OBTAIN, Text.translatable("emi_ores.info.title"))
			.icon(recipes.get(0).getCategory())
			.source(false);
		// the placed features of one ore (large, buried, ...) often look the same on this page, so they share a row
		List<List<AbstractPlacedFeatureEmiRecipe>> rows = EntryGroups.group(recipes, OresInfoProvider::rowKey);
		for (int i = 0; i < rows.size() && i < MAX_ROWS; i++) {
			addRow(section, rows.get(i));
		}
		if (rows.size() > MAX_ROWS) {
			int hidden = 0;
			for (int i = MAX_ROWS; i < rows.size(); i++) {
				hidden += rows.get(i).size();
			}
			section.link(Text.translatable("emi_ores.info.more", hidden), InfoLink.recipes(new ArrayList<EmiRecipe>(recipes)));
		}
	}

	/** Rows are equal when they would show the same dimensions, heights and biomes. */
	static List<Object> rowKey(AbstractPlacedFeatureEmiRecipe recipe) {
		Facts facts = recipe.facts();
		List<Object> heights = new ArrayList<>();
		for (DimensionHeight height : facts.heights()) {
			heights.add(List.of(height.dimension(), EntryGroups.heightKey(height.height())));
		}
		return List.of(heights, facts.biomeSections());
	}

	/** One row for one or more recipes that show the same facts; the link opens the recipe, or all of them. */
	private static void addRow(InfoSection section, List<AbstractPlacedFeatureEmiRecipe> group) {
		AbstractPlacedFeatureEmiRecipe recipe = group.get(0);
		Facts facts = recipe.facts();
		List<Identifier> dimensions = facts.heights().stream().map(DimensionHeight::dimension).toList();
		Text label = dimensions.isEmpty() ? Text.translatable("emi_ores.biomes.other") : OresTexts.dimensionNames(dimensions);

		List<Text> tooltip = new ArrayList<>();
		for (int i = 0; i < group.size() && i < MAX_TOOLTIP_IDS; i++) {
			tooltip.add(Text.literal(group.get(i).facts().featureId().toString()).formatted(Formatting.DARK_GRAY));
		}
		if (group.size() > MAX_TOOLTIP_IDS) {
			tooltip.add(Text.literal("...").formatted(Formatting.DARK_GRAY));
		}
		for (DimensionHeight entry : facts.heights()) {
			if (entry.height() != null) {
				tooltip.add(Text.translatable("emi_ores.info.height_in", OresTexts.dimensionName(entry.dimension()),
					entry.height().min(), entry.height().max()).formatted(Formatting.GRAY));
			}
		}
		List<Text> hidden = hiddenBiomes(facts);
		tooltip.addAll(hidden);

		var entry = section.entry(recipe.getCategory(), label).link(group.size() == 1 ? InfoLink.recipe(recipe) : InfoLink.recipes(new ArrayList<EmiRecipe>(group))).tooltip(tooltip);
		Resolved best = firstHeight(facts);
		if (best != null) {
			entry.detail(bestText(best));
		}
		Text summary = biomeSummary(facts);
		if (summary != null) {
			section.text(summary.copy().formatted(Formatting.DARK_GRAY));
		}
	}

	private static Resolved firstHeight(Facts facts) {
		for (DimensionHeight height : facts.heights()) {
			if (height.height() != null) {
				return height.height();
			}
		}
		return null;
	}

	/** The best level (or the range of a uniform distribution), short enough for the right side of a row. */
	static Text bestText(Resolved height) {
		if (height.peakLow() == height.peakHigh()) {
			return Text.translatable("emi_ores.info.best_single", height.peakLow());
		}
		if (height.shape() == Shape.UNIFORM) {
			return Text.translatable("emi_ores.info.range", height.min(), height.max());
		}
		return Text.translatable("emi_ores.info.best", height.peakLow(), height.peakHigh());
	}

	/** One paragraph: per dimension all biomes, all except some, a few named biomes or a count. */
	static Text biomeSummary(Facts facts) {
		MutableText result = null;
		for (BiomeSummary.Section section : facts.biomeSections()) {
			Text dimension = section.dimension() == null ? Text.translatable("emi_ores.biomes.other") : OresTexts.dimensionName(section.dimension());
			Text part = switch (section.kind()) {
				case ALL -> Text.translatable("emi_ores.biomes.all", dimension);
				case ALL_EXCEPT -> Text.translatable("emi_ores.info.biomes_except", dimension, section.listed().size());
				case LIST -> section.listed().size() <= MAX_NAMED_BIOMES
					? Text.translatable("emi_ores.info.biomes_named", dimension, String.join(", ", OresTexts.sortedNames(section.listed())))
					: Text.translatable("emi_ores.info.biomes_count", section.listed().size(), dimension);
			};
			if (result == null) {
				result = Text.empty().append(part);
			} else {
				result.append("; ").append(part);
			}
		}
		return result;
	}

	/** Tooltip lines with the biomes a summary left out, so the full list is one hover away. */
	private static List<Text> hiddenBiomes(Facts facts) {
		List<Text> lines = new ArrayList<>();
		for (BiomeSummary.Section section : facts.biomeSections()) {
			if (section.kind() == BiomeSummary.Kind.ALL || section.listed().isEmpty()) {
				continue;
			}
			Text dimension = section.dimension() == null ? Text.translatable("emi_ores.biomes.other") : OresTexts.dimensionName(section.dimension());
			Text header = section.kind() == BiomeSummary.Kind.ALL_EXCEPT
				? Text.translatable("emi_ores.biomes.all_except", dimension)
				: Text.translatable("emi_ores.biomes.in", dimension);
			lines.add(header.copy().formatted(Formatting.GRAY));
			List<String> names = OresTexts.sortedNames(section.listed());
			int shown = Math.min(names.size(), NAMES_PER_LINE * MAX_NAME_LINES);
			for (int i = 0; i < shown; i += NAMES_PER_LINE) {
				String line = String.join(", ", names.subList(i, Math.min(shown, i + NAMES_PER_LINE)));
				lines.add(Text.literal(shown < names.size() && i + NAMES_PER_LINE >= shown ? line + ", ..." : line).formatted(Formatting.DARK_GRAY));
			}
		}
		return lines;
	}
}
