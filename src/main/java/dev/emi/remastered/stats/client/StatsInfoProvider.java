package dev.emi.remastered.stats.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiTagKey;
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
import dev.emi.remastered.theme.TextColors;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.ItemTags;
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
	private static final TagKey<Item> MELEE_WEAPONS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "tools/melee_weapon"));
	private static final TagKey<Item> SPEARS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "tools/spear"));
	private static final EquipmentSlot[] ARMOUR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.BODY};

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

	/**
	 * The tool component when it says something about mining: at least one rule or a default speed other than the
	 * bare hand's 1. The mace has a tool component only for its durability cost, which is no Tool section.
	 */
	@Nullable
	private static ToolComponent miningTool(ItemStack item) {
		ToolComponent tool = item.get(DataComponentTypes.TOOL);
		if (tool == null || tool.rules().isEmpty() && tool.defaultMiningSpeed() == 1.0f) {
			return null;
		}
		return tool;
	}

	private static void tool(EmiStack stack, ItemStack item, InfoCollector collector) {
		ToolComponent tool = miningTool(item);
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
				Target target = target(rule.blocks());
				section.text(EmiPort.translatable("emi_stats.tool.speed_on", target.label(),
					StatsText.number(StatsMath.round2(rule.speed().get()))), target.tooltip());
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

	/**
	 * Melee weapons get the Weapon section: swords, axes, tridents and maces, anything in the {@code minecraft:swords},
	 * {@code minecraft:axes}, {@code c:tools/melee_weapon} or {@code c:tools/spear} item tags, and any other item
	 * with an attack damage modifier that is not a tool (modded weapons). Pickaxes, shovels, hoes and other tools
	 * carry the same attack modifiers but are not weapons, so they get the Tool section only. Axes count as weapons
	 * (they hit harder than swords).
	 */
	private static boolean isMeleeWeapon(ItemStack item) {
		Item type = item.getItem();
		if (type instanceof SwordItem || type instanceof AxeItem || type instanceof TridentItem || type instanceof MaceItem) {
			return true;
		}
		if (item.isIn(ItemTags.SWORDS) || item.isIn(ItemTags.AXES) || item.isIn(MELEE_WEAPONS) || item.isIn(SPEARS)) {
			return true;
		}
		return !(type instanceof ToolItem) && !item.contains(DataComponentTypes.TOOL);
	}

	private static void weapon(EmiStack stack, ItemStack item, InfoCollector collector) {
		if (!isMeleeWeapon(item)) {
			return;
		}
		List<Mod> damageMods = collect(item, EquipmentSlot.MAINHAND, EntityAttributes.GENERIC_ATTACK_DAMAGE);
		if (damageMods.isEmpty()) {
			return;
		}
		double damage = StatsMath.apply(StatsMath.PLAYER_ATTACK_DAMAGE, damageMods);
		double speed = StatsMath.apply(StatsMath.PLAYER_ATTACK_SPEED, collect(item, EquipmentSlot.MAINHAND, EntityAttributes.GENERIC_ATTACK_SPEED));
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
		if (miningTool(item) == null) {
			wear(section, item);
		}
	}

	private static void armour(EmiStack stack, ItemStack item, InfoCollector collector) {
		// the slot an armour piece is worn in; items that do not say it are tried in all armour slots
		EquipmentSlot[] slots = item.getItem() instanceof Equipment equipment ? new EquipmentSlot[] {equipment.getSlotType()} : ARMOUR_SLOTS;
		for (EquipmentSlot slot : slots) {
			List<Mod> armour = collect(item, slot, EntityAttributes.GENERIC_ARMOR);
			List<Mod> toughness = collect(item, slot, EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
			List<Mod> knockback = collect(item, slot, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
			double armourValue = StatsMath.apply(0, armour);
			double toughnessValue = StatsMath.apply(0, toughness);
			double knockbackValue = StatsMath.apply(0, knockback);
			if (armour.isEmpty() && toughnessValue == 0 && knockbackValue == 0) {
				continue;
			}
			InfoSection section = collector.addSection(ARMOUR_ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_stats.section.armour"))
				.icon(stack);
			if (item.getItem() instanceof Equipment) {
				String key = "emi_stats.slot." + slot.getName();
				Text name = I18n.hasTranslation(key) ? EmiPort.translatable(key) : EmiPort.literal(StatsMath.humanize(slot.getName()));
				section.text(EmiPort.translatable("emi_stats.armour.slot", name));
			}
			section.bar(EmiPort.translatable("emi_stats.armour.armour"), StatsMath.fraction(armourValue, 20), ARMOUR_COLOR,
				EmiPort.literal(StatsText.number(StatsMath.round2(armourValue))));
			if (toughnessValue != 0) {
				section.bar(EmiPort.translatable("emi_stats.armour.toughness"), StatsMath.fraction(toughnessValue, 12), TOUGHNESS_COLOR,
					EmiPort.literal(StatsText.number(StatsMath.round2(toughnessValue))));
			}
			if (knockbackValue != 0) {
				section.bar(EmiPort.translatable("emi_stats.armour.knockback"), StatsMath.fraction(knockbackValue, 1), KNOCKBACK_COLOR,
					StatsText.percent(StatsMath.round2(knockbackValue)));
			}
			wear(section, item);
			return;
		}
	}

	/**
	 * The modifiers one attribute gets from the item while it is in the given slot, in the pure maths' terms. Read
	 * through {@code ItemStack#applyAttributeModifiers}, as the game does: the stack's attribute modifier component
	 * when it has entries, else the item's own defaults ({@code Item#getAttributeModifiers}, where an
	 * {@code ArmorItem} keeps its armour values), so vanilla and modded items give the same data as in play.
	 */
	private static List<Mod> collect(ItemStack item, EquipmentSlot slot, RegistryEntry<EntityAttribute> attribute) {
		List<Mod> mods = new ArrayList<>();
		item.applyAttributeModifiers(slot, (entry, modifier) -> {
			if (entry.matches(attribute)) {
				Op op = switch (modifier.operation()) {
					case ADD_VALUE -> Op.ADD_VALUE;
					case ADD_MULTIPLIED_BASE -> Op.ADD_MULTIPLIED_BASE;
					case ADD_MULTIPLIED_TOTAL -> Op.ADD_MULTIPLIED_TOTAL;
				};
				mods.add(new Mod(modifier.value(), op));
			}
		});
		return mods;
	}

	/** What a mining rule applies to: its label and, for a tag, a tooltip with the raw tag id. */
	private record Target(Text label, List<Text> tooltip) {
	}

	/**
	 * A mining rule's blocks: a friendly name for a vanilla tag, else the tag's translated name (EMI's
	 * {@code tag.<registry>.<namespace>.<path>} keys), else a readable name built from the path ({@code Wool}); the
	 * raw tag id goes in the tooltip. A plain block list shows the first block names.
	 */
	private static Target target(RegistryEntryList<Block> blocks) {
		Optional<TagKey<Block>> tag = blocks.getTagKey();
		if (tag.isPresent()) {
			TagKey<Block> key = tag.get();
			Identifier id = key.id();
			String langKey = StatsMath.targetKey(id.getNamespace(), id.getPath());
			EmiTagKey<Block> emiTag = EmiTagKey.of(key);
			Text label;
			if (I18n.hasTranslation(langKey)) {
				label = EmiPort.translatable(langKey);
			} else if (emiTag.hasTranslation()) {
				label = emiTag.getTagName();
			} else {
				label = EmiPort.literal(StatsMath.humanize(id.getPath()));
			}
			return new Target(label, List.of(EmiPort.literal("#" + id)));
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
		return new Target(total > count ? EmiPort.translatable("emi_stats.target.blocks.more", names, total - count) : names, List.of());
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
			draw.drawText(MinecraftClient.getInstance().textRenderer, TextColors.map(EmiPort.translatable("emi_stats.food.hunger", StatsText.number(nutrition))),
				HungerIcons.WIDTH + 6, 2, TextColors.map(TEXT), false);
		}

		@Override
		public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
			return List.of(TooltipComponent.of(EmiPort.translatable("emi_stats.food.hunger.tooltip", StatsText.number(nutrition),
				StatsText.number(nutrition / 2.0)).asOrderedText()));
		}
	}
}
