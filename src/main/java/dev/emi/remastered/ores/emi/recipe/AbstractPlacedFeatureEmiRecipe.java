/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi.recipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.ores.OresSettings;
import dev.emi.remastered.ores.display.BiomeSummary;
import dev.emi.remastered.ores.display.HeightModel;
import dev.emi.remastered.ores.display.HeightModel.Anchor;
import dev.emi.remastered.ores.display.HeightModel.Kind;
import dev.emi.remastered.ores.display.HeightModel.Resolved;
import dev.emi.remastered.ores.display.HeightModel.Shape;
import dev.emi.remastered.ores.display.HeightModel.Spec;
import dev.emi.remastered.ores.emi.OresTexts;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.ores.mixin.accessor.TrapezoidHeightAccessor;
import dev.emi.remastered.ores.mixin.accessor.UniformHeightAccessor;
import dev.emi.remastered.ores.sync.OresSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.heightprovider.TrapezoidHeightProvider;
import net.minecraft.world.gen.heightprovider.UniformHeightProvider;

public abstract class AbstractPlacedFeatureEmiRecipe implements EmiRecipe {
	private static final int GRAPH_WIDTH = 32;
	private static final int GRAPH_HEIGHT = 16;
	/** Icons per row of the biome list, the recipe is 160 wide. */
	private static final int BAND_COLUMNS = 8;
	private static final int LINE_HEIGHT = 11;
	private static final int SECTION_GAP = 2;
	private static final int MAX_TOOLTIP_LINES = 16;
	private static final int TOOLTIP_WIDTH = 250;

	private static final int COLOR_UNIFORM = 0xFF3030D0;
	private static final int COLOR_TRIANGLE = 0xFF20A020;
	private static final int COLOR_TRAPEZOID = 0xFFC02020;
	private static final int COLOR_PEAK = 0x66FFD23C;
	private static final int COLOR_PLAYER = 0xFFFF6A00;

	protected final Identifier featureId;
	private final OresSnapshot<PlacedFeature> snapshot;
	/** Every biome of the feature, for EMI's lookups (clicking a biome shows the pages it is in). */
	protected final EmiIngredient allBiomes;
	private final List<BandSection> bandSections = new ArrayList<>();

	protected AbstractPlacedFeatureEmiRecipe(Identifier featureId, OresSnapshot<PlacedFeature> snapshot) {
		this.featureId = featureId;
		this.snapshot = snapshot;

		Registry<Biome> registry = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
		List<Identifier> biomes = BiomeSummary.sorted(snapshot.biomesOf(featureId)).stream()
			.filter(id -> registry.containsId(id))
			.toList();
		this.allBiomes = EmiIngredient.of(biomes.stream().map(id -> BiomeEmiStack.of(registry.get(id))).toList());

		int limit = OresSettings.biomeIconLimit();
		for (BiomeSummary.Section section : BiomeSummary.summarize(biomes, snapshot.dimensionsOf(featureId), limit)) {
			BiomeSummary.Split split = BiomeSummary.split(section.listed(), limit);
			List<EmiStack> shown = split.shown().stream().map(id -> BiomeEmiStack.of(registry.get(id))).toList();
			bandSections.add(new BandSection(section, shown, split.hidden()));
		}
	}

	// ---- biome list ----

	private record BandSection(BiomeSummary.Section data, List<EmiStack> shown, List<Identifier> hidden) {
		int height() {
			int rows = BiomeSummary.rows(shown.size(), !hidden.isEmpty(), BAND_COLUMNS);
			return LINE_HEIGHT + rows * 18 + SECTION_GAP;
		}
	}

	/** Height of the biome list, to add below the rest of the recipe. */
	protected int biomeBandHeight() {
		int height = SECTION_GAP;
		for (BandSection section : bandSections) {
			height += section.height();
		}
		return height;
	}

	/** Adds the dimensions and compact biome list with its top at {@code top}. */
	protected void addBiomeBand(WidgetHolder widgets, int top) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int y = top + SECTION_GAP;
		for (BandSection section : bandSections) {
			BiomeSummary.Section data = section.data();
			Text dimension = data.dimension() == null ? Text.translatable("emi_ores.biomes.other") : OresTexts.dimensionName(data.dimension());
			Text header = switch (data.kind()) {
				case ALL -> Text.translatable("emi_ores.biomes.all", dimension);
				case ALL_EXCEPT -> Text.translatable("emi_ores.biomes.all_except", dimension);
				case LIST -> Text.translatable("emi_ores.biomes.in", dimension);
			};
			widgets.addText(header, 0, y + 1, 0, false);
			if (data.kind() == BiomeSummary.Kind.ALL) {
				Text count = Text.translatable("emi_ores.biomes.count", data.total());
				widgets.addTooltipText(List.of(dimension, count.copy().formatted(Formatting.GRAY)), 0, y, Math.max(1, font.getWidth(header)), LINE_HEIGHT);
			}
			int rowY = y + LINE_HEIGHT;
			int cell = 0;
			for (EmiStack stack : section.shown()) {
				widgets.addSlot(stack, (cell % BAND_COLUMNS) * 18, rowY + (cell / BAND_COLUMNS) * 18);
				cell++;
			}
			if (!section.hidden().isEmpty()) {
				if (cell % BAND_COLUMNS + BiomeSummary.MORE_CELLS > BAND_COLUMNS) {
					cell += BAND_COLUMNS - cell % BAND_COLUMNS;
				}
				int moreX = (cell % BAND_COLUMNS) * 18;
				int moreY = rowY + (cell / BAND_COLUMNS) * 18;
				addMoreLabel(widgets, font, moreX, moreY, section.hidden(), data.kind() == BiomeSummary.Kind.ALL_EXCEPT);
			}
			y += section.height();
		}
	}

	private static void addMoreLabel(WidgetHolder widgets, TextRenderer font, int x, int y, List<Identifier> hidden, boolean excluded) {
		int width = BiomeSummary.MORE_CELLS * 18;
		Text label = Text.translatable("emi_ores.biomes.more", hidden.size());
		widgets.addDrawable(x, y, width, 18, (gui, mouseX, mouseY, delta) -> {
			String text = font.trimToWidth(label.getString(), width - 2);
			gui.drawText(font, text, 2, 5, 0x404040, false);
		}).tooltip((mouseX, mouseY) -> moreTooltip(font, hidden, excluded));
	}

	private static List<TooltipComponent> moreTooltip(TextRenderer font, List<Identifier> hidden, boolean excluded) {
		List<TooltipComponent> lines = new ArrayList<>();
		MutableText title = Text.translatable(excluded ? "emi_ores.biomes.more_excluded" : "emi_ores.biomes.more_tooltip", hidden.size());
		lines.add(TooltipComponent.of(title.asOrderedText()));
		String names = String.join(", ", OresTexts.sortedNames(hidden));
		List<OrderedText> wrapped = font.wrapLines(StringVisitable.plain(names), TOOLTIP_WIDTH);
		for (int i = 0; i < wrapped.size() && i < MAX_TOOLTIP_LINES; i++) {
			lines.add(TooltipComponent.of(wrapped.get(i)));
		}
		if (wrapped.size() > MAX_TOOLTIP_LINES) {
			lines.add(TooltipComponent.of(Text.literal("...").formatted(Formatting.GRAY).asOrderedText()));
		}
		return lines;
	}

	// ---- height graph ----

	/** The dimension a graph is resolved for and its height range. */
	private record Page(Identifier dimension, int bottomY, int topY) {
	}

	/**
	 * The dimension the heights are resolved for: the player's when the feature generates there, else the first
	 * dimension it generates in. Evaluated when the page is opened, the player may have changed dimension since EMI loaded.
	 */
	private Page page() {
		ClientWorld world = MinecraftClient.getInstance().world;
		Identifier current = world.getRegistryKey().getValue();
		OresSnapshot.Dimension chosen = null;
		for (OresSnapshot.Dimension dimension : snapshot.dimensionsOf(featureId)) {
			if (chosen == null || dimension.id().equals(current)) {
				chosen = dimension;
			}
			if (dimension.id().equals(current)) {
				break;
			}
		}
		if (chosen == null) {
			return new Page(null, world.getBottomY(), world.getTopY());
		}
		return new Page(chosen.id(), chosen.bottomY(), chosen.topY());
	}

	private static Anchor toAnchor(YOffset offset) {
		if (offset instanceof YOffset.Fixed fixed) {
			return new Anchor(Kind.FIXED, fixed.y());
		} else if (offset instanceof YOffset.AboveBottom aboveBottom) {
			return new Anchor(Kind.ABOVE_BOTTOM, aboveBottom.offset());
		} else if (offset instanceof YOffset.BelowTop belowTop) {
			return new Anchor(Kind.BELOW_TOP, belowTop.offset());
		}
		throw new IllegalArgumentException("unknown height anchor " + offset);
	}

	private static Spec toSpec(HeightProvider provider) {
		if (provider instanceof UniformHeightProvider uniform) {
			UniformHeightAccessor accessor = (UniformHeightAccessor) uniform;
			return new Spec(true, toAnchor(accessor.emi_ores$getMinInclusive()), toAnchor(accessor.emi_ores$getMaxInclusive()), 0);
		} else if (provider instanceof TrapezoidHeightProvider trapezoid) {
			TrapezoidHeightAccessor accessor = (TrapezoidHeightAccessor) trapezoid;
			return new Spec(false, toAnchor(accessor.emi_ores$getMinInclusive()), toAnchor(accessor.emi_ores$getMaxInclusive()), accessor.emi_ores$getPlateau());
		}
		return null;
	}

	/** Anchors are shown resolved for the page's dimension, or as written (hold shift to switch). */
	private static Text anchorText(Anchor anchor, Page page, boolean resolved, boolean longForm) {
		if (anchor.kind() == Kind.FIXED || resolved) {
			return Text.literal(String.valueOf(anchor.resolve(page.bottomY(), page.topY())));
		}
		int value = anchor.value();
		if (anchor.kind() == Kind.ABOVE_BOTTOM) {
			if (longForm) {
				return value == 0 ? Text.translatable("emi_ores.distribution.anchor.bottom")
					: value > 0 ? Text.translatable("emi_ores.distribution.anchor.above_bottom", value)
					: Text.translatable("emi_ores.distribution.anchor.below_bottom", -value);
			}
			return Text.literal(value == 0 ? "bot" : value > 0 ? "bot+" + value : "bot" + value);
		}
		if (longForm) {
			return value == 0 ? Text.translatable("emi_ores.distribution.anchor.top")
				: value < 0 ? Text.translatable("emi_ores.distribution.anchor.above_top", -value)
				: Text.translatable("emi_ores.distribution.anchor.below_top", value);
		}
		return Text.literal(value == 0 ? "top" : value < 0 ? "top+" + -value : "top-" + value);
	}

	private void addAnchorText(WidgetHolder widgets, Page page, Anchor anchor, int x, int y, TextWidget.Alignment verticalAlign, TextWidget.Alignment horizontalAlign) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;

		widgets.addDrawable(x, y, 0, 0, (gui, mouseX, mouseY, delta) -> {
			Text text = anchorText(anchor, page, useResolvedHeights(), false);
			int textWidth = font.getWidth(text);
			int textHeight = font.fontHeight;
			int textX = switch (horizontalAlign) {
				case START -> 0;
				case CENTER -> -textWidth / 2;
				case END -> -textWidth;
			};
			int textY = switch (verticalAlign) {
				case START -> 0;
				case CENTER -> -textHeight / 2;
				case END -> -textHeight;
			};
			gui.drawText(font, text, textX, textY, 0, false);
		});
	}

	protected void addDistributionGraph(WidgetHolder widgets, int x, int y, HeightProvider heightProvider) {
		if (heightProvider == null) {
			return;
		}
		Spec spec = toSpec(heightProvider);
		if (spec == null) {
			return;
		}
		Page page = page();
		Resolved resolved = HeightModel.resolve(spec, page.bottomY(), page.topY());
		Anchor[] peak = HeightModel.peakAnchors(spec, resolved);

		widgets.addDrawable(x, y, GRAPH_WIDTH, GRAPH_HEIGHT, (gui, mouseX, mouseY, delta) -> drawGraph(gui, resolved, page));
		widgets.addTooltip((mouseX, mouseY) -> getDistributionGraphTooltip(spec, resolved, peak, page), x, y, GRAPH_WIDTH, GRAPH_HEIGHT);
		addAnchorText(widgets, page, spec.min(), x, y + 8, TextWidget.Alignment.CENTER, TextWidget.Alignment.END);
		addAnchorText(widgets, page, spec.max(), x + GRAPH_WIDTH, y + 8, TextWidget.Alignment.CENTER, TextWidget.Alignment.START);
		if (resolved.shape() == Shape.TRIANGULAR) {
			addAnchorText(widgets, page, peak[0], x + GRAPH_WIDTH / 2, y + 8, TextWidget.Alignment.CENTER, TextWidget.Alignment.CENTER);
		}
	}

	/** The player's block height when the page is for the dimension the player is in, else null. */
	private static Integer playerHeight(Page page) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null || page.dimension() == null
			|| !client.world.getRegistryKey().getValue().equals(page.dimension())) {
			return null;
		}
		return client.player.getBlockY();
	}

	private static void drawGraph(DrawContext gui, Resolved resolved, Page page) {
		int color = switch (resolved.shape()) {
			case UNIFORM -> COLOR_UNIFORM;
			case TRIANGULAR -> COLOR_TRIANGLE;
			case TRAPEZOID -> COLOR_TRAPEZOID;
		};
		int fill = (color & 0x00FFFFFF) | 0x38000000;

		if (OresSettings.highlightBestHeight) {
			int from = resolved.shape() == Shape.UNIFORM ? 0 : resolved.pixelOf(resolved.peakLow(), GRAPH_WIDTH);
			int to = resolved.shape() == Shape.UNIFORM ? GRAPH_WIDTH - 1 : resolved.pixelOf(resolved.peakHigh(), GRAPH_WIDTH);
			if (to - from < 2) {
				from = Math.max(0, from - 1);
				to = Math.min(GRAPH_WIDTH - 1, to + 1);
			}
			gui.fill(from, 0, to + 1, GRAPH_HEIGHT, COLOR_PEAK);
		}

		int previousTop = -1;
		for (int px = 0; px < GRAPH_WIDTH; px++) {
			double density = resolved.density(resolved.levelAt(px, GRAPH_WIDTH));
			int top = 1 + (int) Math.round((1 - density) * (GRAPH_HEIGHT - 3));
			gui.fill(px, top + 1, px + 1, GRAPH_HEIGHT - 1, fill);
			int from = previousTop < 0 ? top : Math.min(top, previousTop);
			int to = previousTop < 0 ? top : Math.max(top, previousTop);
			gui.fill(px, from, px + 1, to + 1, color);
			if (px == 0 || px == GRAPH_WIDTH - 1) {
				gui.fill(px, top, px + 1, GRAPH_HEIGHT - 1, color);
			}
			previousTop = top;
		}

		Integer player = OresSettings.showPlayerHeight ? playerHeight(page) : null;
		if (player != null && resolved.contains(player)) {
			int px = resolved.pixelOf(player, GRAPH_WIDTH);
			gui.fill(px, 0, px + 1, GRAPH_HEIGHT, COLOR_PLAYER);
			gui.fill(px - 1, 0, px + 2, 1, COLOR_PLAYER);
		}
	}

	private List<TooltipComponent> getDistributionGraphTooltip(Spec spec, Resolved resolved, Anchor[] peak, Page page) {
		boolean useResolved = useResolvedHeights();
		List<Text> tooltip = new ArrayList<>();

		Text typeName = switch (resolved.shape()) {
			case UNIFORM -> Text.translatable("emi_ores.distribution.uniform").formatted(Formatting.BLUE);
			case TRIANGULAR -> Text.translatable("emi_ores.distribution.triangle").formatted(Formatting.GREEN);
			case TRAPEZOID -> Text.translatable("emi_ores.distribution.trapezoid").formatted(Formatting.RED);
		};
		tooltip.add(typeName);
		tooltip.add(Text.translatable("emi_ores.distribution.range", anchorTextLong(spec.min(), page, useResolved), anchorTextLong(spec.max(), page, useResolved)).formatted(Formatting.GRAY));
		if (resolved.shape() == Shape.UNIFORM) {
			tooltip.add(Text.translatable("emi_ores.distribution.best_all").formatted(Formatting.GRAY));
		} else if (resolved.peakLow() == resolved.peakHigh()) {
			tooltip.add(Text.translatable("emi_ores.distribution.middle", anchorTextLong(peak[0], page, useResolved)).formatted(Formatting.GRAY));
		} else {
			tooltip.add(Text.translatable("emi_ores.distribution.middle_range", anchorTextLong(peak[0], page, useResolved), anchorTextLong(peak[1], page, useResolved)).formatted(Formatting.GRAY));
		}

		if (page.dimension() != null) {
			tooltip.add(Text.translatable("emi_ores.distribution.dimension", OresTexts.dimensionName(page.dimension())).formatted(Formatting.GRAY));
		}
		List<OresSnapshot.Dimension> dimensions = snapshot.dimensionsOf(featureId);
		if (dimensions.size() > 1 && differsBetween(spec, resolved, dimensions)) {
			for (OresSnapshot.Dimension dimension : dimensions) {
				Resolved other = HeightModel.resolve(spec, dimension.bottomY(), dimension.topY());
				tooltip.add(Text.translatable("emi_ores.distribution.in_dimension", OresTexts.dimensionName(dimension.id()), other.min(), other.max()).formatted(Formatting.DARK_GRAY));
			}
		}
		if (hasAnyRelative(spec)) {
			tooltip.add(Text.translatable(useResolved ? "emi_ores.distribution.shift.relative" : "emi_ores.distribution.shift.dimension").formatted(Formatting.GRAY));
		}

		Integer player = OresSettings.showPlayerHeight ? playerHeight(page) : null;
		if (player != null) {
			tooltip.add(playerLine(resolved, player).formatted(Formatting.GOLD));
		}
		return tooltip.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
	}

	private static MutableText playerLine(Resolved resolved, int y) {
		return switch (resolved.position(y)) {
			case AT_PEAK -> Text.translatable(resolved.shape() == Shape.UNIFORM ? "emi_ores.distribution.you.in_range" : "emi_ores.distribution.you.peak", y);
			case IN_RANGE -> Text.translatable("emi_ores.distribution.you.in_range", y);
			case BELOW -> Text.translatable("emi_ores.distribution.you.below", y, resolved.min() - y);
			case ABOVE -> Text.translatable("emi_ores.distribution.you.above", y, y - resolved.max());
		};
	}

	private static boolean differsBetween(Spec spec, Resolved reference, List<OresSnapshot.Dimension> dimensions) {
		for (OresSnapshot.Dimension dimension : dimensions) {
			Resolved other = HeightModel.resolve(spec, dimension.bottomY(), dimension.topY());
			if (other.min() != reference.min() || other.max() != reference.max()) {
				return true;
			}
		}
		return false;
	}

	private static Text anchorTextLong(Anchor anchor, Page page, boolean resolved) {
		return anchorText(anchor, page, resolved, true).copy().formatted(Formatting.WHITE);
	}

	protected static Text getVeinFreqComponent(int countMin, int countMax, int rarityChance) {
		Text veinFreq;
		if (countMin != -1 && countMax != -1) {
			if (countMin == countMax) {
				veinFreq = Text.translatable("emi_ores.veins_per_chunk", countMin);
			} else {
				veinFreq = Text.translatable("emi_ores.veins_per_chunk_range", countMin, countMax);
			}
		} else if (rarityChance != -1) {
			veinFreq = Text.translatable("emi_ores.rarity_chance", rarityChance);
		} else {
			veinFreq = null;
		}
		return veinFreq;
	}

	private static boolean useResolvedHeights() {
		return Screen.hasShiftDown() != OresSettings.showHeightValuesForCurrentDimensionByDefault;
	}

	private static boolean hasAnyRelative(Spec spec) {
		return spec.min().kind() != Kind.FIXED || spec.max().kind() != Kind.FIXED;
	}
}
