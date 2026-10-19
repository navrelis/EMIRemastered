package dev.emi.remastered.effects;

import java.util.Map;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiRegistryAdapter;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Client side of the {@code effects} module: the EMI registration. Never loaded on a dedicated server. */
final class EffectsEmi {
	static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(Identifier.of("emiffect", "status_effect_info"),
		new EmiTexture(Identifier.of("emiffect", "textures/gui/emi/icon.png"), 0, 0, 16, 16, 16, 16, 16, 16));

	private EffectsEmi() {
	}

	static void initialize(EmiInitRegistry registry) {
		registry.addIngredientSerializer(EffectEmiStack.class, new EffectEmiStack.Serializer());
		// the short string forms written by EMIffect 2.1.5 / 2.1.6 ("emiffect:effect:..." could never be read back)
		registry.addIngredientSerializer(EffectEmiStack.Legacy.class, new EffectEmiStack.Alias("emiffect"));
		registry.addIngredientSerializer(EffectEmiStack.Legacy.class, new EffectEmiStack.Alias("mob_effect"));
		registry.addRegistryAdapter(EmiRegistryAdapter.simple(StatusEffect.class, Registries.STATUS_EFFECT,
			(effect, changes, amount) -> new EffectEmiStack(effect)));
	}

	static void register(EmiRegistry registry) {
		long start = System.nanoTime();
		registry.addCategory(CATEGORY);

		Map<StatusEffect, EffectSources> index = EffectSourceCollector.collect();
		if (EffectsSettings.showHardcodedSources) {
			EffectSourceTable.apply(MinecraftClient.getInstance().getResourceManager(), index);
		}

		EffectSources none = new EffectSources();
		int effects = 0;
		int sources = 0;
		for (StatusEffect effect : Registries.STATUS_EFFECT) {
			EffectSources effectSources = index.getOrDefault(effect, none);
			EffectEmiStack stack = new EffectEmiStack(effect);
			registry.addRecipe(new EffectInfoRecipe(stack, effectSources));
			if (EffectsSettings.showInIndex) {
				registry.addEmiStack(stack);
			}
			effects++;
			sources += effectSources.count();
		}

		registry.addWorkstation(CATEGORY, EmiStack.of(Items.BEACON));
		registry.addWorkstation(CATEGORY, EmiStack.of(Items.POTION));
		registry.addWorkstation(CATEGORY, EmiStack.of(Items.SPLASH_POTION));
		registry.addWorkstation(CATEGORY, EmiStack.of(Items.LINGERING_POTION));
		registry.addWorkstation(CATEGORY, EmiStack.of(Items.TIPPED_ARROW));
		registry.addWorkstation(CATEGORY, EmiStack.of(Items.SUSPICIOUS_STEW));
		EmiLog.info("effects: " + effects + " effects, " + sources + " sources in " + (System.nanoTime() - start) / 1_000_000 + " ms");
	}
}
