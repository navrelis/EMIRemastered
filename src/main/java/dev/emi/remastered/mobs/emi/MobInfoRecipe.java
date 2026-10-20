package dev.emi.remastered.mobs.emi;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.loot.client.ClientLootTables;
import dev.emi.remastered.loot.client.ClientMobLootTable;
import dev.emi.remastered.loot.client.ClientResourceData;
import dev.emi.remastered.loot.client.LootReceiver;
import dev.emi.remastered.loot.util.EntityBounds;
import dev.emi.remastered.loot.util.stack.EntityEmiStack;
import dev.emi.remastered.mobs.Mobs;
import dev.emi.remastered.mobs.MobsSettings;
import dev.emi.remastered.mobs.client.ClientSpawnData;
import dev.emi.remastered.mobs.data.MobFormat;
import dev.emi.remastered.mobs.data.MobStats;
import dev.emi.remastered.mobs.data.MobStats.Stat;
import dev.emi.remastered.mobs.data.PortraitFit;
import dev.emi.remastered.mobs.data.SpawnOverrides;
import dev.emi.remastered.mobs.data.SpawnRow;
import dev.emi.remastered.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/**
 * The info page of one mob: portrait and spawn egg, name and spawn group, attributes, natural spawn biomes (from the
 * data the server sent) and a link to its drops.
 */
public class MobInfoRecipe implements EmiRecipe {
	public static final int WIDTH = 176;
	private static final int HEADER_HEIGHT = 31;
	private static final int STAT_ROW = 11;
	private static final int STAT_LABEL_WIDTH = 56;
	private static final int STAT_COLUMN_2 = 88;
	private static final int SPAWN_ROW = 10;
	private static final int TOOLTIP_LIMIT = 28;
	/** Side of the square the mob is fitted into, inside the 26x26 portrait slot. */
	private static final int PORTRAIT_BOX = 22;
	private static final int TEXT = 0x404040;
	private static final int TEXT_DARK = 0x202020;
	private static final int TEXT_LIGHT = 0x6A6A6A;
	private static final Identifier HEART = Identifier.ofVanilla("hud/heart/full");
	private static final Identifier ARMOR = Identifier.ofVanilla("hud/armor_full");

	private final EntityType<?> type;
	private final Identifier typeId;
	private final Identifier id;
	private final @Nullable EmiStack egg;
	/** The mob itself as stack: what the page opens from when its spawn egg is missing or not in the index. */
	private final EmiStack mobStack;
	private final MobStats stats;

	public MobInfoRecipe(EntityType<?> type, Identifier typeId) {
		this.type = type;
		this.typeId = typeId;
		this.id = recipeId(typeId);
		SpawnEggItem item = SpawnEggItem.forEntity(type);
		this.egg = item == null ? null : EmiStack.of(item);
		this.mobStack = MobEmiStack.of(type);
		this.stats = readStats(type);
	}

	/** Recipe id of the mob page of an entity type. */
	public static Identifier recipeId(Identifier typeId) {
		return Mobs.id("/mob/" + typeId.getNamespace() + "/" + typeId.getPath());
	}

	@SuppressWarnings("unchecked")
	public static MobStats readStats(EntityType<?> type) {
		DefaultAttributeContainer attributes = DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) type);
		String typeId = Registries.ENTITY_TYPE.getId(type).toString();
		return new MobStats(
			attribute(attributes, EntityAttributes.GENERIC_MAX_HEALTH), attribute(attributes, EntityAttributes.GENERIC_ARMOR),
			attribute(attributes, EntityAttributes.GENERIC_ARMOR_TOUGHNESS),
			SpawnOverrides.attackDamage(typeId, attribute(attributes, EntityAttributes.GENERIC_ATTACK_DAMAGE)),
			MobStats.walkingSpeed(attribute(attributes, EntityAttributes.GENERIC_MOVEMENT_SPEED),
				EntityAttributes.GENERIC_MOVEMENT_SPEED.value().getDefaultValue()),
			attribute(attributes, EntityAttributes.GENERIC_FLYING_SPEED), attribute(attributes, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
			attribute(attributes, EntityAttributes.GENERIC_FOLLOW_RANGE),
			type.getDimensions().width(), type.getDimensions().height(), type.isFireImmune(), type.getSpawnGroup().getName());
	}

	private static double attribute(DefaultAttributeContainer attributes, RegistryEntry<EntityAttribute> attribute) {
		return attributes.has(attribute) ? attributes.getValue(attribute) : Double.NaN;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return MobsEmiRegistration.MOB_INFO;
	}

	@Override
	public @Nullable Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of();
	}

	/** The spawn egg and the mob stack are catalysts so that "uses" of them find the page. */
	@Override
	public List<EmiIngredient> getCatalysts() {
		return List.copyOf(outputs());
	}

	/** The spawn egg and the mob stack are the outputs so that "recipes" of them find the page. */
	@Override
	public List<EmiStack> getOutputs() {
		return outputs();
	}

	private List<EmiStack> outputs() {
		return mobStack.isEmpty() ? (egg == null ? List.of() : List.of(egg)) : (egg == null ? List.of(mobStack) : List.of(egg, mobStack));
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return layout().height();
	}

	private record Layout(int spawnY, MobFormat.Compact<SpawnRow> spawns, int spawnRows, int dropsY, int height, boolean dropsRow,
			List<SpawnRow> allSpawns) {
	}

	private Layout layout() {
		int statRows = (stats.visible().size() + 1) / 2;
		int spawnY = HEADER_HEIGHT + statRows * STAT_ROW + 3;
		List<SpawnRow> all = ClientSpawnData.rowsOf(typeId.toString());
		MobFormat.Compact<SpawnRow> compact = MobFormat.Compact.of(all, MobsSettings.compactBiomeCount());
		int rows = all.isEmpty() ? 1 : compact.shown().size() + (compact.hidden() > 0 ? 1 : 0);
		int end = spawnY + 11 + rows * SPAWN_ROW;
		boolean drops = MobsSettings.showDropsLink && Modules.LOOT.isEnabled();
		int dropsY = end + 4;
		return new Layout(spawnY, compact, rows, dropsY, drops ? dropsY + 10 : end, drops, all);
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		Layout layout = layout();
		MinecraftClient client = MinecraftClient.getInstance();
		TextRenderer font = client.textRenderer;

		addPortrait(widgets, client);
		int textX = egg == null ? 30 : 49;
		if (egg != null) {
			widgets.addSlot(egg, 28, 0);
		}
		addTrimmed(widgets, font, type.getName(), textX, 0, WIDTH - textX, TEXT);

		MutableText group = Text.translatable("emi_mobs.group." + stats.group());
		if (stats.fireImmune()) {
			group.append(Text.literal(" · ")).append(Text.translatable("emi_mobs.fire_immune"));
		}
		addTrimmed(widgets, font, group, textX, 10, WIDTH - textX, TEXT_LIGHT);
		addTrimmed(widgets, font, Text.translatable("emi_mobs.size", stats.sizeText()), textX, 19, WIDTH - textX, TEXT_LIGHT);

		addStats(widgets);
		addSpawns(widgets, font, layout);
		if (layout.dropsRow()) {
			addDrops(widgets, layout.dropsY());
		}
	}

	private void addPortrait(WidgetHolder widgets, MinecraftClient client) {
		Entity entity = null;
		try {
			entity = type.create(client.world);
		} catch (RuntimeException e) {
			Mobs.LOGGER.error("Could not create {} for its mob page", typeId, e);
		}
		if (entity == null) {
			widgets.addSlot(EmiStack.EMPTY, 0, 0).large(true);
			return;
		}
		if (entity instanceof SlimeEntity slime) {
			// resize first so the bounding box used for the scale below is the one that gets rendered
			slime.setSize(5, false);
		}
		EmiStack fitted = fitted(entity, PORTRAIT_BOX);
		if (fitted != null) {
			widgets.addSlot(fitted, 0, 0).large(true);
			return;
		}
		Box box = entity.getBoundingBox();
		double len = box.getAverageSideLength();
		if (len > 1.05) {
			len = (len + Math.sqrt(len)) / 2.0;
		}
		double scale = 1.05 / len * 8.0;
		if (ClientResourceData.MOB_SCALES.containsKey(type)) {
			scale *= ClientResourceData.MOB_SCALES.getOrDefault(type, 1.0f);
		}
		EmiStack stack = EntityEmiStack.ofScaled(entity, scale);
		if (!ClientResourceData.MOB_OFFSETS.containsKey(type)) {
			widgets.addSlot(stack, 0, 0).large(true);
		} else {
			int offset = ClientResourceData.MOB_OFFSETS.getOrDefault(type, 0);
			widgets.addTexture(EmiTexture.LARGE_SLOT, 0, 0);
			widgets.addDrawable(0, 0, 16, 16, (draw, mx, my, delta) -> stack.render(draw, 5, 6 + offset, delta));
			widgets.addTooltip(stack.getTooltip(), 0, 0, 26, 26);
		}
	}

	/**
	 * The mob scaled and centred by what its model really renders (a bat is drawn far smaller than its hit box), so
	 * that small and large mobs fill the portrait box alike. Null if it could not be measured.
	 */
	static @Nullable EmiStack fitted(Entity entity, int box) {
		if (!(entity instanceof LivingEntity living)) {
			return null;
		}
		Vector3f rotation = ClientResourceData.MOB_ROTATIONS.get(entity.getType());
		EntityBounds.Box bounds = EntityBounds.measure(living, rotation == null ? null : new Vector3f(rotation).mul(0.017453292F));
		if (bounds == null) {
			Mobs.LOGGER.warn("Could not measure {} for its portrait, using the hit box scale", entity.getType());
			return null;
		}
		// the portrait turns with the mouse, so a long mob seen from the front may end up seen from the side
		double centre = (bounds.minX() + bounds.maxX()) / 2.0;
		double reach = Math.hypot(bounds.width(), bounds.depth()) / 2.0;
		PortraitFit fit = PortraitFit.of(centre - reach, bounds.minY(), centre + reach, bounds.maxY(), box);
		return fit == null ? null : EntityEmiStack.ofFitted(entity, fit.scale(), fit.offsetX(), fit.offsetY());
	}

	private void addStats(WidgetHolder widgets) {
		List<Stat> visible = stats.visible();
		boolean icons = MobsSettings.attributesAsIcons;
		int perColumn = (visible.size() + 1) / 2;
		for (int i = 0; i < visible.size(); i++) {
			Stat stat = visible.get(i);
			int column = i / perColumn;
			int x = column == 0 ? 0 : STAT_COLUMN_2;
			int y = HEADER_HEIGHT + (i % perColumn) * STAT_ROW;
			widgets.addText(Text.translatable("emi_mobs.stat." + stat.key()), x, y, TEXT_LIGHT, false);
			int valueX = x + STAT_LABEL_WIDTH;
			Identifier icon = !icons ? null : switch (stat) {
				case HEALTH, ATTACK -> HEART;
				case ARMOR -> ARMOR;
				default -> null;
			};
			if (icon != null) {
				widgets.addDrawable(valueX, y, 9, 9, (draw, mx, my, delta) -> draw.drawGuiTexture(icon, 0, 0, 9, 9));
				valueX += 11;
			}
			String value = stats.value(stat);
			if (stat == Stat.SPEED) {
				value = "≈" + value;
			}
			widgets.addText(Text.literal(value), valueX, y, TEXT_DARK, false);
			List<Text> tooltip = new ArrayList<>();
			tooltip.add(Text.translatable("emi_mobs.stat." + stat.key() + ".tooltip", stats.tooltipArgs(stat)));
			if (stat == Stat.SPEED) {
				tooltip.add(Text.translatable("emi_mobs.stat.speed.approx").formatted(Formatting.GRAY));
			}
			widgets.addTooltipText(tooltip, x, y, column == 0 ? STAT_COLUMN_2 - 2 : WIDTH - STAT_COLUMN_2, STAT_ROW - 1);
		}
	}

	private void addSpawns(WidgetHolder widgets, TextRenderer font, Layout layout) {
		int y = layout.spawnY();
		boolean details = MobsSettings.showSpawnDetails;
		widgets.addText(Text.translatable("emi_mobs.spawn.title"), 0, y, TEXT, false);
		if (details && !layout.allSpawns().isEmpty()) {
			widgets.addText(Text.translatable("emi_mobs.spawn.columns").formatted(Formatting.ITALIC), WIDTH, y, TEXT_LIGHT, false)
				.horizontalAlign(TextWidget.Alignment.END);
		}
		y += 11;
		if (layout.allSpawns().isEmpty()) {
			boolean complete = ClientSpawnData.isComplete();
			addTrimmed(widgets, font, Text.translatable(complete ? "emi_mobs.spawn.none" : "emi_mobs.spawn.no_data"), 0, y, WIDTH, TEXT_LIGHT);
			widgets.addTooltipText(List.of(Text.translatable(complete ? "emi_mobs.spawn.none.tooltip" : "emi_mobs.spawn.no_data.tooltip")),
				0, layout.spawnY(), WIDTH, 11 + SPAWN_ROW);
			return;
		}
		int detailsWidth = details ? 54 : 0;
		for (SpawnRow row : layout.spawns().shown()) {
			addTrimmed(widgets, font, biomeName(row), 0, y, WIDTH - detailsWidth - 4, TEXT_DARK);
			if (details) {
				widgets.addText(Text.literal(row.weight() + " · " + MobFormat.groupSize(row.minGroup(), row.maxGroup())), WIDTH, y, TEXT_LIGHT, false)
					.horizontalAlign(TextWidget.Alignment.END);
			}
			y += SPAWN_ROW;
		}
		if (layout.spawns().hidden() > 0) {
			widgets.addText(Text.translatable("emi_mobs.spawn.more", layout.spawns().hidden()).formatted(Formatting.ITALIC), 0, y, TEXT, false);
		}
		List<Text> tooltip = new ArrayList<>();
		tooltip.add(Text.translatable("emi_mobs.spawn.tooltip", layout.allSpawns().size()).formatted(Formatting.GOLD));
		int listed = 0;
		for (SpawnRow row : layout.allSpawns()) {
			if (listed == TOOLTIP_LIMIT) {
				tooltip.add(Text.translatable("emi_mobs.spawn.more", layout.allSpawns().size() - listed).formatted(Formatting.GRAY, Formatting.ITALIC));
				break;
			}
			tooltip.add(Text.translatable("emi_mobs.spawn.entry", biomeName(row), row.weight(), MobFormat.groupSize(row.minGroup(), row.maxGroup())));
			listed++;
		}
		tooltip.add(Text.translatable("emi_mobs.spawn.note").formatted(Formatting.DARK_GRAY));
		widgets.addTooltipText(tooltip, 0, layout.spawnY(), WIDTH, 11 + layout.spawnRows() * SPAWN_ROW);
	}

	private static Text biomeName(SpawnRow row) {
		Identifier biome = Identifier.tryParse(row.biome());
		return biome == null ? Text.literal(row.biome()) : Text.translatable(biome.toTranslationKey("biome"));
	}

	private void addDrops(WidgetHolder widgets, int y) {
		EmiRecipe drops = findDrops(type);
		Text label = Text.translatable("emi_mobs.drops");
		widgets.addText(label, 0, y, TEXT, false);
		int linkX = MinecraftClient.getInstance().textRenderer.getWidth(label) + 5;
		if (drops == null) {
			widgets.add(new LinkWidget(Text.translatable("emi_mobs.drops.none"), linkX, y, false, () -> {
			}, List.of()));
		} else {
			widgets.add(new LinkWidget(Text.translatable("emi_mobs.drops.show"), linkX, y, true,
				() -> EmiApi.displayRecipe(drops), List.of(Text.translatable("emi_mobs.drops.tooltip", type.getName()))));
		}
	}

	/** The loot module's drop page of this mob, if the loot module is on and the server sent a table for it. */
	private static @Nullable EmiRecipe findDrops(EntityType<?> type) {
		EmiRecipeManager manager = EmiApi.getRecipeManager();
		Identifier table = type.getLootTableId().getValue();
		EmiRecipe recipe = manager.getRecipe(Identifier.of("emi_loot", "/mob_drops/" + table.getNamespace() + "/" + table.getPath()));
		if (recipe != null) {
			return recipe;
		}
		Identifier mob = Registries.ENTITY_TYPE.getId(type);
		for (LootReceiver receiver : new ArrayList<>(ClientLootTables.INSTANCE.mobLoots)) {
			if (receiver instanceof ClientMobLootTable mobTable && mob.equals(mobTable.mobId)) {
				Identifier tableId = mobTable.getId();
				recipe = manager.getRecipe(Identifier.of("emi_loot", "/mob_drops/" + tableId.getNamespace() + "/" + tableId.getPath()));
				if (recipe != null) {
					return recipe;
				}
			}
		}
		return null;
	}

	/** Adds the text, cut to the width with "..." and the full text as tooltip when it does not fit. */
	private static void addTrimmed(WidgetHolder widgets, TextRenderer font, Text text, int x, int y, int width, int color) {
		if (font.getWidth(text) <= width) {
			widgets.addText(text, x, y, color, false);
			return;
		}
		StringVisitable trimmed = font.trimToWidth(text, width - font.getWidth("..."));
		widgets.addText(Text.literal(trimmed.getString() + "..."), x, y, color, false);
		widgets.addTooltipText(List.of(text), x, y, width, 9);
	}
}
