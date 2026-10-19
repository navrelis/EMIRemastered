package dev.emi.remastered.stats.client;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.effects.EffectEmiStack;
import dev.emi.remastered.module.Modules;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;

/** Shared number and effect formatting of the {@code stats} module (client only). */
final class StatsText {
	private static final float TICK_RATE = 20.0f;

	private StatsText() {
	}

	/** Number with at most two decimals, grouped like the rest of EMI. */
	static String number(double value) {
		return EmiRenderHelper.TEXT_FORMAT.format(value);
	}

	static Text percent(double fraction) {
		return EmiPort.translatable("emi_stats.percent", number(fraction * 100));
	}

	/** {@code Regeneration II (0:05)}, as vanilla names a potion effect. */
	static Text effectLabel(StatusEffectInstance instance) {
		Text name = instance.getEffectType().value().getName();
		int amplifier = instance.getAmplifier();
		if (amplifier > 0) {
			String key = "potion.potency." + amplifier;
			Text level = I18n.hasTranslation(key) ? EmiPort.translatable(key) : EmiPort.literal(String.valueOf(amplifier + 1));
			name = EmiPort.translatable("potion.withAmplifier", name, level);
		}
		if (instance.getDuration() > 20 || instance.isInfinite()) {
			name = EmiPort.translatable("potion.withDuration", name, StatusEffectUtil.getDurationText(instance, 1.0f, TICK_RATE));
		}
		return name;
	}

	/** Whether effects can be shown as EMI stacks, which needs the effects module. */
	static boolean effectStacks() {
		return Modules.EFFECTS.isEnabled();
	}

	/** The effect as EMI stack (only call while {@link #effectStacks()}), with the chance as slot chance. */
	static EmiStack effectStack(StatusEffectInstance instance, float chance) {
		StatusEffect effect = instance.getEffectType().value();
		return new EffectEmiStack(effect).setChance(chance);
	}
}
