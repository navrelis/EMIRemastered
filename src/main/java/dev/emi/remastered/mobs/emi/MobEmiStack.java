package dev.emi.remastered.mobs.emi;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import dev.emi.remastered.mobs.Mobs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.ComponentChanges;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * A mob as EMI stack, for the mobs that have no spawn egg (ender dragon, wither, giant, illusioner, armor stand, ...) and so no item that
 * leads to their mob page. It is drawn as the mob's portrait and is the output of the page, so that "recipes" of it
 * opens the page and the name finds it in the index. Its own key keeps it apart from every item. Client only.
 */
public class MobEmiStack extends EmiStack {
	/** Side of the square the mob is fitted into inside the 16x16 stack area. */
	private static final int ICON_BOX = 14;
	private static final EmiStack FALLBACK_ICON = EmiStack.of(Items.ZOMBIE_HEAD);

	private final EntityType<?> type;
	private final Identifier typeId;
	/** The entity and its rendering, made on first use on the render thread and shared by the copies. */
	private final Portrait portrait;

	/** The key of a mob stack, so that other code can recognise one without this class's constructor. */
	public record Key(Identifier type) {
	}

	private static final class Portrait {
		private boolean built;
		private @Nullable EmiStack stack;
	}

	private MobEmiStack(EntityType<?> type, Identifier typeId, Portrait portrait) {
		this.type = type;
		this.typeId = typeId;
		this.portrait = portrait;
	}

	/** Whether the mob has a page: living entities with attributes; the player type has them but cannot be created. */
	public static boolean hasPage(EntityType<?> type) {
		return type != EntityType.PLAYER && DefaultAttributeRegistry.hasDefinitionFor(type);
	}

	/** The stack of a mob that has a page, EMPTY for anything else. */
	public static EmiStack of(EntityType<?> type) {
		return hasPage(type) ? new MobEmiStack(type, Registries.ENTITY_TYPE.getId(type), new Portrait()) : EmiStack.EMPTY;
	}

	public static EmiStack of(Identifier typeId) {
		return Registries.ENTITY_TYPE.getOrEmpty(typeId).map(MobEmiStack::of).orElse(EmiStack.EMPTY);
	}

	public EntityType<?> getType() {
		return type;
	}

	@Override
	public EmiStack copy() {
		EmiStack e = new MobEmiStack(type, typeId, portrait);
		e.setAmount(getAmount());
		e.setChance(getChance());
		e.setRemainder(getRemainder().copy());
		e.comparison(comparison);
		return e;
	}

	@Override
	public void render(DrawContext gui, int x, int y, float delta, int flags) {
		if ((flags & RENDER_ICON) != 0) {
			icon().render(gui, x, y, delta, RENDER_ICON);
		}
	}

	private EmiStack icon() {
		if (!portrait.built) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null) {
				return FALLBACK_ICON;
			}
			portrait.built = true;
			try {
				Entity entity = type.create(client.world);
				if (entity instanceof SlimeEntity slime) {
					slime.setSize(5, false);
				}
				portrait.stack = entity == null ? null : MobInfoRecipe.fitted(entity, ICON_BOX);
			} catch (RuntimeException e) {
				Mobs.LOGGER.warn("Could not create {} for its index icon: {}", typeId, e.toString());
				portrait.stack = null;
			}
		}
		return portrait.stack == null ? FALLBACK_ICON : portrait.stack;
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
		return new Key(typeId);
	}

	@Override
	public Identifier getId() {
		return typeId;
	}

	@Override
	public List<Text> getTooltipText() {
		// Line 0 is the name; the following lines are what EMI searches with the $ prefix, so $monster finds monsters
		return List.of(getName(), groupText());
	}

	private Text groupText() {
		return Text.translatable("emi_mobs.group." + type.getSpawnGroup().getName());
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		List<TooltipComponent> list = new ArrayList<>();
		list.add(TooltipComponent.of(getName().asOrderedText()));
		if (MinecraftClient.getInstance().options.advancedItemTooltips) {
			list.add(TooltipComponent.of(Text.literal(typeId.toString()).formatted(Formatting.DARK_GRAY).asOrderedText()));
		}
		list.add(TooltipComponent.of(groupText().copy().formatted(Formatting.GRAY).asOrderedText()));
		EmiTooltipComponents.appendModName(list, typeId.getNamespace());
		list.addAll(super.getTooltip());
		return list;
	}

	@Override
	public Text getName() {
		return type.getName();
	}

	/**
	 * Serializer type {@code emi_mobs:mob}. EMI dispatches the short string form ({@code type:namespace:path}) by the text
	 * before the first colon, which can never find a type that itself contains a colon, so mob stacks are always written
	 * in the object form ({@code {"type":"emi_mobs:mob","id":"minecraft:ender_dragon"}}), which round-trips. The type is
	 * namespaced because serializer types are global strings.
	 */
	public static class Serializer implements EmiStackSerializer<MobEmiStack> {
		@Override
		public String getType() {
			return "emi_mobs:mob";
		}

		@Override
		public JsonElement serialize(MobEmiStack stack) {
			JsonObject json = new JsonObject();
			json.addProperty("type", getType());
			json.addProperty("id", stack.getId().toString());
			return json;
		}

		@Override
		public EmiStack create(Identifier id, ComponentChanges componentChanges, long amount) {
			EmiStack stack = of(id);
			return stack.isEmpty() ? stack : stack.setAmount(amount);
		}
	}
}
