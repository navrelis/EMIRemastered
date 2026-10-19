package dev.emi.remastered.loot.info;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.LootSettings;
import dev.emi.remastered.loot.emi.ChestLootRecipe;
import dev.emi.remastered.loot.structure.StructureRef;
import dev.emi.remastered.loot.util.ConditionIcons;
import dev.emi.remastered.loot.util.ConditionKind;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.StructureText;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Item Info sections of the loot module. EMI's recipe provider already lists the loot recipes of a stack under
 * "Produced by", one row per loot category, so this provider only adds what those rows do not say: in which
 * structures the chests that hold the item are found, and which drop conditions (Looting, Fortune, Silk Touch, fire,
 * player kill) apply to its block, mob and gameplay drops.
 */
public final class LootInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EMILoot.identity("loot");
	public static final Identifier STRUCTURES = EMILoot.identity("structures");
	public static final Identifier CONDITIONS = EMILoot.identity("conditions");
	private static final int MAX_STRUCTURES = 24;
	private static final int MAX_CHESTS_IN_TOOLTIP = 8;
	public static final LootInfoProvider INSTANCE = new LootInfoProvider();
	private LootInfoIndex index;

	private LootInfoProvider() {
	}

	/**
	 * The structures of chest tables arrived after EMI built its pages: rebuild the index and drop the cached Item Info
	 * pages (registering the same provider again changes the registry version the page cache depends on). Client thread.
	 */
	public static void refresh() {
		INSTANCE.index = null;
		ItemInfoRegistry.register(INSTANCE);
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "loot";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return LootSettings.itemInfo && stack.getKey() instanceof Item;
	}

	@Override
	public void onReload() {
		index = null;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		if (!(stack.getKey() instanceof Item item)) {
			return;
		}
		if (index == null) {
			index = LootInfoIndex.build();
		}
		if (LootSettings.showStructures) {
			addStructures(item, collector);
		}
		if (LootSettings.conditionHints) {
			addConditions(item, collector);
		}
	}

	/** A structure name as the player sees it, with the dimensions and chests it has for the item. */
	private static final class Place {
		final Set<String> dimensions = new LinkedHashSet<>();
		final List<ChestLootRecipe> chests = new ArrayList<>();
	}

	private void addStructures(Item item, InfoCollector collector) {
		Map<String, Place> places = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (ChestLootRecipe chest : index.chestsWith(item)) {
			for (StructureRef ref : chest.getStructures()) {
				Place place = places.computeIfAbsent(StructureText.structureName(ref.structure()), n -> new Place());
				for (Identifier dimension : ref.dimensions()) {
					place.dimensions.add(StructureText.dimensionName(dimension));
				}
				if (!place.chests.contains(chest)) {
					place.chests.add(chest);
				}
			}
		}
		if (places.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(STRUCTURES, InfoSection.Kind.OBTAIN, LText.translatable("emi_loot.info.structures", places.size()))
			.icon(EmiStack.of(Items.CHEST))
			.source(false);
		int shown = 0;
		for (Map.Entry<String, Place> entry : places.entrySet()) {
			if (shown++ >= MAX_STRUCTURES) {
				section.text(LText.translatable("emi_loot.info.more", places.size() - MAX_STRUCTURES));
				break;
			}
			Place place = entry.getValue();
			List<Text> tooltip = new ArrayList<>();
			for (int i = 0; i < place.chests.size() && i < MAX_CHESTS_IN_TOOLTIP; i++) {
				tooltip.add(LText.translatable("emi_loot.info.in_chest", place.chests.get(i).getTitle()).formatted(Formatting.GRAY));
			}
			if (place.chests.size() > MAX_CHESTS_IN_TOOLTIP) {
				tooltip.add(LText.translatable("emi_loot.info.more", place.chests.size() - MAX_CHESTS_IN_TOOLTIP).formatted(Formatting.GRAY));
			}
			List<EmiRecipe> recipes = new ArrayList<>(place.chests);
			section.entry(EmiStack.of(Items.CHEST), LText.literal(entry.getKey()))
				.detail(place.dimensions.isEmpty() ? null : LText.literal(String.join(", ", place.dimensions)))
				.tooltip(tooltip)
				.link(recipes.size() == 1 ? InfoLink.recipe(recipes.get(0)) : InfoLink.recipes(recipes));
		}
	}

	private void addConditions(Item item, InfoCollector collector) {
		Map<ConditionKind, List<EmiRecipe>> kinds = index.conditionsOf(item);
		if (kinds.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(CONDITIONS, InfoSection.Kind.DETAILS, LText.translatable("emi_loot.info.conditions"))
			.icon(EmiStack.of(Items.ENCHANTED_BOOK));
		for (ConditionKind kind : ConditionKind.values()) {
			List<EmiRecipe> recipes = kinds.get(kind);
			if (recipes == null) {
				continue;
			}
			section.entry(ConditionIcons.renderable(kind), LText.translatable(kind.labelKey()))
				.count(recipes.size())
				.tooltip(List.of(ConditionIcons.hint(kind)))
				.link(InfoLink.recipes(recipes));
		}
	}
}
