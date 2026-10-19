package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.ComponentChanges;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * A status effect as EMI stack (EMIffect's {@code MobEffectEmiStack}). Rendered with the effect's HUD sprite.
 * Serializer type {@code emiffect:effect}, see {@link Serializer}.
 */
public class EffectEmiStack extends EmiStack {
	private static final Identifier MISSING = Identifier.of("emiffect", "missingno");
	@Nullable
	private final StatusEffect effect;

	public EffectEmiStack(@Nullable StatusEffect effect) {
		this.effect = effect;
	}

	@Nullable
	public StatusEffect getEffect() {
		return effect;
	}

	@Override
	public EmiStack copy() {
		return new EffectEmiStack(effect);
	}

	@Override
	public void render(DrawContext draw, int x, int y, float delta, int flags) {
		if (effect == null) {
			return;
		}
		RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
		Sprite sprite = MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(entry);
		// the HUD sprites are 18x18 including their border, a slot content is 16x16
		draw.drawSprite(x - 1, y - 1, 0, 18, 18, sprite);
	}

	@Override
	public boolean isEmpty() {
		return effect == null;
	}

	@Override
	public ComponentChanges getComponentChanges() {
		return ComponentChanges.EMPTY;
	}

	@Override
	public Object getKey() {
		return effect == null ? MISSING : effect;
	}

	@Override
	public Identifier getId() {
		if (effect == null) {
			return MISSING;
		}
		return Registries.STATUS_EFFECT.getId(effect);
	}

	@Override
	public List<Text> getTooltipText() {
		return List.of(getName());
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		if (effect == null) {
			return List.of();
		}
		List<TooltipComponent> tooltip = new ArrayList<>();
		tooltip.add(TooltipComponent.of(EmiPort.ordered(getName())));
		MutableText category = switch (effect.getCategory()) {
			case BENEFICIAL -> EmiPort.translatable("tooltip.emiffect.beneficial").formatted(Formatting.GREEN);
			case NEUTRAL -> EmiPort.translatable("tooltip.emiffect.neutral").formatted(Formatting.GOLD);
			case HARMFUL -> EmiPort.translatable("tooltip.emiffect.harmful").formatted(Formatting.RED);
		};
		tooltip.add(TooltipComponent.of(EmiPort.ordered(category)));
		int color = effect.getColor() & 0xFFFFFF;
		MutableText colorLine = EmiPort.translatable("tooltip.emiffect.color", String.format(Locale.ROOT, "#%06X", color))
			.formatted(Formatting.GRAY)
			.append(" ")
			.append(EmiPort.literal("█").styled(style -> style.withColor(TextColor.fromRgb(color))));
		tooltip.add(TooltipComponent.of(EmiPort.ordered(colorLine)));
		tooltip.add(TooltipComponent.of(EmiPort.ordered(EmiPort.literal(
			EmiUtil.getModName(getId().getNamespace()), Formatting.BLUE, Formatting.ITALIC))));
		tooltip.addAll(super.getTooltip());
		return tooltip;
	}

	@Override
	public Text getName() {
		return effect != null ? effect.getName() : EmiPort.literal("missingno");
	}

	/**
	 * Serializer type {@code emiffect:effect}, as in EMIffect 2.1.5. EMI dispatches the short string form
	 * ({@code type:namespace:path}) by the text before the first colon, which can never find a type that itself
	 * contains a colon, so effects are always written in the object form
	 * ({@code {"type":"emiffect:effect","id":"minecraft:speed"}}), which round-trips. Strings written by older
	 * versions are read through {@link Alias}.
	 */
	public static class Serializer implements EmiStackSerializer<EffectEmiStack> {

		@Override
		public EmiStack create(Identifier id, ComponentChanges componentChanges, long amount) {
			return of(id);
		}

		@Override
		public String getType() {
			return "emiffect:effect";
		}

		@Override
		public JsonElement serialize(EffectEmiStack stack) {
			JsonObject json = new JsonObject();
			json.addProperty("type", getType());
			json.addProperty("id", stack.getId().toString());
			return json;
		}
	}

	/** Read-only serializer for the short string spellings ({@code emiffect:...} and {@code mob_effect:...}). */
	public static class Alias implements EmiStackSerializer<Legacy> {
		private final String type;

		public Alias(String type) {
			this.type = type;
		}

		@Override
		public EmiStack create(Identifier id, ComponentChanges componentChanges, long amount) {
			return of(id);
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public EmiIngredient deserialize(JsonElement element) {
			if (element instanceof JsonPrimitive primitive && primitive.isString()) {
				String id = EffectFormat.parseStackString(primitive.getAsString());
				Identifier identifier = id == null ? null : Identifier.tryParse(id);
				return identifier == null ? EmiStack.EMPTY : of(identifier);
			}
			return EmiStackSerializer.super.deserialize(element);
		}

		@Override
		public JsonElement serialize(Legacy stack) {
			throw new UnsupportedOperationException("read-only alias");
		}
	}

	/** Class key of the alias serializers; never instantiated. */
	public static final class Legacy extends EffectEmiStack {
		private Legacy() {
			super(null);
		}
	}

	private static EmiStack of(Identifier id) {
		StatusEffect effect = Registries.STATUS_EFFECT.get(id);
		return effect != null ? new EffectEmiStack(effect) : EmiStack.EMPTY;
	}
}
