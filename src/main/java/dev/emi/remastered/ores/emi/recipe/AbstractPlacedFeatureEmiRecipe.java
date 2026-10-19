/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi.recipe;

import dev.emi.remastered.ores.Ores;
import dev.emi.remastered.ores.client.OresFeatures;
import dev.emi.remastered.ores.OresSettings;
import dev.emi.remastered.ores.mixin.accessor.TrapezoidHeightAccessor;
import dev.emi.remastered.ores.mixin.accessor.UniformHeightAccessor;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
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

	private static final Identifier DISTRIBUTION = Ores.id("textures/gui/distribution.png");

	private static Text anchorText(YOffset anchor) {
		String s;
		if (anchor instanceof YOffset.Fixed absolute) {
			s = String.valueOf(absolute.y());
		} else if (anchor instanceof YOffset.AboveBottom aboveBottom) {
			int offset = aboveBottom.offset();
			if (useCurrentDimension()) {
				int height = MinecraftClient.getInstance().world.getBottomY() + offset;
				s = String.valueOf(height);
			} else {
				if (offset == 0) {
					s = "bot";
				} else if (offset > 0) {
					s = "bot+" + offset;
				} else {
					s = "bot" + offset;
				}
			}
		} else if (anchor instanceof YOffset.BelowTop belowTop) {
			int offset = -belowTop.offset();
			if (useCurrentDimension()) {
				int height = MinecraftClient.getInstance().world.getTopY() + offset;
				s = String.valueOf(height);
			} else {
				if (offset == 0) {
					s = "top";
				} else if (offset > 0) {
					s = "top+" + offset;
				} else {
					s = "top" + offset;
				}
			}
		} else {
			throw new RuntimeException();
		}
		return Text.literal(s);
	}

	private static Text anchorTextLong(YOffset anchor) {
		return anchorTextLongInner(anchor).formatted(Formatting.WHITE);
	}

	private static MutableText anchorTextLongInner(YOffset anchor) {
		if (anchor instanceof YOffset.Fixed absolute) {
			return Text.literal(String.valueOf(absolute.y()));
		} else if (anchor instanceof YOffset.AboveBottom aboveBottom) {
			int offset = aboveBottom.offset();
			if (useCurrentDimension()) {
				int height = MinecraftClient.getInstance().world.getBottomY() + offset;
				return Text.literal(String.valueOf(height));
			} else {
				if (offset == 0) {
					return Text.translatable("emi_ores.distribution.anchor.bottom");
				} else if (offset > 0) {
					return Text.translatable("emi_ores.distribution.anchor.above_bottom", offset);
				} else {
					return Text.translatable("emi_ores.distribution.anchor.below_bottom", -offset);
				}
			}
		} else if (anchor instanceof YOffset.BelowTop belowTop) {
			int offset = -belowTop.offset();
			if (useCurrentDimension()) {
				int height = MinecraftClient.getInstance().world.getTopY() + offset;
				return Text.literal(String.valueOf(height));
			} else {
				if (offset == 0) {
					return Text.translatable("emi_ores.distribution.anchor.top");
				} else if (offset > 0) {
					return Text.translatable("emi_ores.distribution.anchor.above_top", offset);
				} else {
					return Text.translatable("emi_ores.distribution.anchor.below_top", -offset);
				}
			}
		} else {
			throw new RuntimeException();
		}
	}

	protected static List<Biome> getBiomes(Identifier id, PlacedFeature feature) {
		Registry<Biome> biomeRegistry = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
		return OresFeatures.getBiomes()
				.get(RegistryKey.of(RegistryKeys.PLACED_FEATURE, id))
				.stream()
				.map(biomeRegistry::get)
				.toList();
	}
	
	private static void addAnchorText(WidgetHolder widgets, YOffset anchor, int x, int y, TextWidget.Alignment verticalAlign, TextWidget.Alignment horizontalAlign) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		
		widgets.addDrawable(x, y, 0, 0, (gui, mouseX, mouseY, delta) -> {
			Text text = anchorText(anchor);
			int textWidth = font.getWidth(text);
			int textHeight = font.fontHeight;
			int textX = switch (horizontalAlign) {
				case START -> 0;
				case CENTER -> -textWidth/2;
				case END -> -textWidth;
			};
			int textY = switch (verticalAlign) {
				case START -> 0;
				case CENTER -> -textHeight/2;
				case END -> -textHeight;
			};
			gui.drawText(MinecraftClient.getInstance().textRenderer, text, textX, textY, 0, false);
		});
	}

	protected static void addDistributionGraph(WidgetHolder widgets, int x, int y, HeightProvider heightProvider) {
		if (heightProvider == null) return;

		HeightProviderType type;
		YOffset min, max, midLow, midHigh;

		if (heightProvider instanceof UniformHeightProvider uniform) {
			type = HeightProviderType.UNIFORM;
			UniformHeightAccessor accessor = (UniformHeightAccessor) uniform;
			min = accessor.emi_ores$getMinInclusive();
			max = accessor.emi_ores$getMaxInclusive();
			midLow = midHigh = null;
		} else if (heightProvider instanceof TrapezoidHeightProvider trapezoid) {
			TrapezoidHeightAccessor accessor = (TrapezoidHeightAccessor) trapezoid;
			min = accessor.emi_ores$getMinInclusive();
			max = accessor.emi_ores$getMaxInclusive();

			int plateau = accessor.emi_ores$getPlateau();

			// if the min and max are the same type, we can calculate the y-level with the highest frequency
			if (min instanceof YOffset.Fixed minAbs && max instanceof YOffset.Fixed maxAbs) {
				midLow = YOffset.fixed((minAbs.y() + maxAbs.y() - plateau) / 2);
				midHigh = YOffset.fixed((minAbs.y() + maxAbs.y() + plateau) / 2);
			} else if (min instanceof YOffset.AboveBottom minBot && max instanceof YOffset.AboveBottom maxBot) {
				midLow = YOffset.aboveBottom((minBot.offset() + maxBot.offset() - plateau) / 2);
				midHigh = YOffset.aboveBottom((minBot.offset() + maxBot.offset() + plateau) / 2);
			} else if (min instanceof YOffset.BelowTop minTop && max instanceof YOffset.BelowTop maxTop) {
				midLow = YOffset.belowTop((minTop.offset() + maxTop.offset() - plateau) / 2);
				midHigh = YOffset.belowTop((minTop.offset() + maxTop.offset() + plateau) / 2);
			} else {
				midLow = midHigh = null;
			}

			if (plateau == 0) {
				type = HeightProviderType.TRIANGULAR;
			} else {
				type = HeightProviderType.TRAPEZOID;
			}
		} else {
			type = null;
			min = max = midLow = midHigh = null;
		}

		if (type != null && min != null && max != null) {
			widgets.addTexture(DISTRIBUTION, x, y, 32, 16, 0, type.v);
			widgets.addTooltip((mouseX, mouseY) -> getDistributionGraphTooltip(type, min, max, midLow, midHigh), x, y, 32, 16);
			addAnchorText(widgets, min, x, y+8, TextWidget.Alignment.CENTER, TextWidget.Alignment.END);
			addAnchorText(widgets, max, x+32, y+8, TextWidget.Alignment.CENTER, TextWidget.Alignment.START);
			if (type == HeightProviderType.TRIANGULAR && midLow != null) {
				addAnchorText(widgets, midLow, 80, 8, TextWidget.Alignment.CENTER, TextWidget.Alignment.CENTER);
			}
		}
	}

	private static List<TooltipComponent> getDistributionGraphTooltip(HeightProviderType type, YOffset min, YOffset max, YOffset midLow, YOffset midHigh) {
		List<Text> tooltip = new ArrayList<>();

		tooltip.add(type.name);
		tooltip.add(Text.translatable("emi_ores.distribution.range", anchorTextLong(min), anchorTextLong(max)).formatted(Formatting.GRAY));
		if (midLow != null && midHigh != null) {
			if (midLow.equals(midHigh)) {
				tooltip.add(Text.translatable("emi_ores.distribution.middle", anchorTextLong(midLow)).formatted(Formatting.GRAY));
			} else {
				tooltip.add(Text.translatable("emi_ores.distribution.middle_range", anchorTextLong(midLow), anchorTextLong(midHigh)).formatted(Formatting.GRAY));
			}
		}
		if (hasAnyRelative(min, max, midLow, midHigh)) {
			if (useCurrentDimension()) {
				tooltip.add(Text.translatable("emi_ores.distribution.dimension", MinecraftClient.getInstance().world.getRegistryKey().getValue()).formatted(Formatting.GRAY));
				if (!Screen.hasShiftDown()) tooltip.add(Text.translatable("emi_ores.distribution.shift.relative").formatted(Formatting.GRAY));
			} else if (!Screen.hasShiftDown()) {
				tooltip.add(Text.translatable("emi_ores.distribution.shift.dimension").formatted(Formatting.GRAY));
			}
		}
		return tooltip.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
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

	private static boolean useCurrentDimension() {
		return Screen.hasShiftDown() != OresSettings.showHeightValuesForCurrentDimensionByDefault;
	}
	
	private static boolean hasAnyRelative(YOffset... anchors) {
		for (YOffset anchor : anchors) {
			if (anchor instanceof YOffset.AboveBottom || anchor instanceof YOffset.BelowTop) {
				return true;
			}
		}
		return false;
	}

	protected enum HeightProviderType {
		UNIFORM(0, Text.translatable("emi_ores.distribution.uniform").formatted(Formatting.BLUE)),
		TRIANGULAR(16, Text.translatable("emi_ores.distribution.triangle").formatted(Formatting.GREEN)),
		TRAPEZOID(32, Text.translatable("emi_ores.distribution.trapezoid").formatted(Formatting.RED));

		public final int v;
		public final Text name;

		HeightProviderType(int v, Text name) {
			this.v = v;
			this.name = name;
		}
	}
}
