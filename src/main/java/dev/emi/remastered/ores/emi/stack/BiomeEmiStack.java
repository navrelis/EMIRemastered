/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.emi.stack;

import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import dev.emi.remastered.ores.Ores;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentChanges;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class BiomeEmiStack extends EmiStack {
	private static final Identifier MISSING_SPRITE_ID = Ores.id("biome/missing");

	private final Biome biome;
	private final Sprite sprite;

	private BiomeEmiStack(Biome biome) {
		this.biome = biome;

		SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
		Identifier id = getId();

		Sprite sprite;
		if (id == null) {
			sprite = atlas.getSprite(MISSING_SPRITE_ID);
		} else {
			sprite = atlas.getSprite(id.withPrefixedPath("biome/"));

			if (MissingSprite.getMissingSpriteId().equals(sprite.getContents().getId())) {
				sprite = atlas.getSprite(MISSING_SPRITE_ID);
			}
		}

		this.sprite = sprite;
	}

	public static EmiStack of(Biome biome, ComponentChanges componentChanges, long amount) {
		return new BiomeEmiStack(biome).setAmount(amount);
	}

	public static EmiStack of(Biome biome) {
		return BiomeEmiStack.of(biome, ComponentChanges.EMPTY, 1);
	}

	@Override
	public EmiStack copy() {
		EmiStack e = new BiomeEmiStack(biome);
		e.setAmount(getAmount());
		e.setChance(getChance());
		e.setRemainder(getRemainder().copy());
		e.comparison(comparison);
		return e;
	}

	@Override
	public void render(DrawContext gui, int x, int y, float delta, int flags) {
		if ((flags & RENDER_ICON) != 0) {
			MatrixStack pose = gui.getMatrices();
			pose.push();
			pose.translate(0, 0, 150);

			gui.drawSprite(x, y, 0, 16, 16, sprite);

			pose.pop();
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public ComponentChanges getComponentChanges() {
		return ComponentChanges.EMPTY;
	}

	@Override
	public Object getKey() {
		return biome;
	}

	@Override
	public Identifier getId() {
		return getBiomeRegistry().getId(biome);
	}

	@Override
	public List<Text> getTooltipText() {
		return null;
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		Identifier id = getId();
		List<TooltipComponent> list = new ArrayList<>();
		list.add(TooltipComponent.of(getName().asOrderedText()));
		if (id != null) {
			if (MinecraftClient.getInstance().options.advancedItemTooltips) {
				list.add(TooltipComponent.of(Text.literal(id.toString()).formatted(Formatting.DARK_GRAY).asOrderedText()));
			}
			EmiTooltipComponents.appendModName(list, id.getNamespace());
		}
		list.addAll(super.getTooltip());
		return list;
	}

	@Override
	public Text getName() {
		Identifier id = getId();
		return id == null ? Text.empty() : Text.translatable(id.toTranslationKey("biome"));
	}

	private static Registry<Biome> getBiomeRegistry() {
		return MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
	}

	// The type id is saved in players' favourites, keep it
	public static class Serializer implements EmiStackSerializer<BiomeEmiStack> {
		@Override
		public String getType() {
			return "biome";
		}

		@Override
		public EmiStack create(Identifier id, ComponentChanges componentChanges, long amount) {
			Biome biome = getBiomeRegistry().get(id);
			if (biome == null) {
				return EmiStack.EMPTY;
			}
			return BiomeEmiStack.of(biome, componentChanges, amount);
		}
	}
}
