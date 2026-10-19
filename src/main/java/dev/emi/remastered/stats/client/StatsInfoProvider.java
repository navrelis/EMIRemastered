package dev.emi.remastered.stats.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoElement;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.stats.StatsSettings;
import dev.emi.remastered.stats.client.FoodFacts.EffectFact;
import dev.emi.remastered.stats.core.StatsMath;
import dev.emi.remastered.stats.core.StatsMath.Mod;
import dev.emi.remastered.stats.core.StatsMath.Op;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Item Info provider of the {@code stats} module: Food, Tool, Weapon and Armour sections in the Details part of the
 * page. All values come from the stack's data components and attribute modifiers, so modded items work too.
 */
public final class StatsInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Identifier.of("emi_stats", "stats");
	private static final Identifier FOOD_ID = Identifier.of("emi_stats", "food");
	private static final Identifier TOOL_ID = Identifier.of("emi_stats", "tool");
	private static final Identifier WEAPON_ID = Identifier.of("emi_stats", "weapon");
	private static final Identifier ARMOUR_ID = Identifier.of("emi_stats", "armour");

	private static final int TEXT = 0x404040;
	private static final int SATURATION_COLOR = 0xE3A41E;
	private static final int ARMOUR_COLOR = 0x5C6F82;
	private static final int TOUGHNESS_COLOR = 0x3E4C5C;
	private static final int KNOCKBACK_COLOR = 0x8A5A3C;
	private static final int MAX_RULE_LINES = 6;
	private static final int MAX_BLOCK_NAMES = 3;

	private static final Set<AttributeModifierSlot> HAND_SLOTS = EnumSet.of(AttributeModifierSlot.MAINHAND, AttributeModifierSlot.HAND,
		AttributeModifierSlot.ANY);
	private static final Set<AttributeModifierSlot> ARMOUR_SLOTS = EnumSet.of(AttributeModifierSlot.HEAD, AttributeModifierSlot.CHEST,
		AttributeModifierSlot.LEGS, AttributeModifierSlot.FEET, AttributeModifierSlot.ARMOR, AttributeModifierSlot.BODY);

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "stats";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return StatsSettings.infoSections && stack.getKey() instanceof Item;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		ItemStack item = stack.getItemStack();
		if (item.isEmpty()) {
			return;
		}
		food(stack, item, collector);
		tool(stack, item, collector);
		weapon(stack, item, collector);
		armour(stack, item, collector);
	}

	private static void food(EmiStack stack, ItemStack item, InfoCollector collector) {
		FoodFacts food = FoodFacts.of(item);
		if (food == null) {
			return;
		}
		InfoSection section = collector.addSection(FOOD_ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_stats.section.food"))
			.icon(EmiStack.of(Items.COOKED_BEEF));
		section.element(new HungerElement(food.nutrition()));
		section.bar(EmiPort.translatable("emi_stats.food.saturation", StatsText.number(StatsMath.round2(food.saturation()))),
			StatsMath.fraction(food.saturation(), StatsMath.MAX_FOOD), SATURATION_COLOR, null);
		section.text(EmiPort.translatable("emi_stats.food.eat_time", StatsText.number(StatsMath.round2(food.eatSeconds()))));
		section.text(food.canAlwaysEat()
			? EmiPort.translatable("emi_stats.food.always").formatted(Formatting.DARK_GREEN)
			: EmiPort.translatable("emi_stats.food.hungry_only"));
		if (!food.convertsTo().isEmpty()) {
			section.stacks(EmiPort.translatable("emi_stats.food.converts"), List.of(EmiStack.of(food.convertsTo())));
		}
		boolean stacks = StatsText.effectStacks();
		for (EffectFact effect : food.effects()) {
			EmiStack icon = stacks ? StatsText.effectStack(effect.instance(), 1.0f) : EmiStack.of(Items.POTION);
			section.entry(icon, StatsText.effectLabel(effect.instance()))
				.detail(StatsText.percent(effect.chance()))
				.link(stacks ? InfoLink.recipesOf(icon, null) : null);
		}
		if (StatsEmi.foodCategoryRegistered) {
			section.link(EmiPort.translatable("emi_stats.food.open"), InfoLink.usesOf(stack, StatsEmi.FOOD));
		}
	}

	private static void tool(EmiStack stack, ItemStack item, InfoCollector collector) {
		ToolComponent tool = item.get(DataComponentTypes.TOOL);
		if (tool == null) {
			return;
		}
		InfoSection section = collector.addSection(TOOL_ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_stats.section.tool"))
			.icon(stack);
		List<String> denied = new ArrayList<>();
		int lines = 0;
		for (ToolComponent.Rule rule : tool.rules()) {
			Optional<TagKey<Block>> tag = rule.blocks().getTagKey();
			if (tag.isPresent() && rule.correctForDrops().isPresent() && !rule.correctForDrops().get()) {
				denied.add(tag.get().id().toString());
			}
			if (rule.speed().isPresent() && lines < MAX_RULE_LINES) {
				section.text(EmiPort.translatable("emi_stats.tool.speed_on", targetLabel(rule.blocks()),
					StatsText.number(StatsMath.round2(rule.speed().get()))));
				lines++;
			}
		}
		section.text(EmiPort.translatable("emi_stats.tool.speed_default", StatsText.number(StatsMath.round2(tool.defaultMiningSpeed()))));
		for (String tag : denied) {
			Optional<String> tier = StatsMath.tierOfIncorrectTag(tag);
			if (tier.isPresent()) {
				String key = "emi_stats.tier." + tier.get();
				Text name = I18n.hasTranslation(key) ? EmiPort.translatable(key) : EmiPort.literal(StatsMath.humanize(tier.get()));
				section.text(EmiPort.translatable("emi_stats.tool.tier", name), List.of(EmiPort.translatable("emi_stats.tool.tier.tooltip", tag)));
				break;
			}
		}
		section.text(EmiPort.translatable("emi_stats.tool.wear", StatsText.number(tool.damagePerBlock())));
		wear(section, item);
	}

	/** Durability, enchantability and repair material, shared by tools, weapons and armour. */
	private static void wear(InfoSection section, ItemStack item) {
		int durability = item.getMaxDamage();
		if (durability > 0) {
			section.text(EmiPort.translatable("emi_stats.tool.durability", StatsText.number(durability)));
		}
		int enchantability = item.getItem().getEnchantability();
		if (enchantability > 0) {
			section.text(EmiPort.translatable("emi_stats.tool.enchantability", StatsText.number(enchantability)));
		}
		Ingredient repair = repairIngredient(item.getItem());
		if (repair != null) {
			EmiIngredient ingredient = EmiIngredient.of(repair);
			if (!ingredient.isEmpty()) {
				section.stacks(EmiPort.translatable("emi_stats.tool.repair"), List.of(ingredient));
			}
		}
	}

	@Nullable
	private static Ingredient repairIngredient(Item item) {
		if (item instanceof ToolItem tool) {
			return tool.getMaterial().getRepairIngredient();
		}
		if (item instanceof ArmorItem armour) {
			return armour.getMaterial().value().repairIngredient().get();
		}
		return null;
	}

	private static void weapon(EmiStack stack, ItemStack item, InfoCollector collector) {
		AttributeModifiersComponent modifiers = item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		List<Mod> damageMods = collect(modifiers, EntityAttributes.GENERIC_ATTACK_DAMAGE, HAND_SLOTS);
		if (damageMods.isEmpty()) {
			return;
		}
		double damage = StatsMath.apply(StatsMath.PLAYER_ATTACK_DAMAGE, damageMods);
		double speed = StatsMath.apply(StatsMath.PLAYER_ATTACK_SPEED, collect(modifiers, EntityAttributes.GENERIC_ATTACK_SPEED, HAND_SLOTS));
		InfoSection section = collector.addSection(WEAPON_ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_stats.section.weapon"))
			.icon(stack);
		section.text(EmiPort.translatable("emi_stats.weapon.damage", StatsText.number(StatsMath.round2(damage))),
			List.of(EmiPort.translatable("emi_stats.weapon.damage.tooltip")));
		section.text(EmiPort.translatable("emi_stats.weapon.speed", StatsText.number(StatsMath.round2(speed))),
			List.of(EmiPort.translatable("emi_stats.weapon.speed.tooltip")));
		if (speed > 0) {
			section.text(EmiPort.translatable("emi_stats.weapon.cooldown", StatsText.number(StatsMath.round2(StatsMath.cooldownSeconds(speed)))));
		}
		section.text(EmiPort.translatable("emi_stats.weapon.dps", StatsText.number(StatsMath.dps(damage, speed))),
			List.of(EmiPort.translatable("emi_stats.weapon.dps.tooltip")));
		if (!item.contains(DataComponentTypes.TOOL)) {
			wear(section, item);
		}
	}

	private static void armour(EmiStack stack, ItemStack item, InfoCollector collector) {
		AttributeModifiersComponent modifiers = item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		List<Mod> armour = collect(modifiers, EntityAttributes.GENERIC_ARMOR, ARMOUR_SLOTS);
		List<Mod> toughness = collect(modifiers, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, ARMOUR_SLOTS);
		List<Mod> knockback = collect(modifiers, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, ARMOUR_SLOTS);
		if (armour.isEmpty() && toughness.isEmpty() && knockback.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ARMOUR_ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_stats.section.armour"))
			.icon(stack);
		AttributeModifierSlot slot = armourSlot(modifiers);
		if (slot != null) {
			String key = "emi_stats.slot." + slot.asString();
			Text name = I18n.hasTranslation(key) ? EmiPort.translatable(key) : EmiPort.literal(StatsMath.humanize(slot.asString()));
			section.text(EmiPort.translatable("emi_stats.armour.slot", name));
		}
		double armourValue = StatsMath.apply(0, armour);
		section.bar(EmiPort.translatable("emi_stats.armour.armour"), StatsMath.fraction(armourValue, 20), ARMOUR_COLOR,
			EmiPort.literal(StatsText.number(StatsMath.round2(armourValue))));
		if (!toughness.isEmpty()) {
			double value = StatsMath.apply(0, toughness);
			section.bar(EmiPort.translatable("emi_stats.armour.toughness"), StatsMath.fraction(value, 12), TOUGHNESS_COLOR,
				EmiPort.literal(StatsText.number(StatsMath.round2(value))));
		}
		if (!knockback.isEmpty()) {
			double value = StatsMath.apply(0, knockback);
			section.bar(EmiPort.translatable("emi_stats.armour.knockback"), StatsMath.fraction(value, 1), KNOCKBACK_COLOR,
				StatsText.percent(StatsMath.round2(value)));
		}
		wear(section, item);
	}

	@Nullable
	private static AttributeModifierSlot armourSlot(@Nullable AttributeModifiersComponent modifiers) {
		if (modifiers == null) {
			return null;
		}
		for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
			if (ARMOUR_SLOTS.contains(entry.slot())) {
				return entry.slot();
			}
		}
		return null;
	}

	/** The modifiers one attribute gets from the item in the given slots, in the pure maths' terms. */
	private static List<Mod> collect(@Nullable AttributeModifiersComponent component, RegistryEntry<EntityAttribute> attribute,
			Set<AttributeModifierSlot> slots) {
		List<Mod> mods = new ArrayList<>();
		if (component == null) {
			return mods;
		}
		for (AttributeModifiersComponent.Entry entry : component.modifiers()) {
			if (entry.attribute().matches(attribute) && slots.contains(entry.slot())) {
				EntityAttributeModifier modifier = entry.modifier();
				Op op = switch (modifier.operation()) {
					case ADD_VALUE -> Op.ADD_VALUE;
					case ADD_MULTIPLIED_BASE -> Op.ADD_MULTIPLIED_BASE;
					case ADD_MULTIPLIED_TOTAL -> Op.ADD_MULTIPLIED_TOTAL;
				};
				mods.add(new Mod(modifier.value(), op));
			}
		}
		return mods;
	}

	/** A mining rule's blocks: a friendly name for a vanilla tag, the tag id, or the first block names. */
	private static Text targetLabel(RegistryEntryList<Block> blocks) {
		Optional<TagKey<Block>> tag = blocks.getTagKey();
		if (tag.isPresent()) {
			Identifier id = tag.get().id();
			String key = StatsMath.targetKey(id.getNamespace(), id.getPath());
			return I18n.hasTranslation(key) ? EmiPort.translatable(key) : EmiPort.literal("#" + id);
		}
		MutableText names = EmiPort.literal("");
		int count = 0;
		int total = blocks.size();
		for (RegistryEntry<Block> block : blocks) {
			if (count == MAX_BLOCK_NAMES) {
				break;
			}
			if (count > 0) {
				names.append(", ");
			}
			names.append(block.value().getName());
			count++;
		}
		return total > count ? EmiPort.translatable("emi_stats.target.blocks.more", names, total - count) : names;
	}

	/** The hunger shanks of a food with their value, as a line of the page. */
	private record HungerElement(int nutrition) implements InfoElement {
		@Override
		public int getHeight(int width) {
			return HungerIcons.HEIGHT + 3;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			HungerIcons.drawShanks(draw, 0, 1, nutrition);
			draw.drawText(MinecraftClient.getInstance().textRenderer, EmiPort.translatable("emi_stats.food.hunger", StatsText.number(nutrition)),
				HungerIcons.WIDTH + 6, 2, TEXT, false);
		}

		@Override
		public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
			return List.of(TooltipComponent.of(EmiPort.translatable("emi_stats.food.hunger.tooltip", StatsText.number(nutrition),
				StatsText.number(nutrition / 2.0)).asOrderedText()));
		}
	}
}
