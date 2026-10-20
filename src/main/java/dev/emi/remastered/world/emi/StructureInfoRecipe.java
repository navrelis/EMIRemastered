package dev.emi.remastered.world.emi;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.WorldSettings;
import dev.emi.remastered.world.client.ClientWorldData;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.StructureRow.Kind;
import dev.emi.remastered.world.data.StructureRow.Placement;
import dev.emi.remastered.world.data.WorldFormat;
import dev.emi.remastered.world.emi.PageParts.Icon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * The page of one structure: dimensions, generation step, placement (spacing and separation, or rings), the biomes it
 * generates in as clickable biome icons, and links to the chest loot tables found in it (needs the loot module). The
 * data is what the server sent when EMI registered the page; the loot links are read from the current client data,
 * since the server may send them later.
 */
public class StructureInfoRecipe implements EmiRecipe {
	private static final int HEADER_HEIGHT = 22;
	private static final int LINK_ROW = 10;
	private static final int LINK_COLUMN = 88;
	private static final String CHEST_RECIPE_NAMESPACE = "emi_loot";

	private final StructureRow row;
	private final Identifier id;
	private final EmiStack stack;
	private final List<EmiStack> biomes;
	private List<LootLink> loot;
	private int lootVersion;

	public StructureInfoRecipe(StructureRow row, Registry<Biome> registry) {
		this.row = row;
		this.stack = StructureEmiStack.of(row.id());
		this.id = recipeId(row.id());
		List<EmiStack> stacks = new ArrayList<>();
		for (String biome : row.biomes()) {
			Identifier biomeId = Identifier.tryParse(biome);
			Biome entry = biomeId == null ? null : registry.get(biomeId);
			if (entry != null) {
				stacks.add(BiomeEmiStack.of(entry));
			}
		}
		this.biomes = List.copyOf(stacks);
	}

	/** Recipe id of the page of a structure. */
	public static Identifier recipeId(String structureId) {
		Identifier structure = Identifier.tryParse(structureId);
		return World.id("/structure/" + (structure == null ? structureId.replace(':', '/') : structure.getNamespace() + "/" + structure.getPath()));
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return WorldEmiRegistration.STRUCTURE;
	}

	@Override
	public @Nullable Identifier getId() {
		return id;
	}

	/** The biomes the structure generates in, so that "uses" of a biome find the structure. */
	@Override
	public List<EmiIngredient> getInputs() {
		return biomes.isEmpty() ? List.of() : List.of(EmiIngredient.of(biomes));
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(stack);
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return PageParts.WIDTH;
	}

	/** A chest loot table with the page the loot module made for it. */
	private record LootLink(String table, EmiRecipe recipe) {
	}

	/**
	 * The loot pages of the structure's chests that exist in this EMI session; empty when the loot module is off. The
	 * tables are read from {@link ClientWorldData} because they can arrive after the page was created.
	 */
	private synchronized List<LootLink> loot() {
		int version = ClientWorldData.lootVersion();
		if (loot == null || lootVersion != version) {
			lootVersion = version;
			List<LootLink> links = new ArrayList<>();
			if (WorldSettings.maxLootLinks() > 0 && Modules.LOOT.isEnabled()) {
				for (String table : ClientWorldData.lootTablesOf(row.id())) {
					Identifier tableId = Identifier.tryParse(table);
					if (tableId == null) {
						continue;
					}
					EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(
						Identifier.of(CHEST_RECIPE_NAMESPACE, "/chest_loot/" + tableId.getNamespace() + "/" + tableId.getPath()));
					if (recipe != null) {
						links.add(new LootLink(table, recipe));
					}
				}
			}
			loot = links;
		}
		return loot;
	}

	private int lootRows() {
		int shown = Math.min(WorldSettings.maxLootLinks(), loot().size());
		int rows = (shown + 1) / 2;
		return loot().size() > shown ? rows + 1 : rows;
	}

	private int placementLines() {
		return switch (row.placement().kind()) {
			case RANDOM_SPREAD -> 2;
			case CONCENTRIC_RINGS -> 3;
			default -> 1;
		};
	}

	private boolean hasSet() {
		return !row.set().isEmpty() && WorldFormat.hasSiblings(row.weight(), row.totalWeight());
	}

	private int biomesHeight() {
		return biomes.isEmpty() ? PageParts.LINE + 2 : PageParts.sectionHeight(biomes.size());
	}

	@Override
	public int getDisplayHeight() {
		int height = HEADER_HEIGHT + 3 + (2 + placementLines() + (hasSet() ? 1 : 0)) * PageParts.LINE + 3 + biomesHeight();
		if (!loot().isEmpty()) {
			height += PageParts.LINE + lootRows() * LINK_ROW + 2;
		}
		return height;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		widgets.addSlot(stack, 0, 0).recipeContext(this);
		PageParts.addTrimmed(widgets, font, stack.getName(), 22, 1, PageParts.WIDTH - 22, PageParts.TEXT_DARK);
		widgets.addText(Text.literal(WorldFormat.shortId(row.id())), 22, 11, PageParts.TEXT_LIGHT, false);

		int y = HEADER_HEIGHT + 3;
		Text dimensions = row.dimensions().isEmpty() ? Text.translatable("emi_world.structure.dimensions.none") : WorldTexts.dimensionNames(row.dimensions());
		PageParts.addLine(widgets, font, Text.translatable("emi_world.biome.dimensions"), dimensions, y, List.of());
		y += PageParts.LINE;
		PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.step"), WorldTexts.stepName(row.step()), y,
			List.of(Text.translatable("emi_world.structure.step.tooltip")));
		y += PageParts.LINE;
		y = addPlacement(widgets, font, y);
		if (hasSet()) {
			PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.set"),
				Text.translatable("emi_world.structure.set.value", WorldFormat.shortId(row.set()), row.weight(), row.totalWeight()), y,
				List.of(Text.translatable("emi_world.structure.set.tooltip", row.set(), row.weight(), row.totalWeight())));
			y += PageParts.LINE;
		}
		y += 3;

		y = addBiomes(widgets, font, y);
		if (!loot().isEmpty()) {
			addLoot(widgets, font, y);
		}
	}

	private int addPlacement(WidgetHolder widgets, TextRenderer font, int y) {
		Placement placement = row.placement();
		Text label = Text.translatable("emi_world.structure.placement");
		switch (placement.kind()) {
			case RANDOM_SPREAD -> {
				PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.spacing"),
					Text.translatable("emi_world.structure.chunks", placement.spacing(), WorldFormat.blocks(placement.spacing())), y,
					List.of(Text.translatable("emi_world.structure.spacing.tooltip"),
						Text.translatable("emi_world.structure.spread." + (placement.triangular() ? "triangular" : "linear")).formatted(Formatting.GRAY)));
				PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.separation"),
					Text.translatable("emi_world.structure.chunks_only", placement.separation()), y + PageParts.LINE,
					List.of(Text.translatable("emi_world.structure.separation.tooltip", WorldFormat.blocks(placement.separation()))));
				return y + 2 * PageParts.LINE;
			}
			case CONCENTRIC_RINGS -> {
				PageParts.addLine(widgets, font, label, Text.translatable("emi_world.structure.rings"), y,
					List.of(Text.translatable("emi_world.structure.rings.tooltip")));
				PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.rings.count"),
					Text.translatable("emi_world.structure.rings.count.value", placement.ringCount(), placement.ringSpread()), y + PageParts.LINE,
					List.of(Text.translatable("emi_world.structure.rings.count.tooltip")));
				PageParts.addLine(widgets, font, Text.translatable("emi_world.structure.rings.distance"),
					Text.translatable("emi_world.structure.chunks_only", placement.ringDistance()), y + 2 * PageParts.LINE,
					List.of(Text.translatable("emi_world.structure.rings.distance.tooltip", WorldFormat.blocks(placement.ringDistance()))));
				return y + 3 * PageParts.LINE;
			}
			default -> {
				PageParts.addLine(widgets, font, label,
					Text.translatable(placement.kind() == Kind.OTHER ? "emi_world.structure.placement.other" : "emi_world.structure.placement.none"), y,
					List.of(Text.translatable(placement.kind() == Kind.OTHER ? "emi_world.structure.placement.other.tooltip" : "emi_world.structure.placement.none.tooltip")));
				return y + PageParts.LINE;
			}
		}
	}

	private int addBiomes(WidgetHolder widgets, TextRenderer font, int y) {
		Text label = Text.translatable("emi_world.structure.biomes", biomes.size());
		if (biomes.isEmpty()) {
			widgets.addText(Text.empty().append(label.copy().formatted(Formatting.DARK_GRAY)).append(" ")
				.append(Text.translatable("emi_world.biome.none").formatted(Formatting.GRAY, Formatting.ITALIC)), 0, y, PageParts.TEXT, false);
			return y + biomesHeight();
		}
		List<Icon> icons = new ArrayList<>();
		for (EmiStack biome : biomes) {
			icons.add(new Icon(biome));
		}
		if (!row.biomeTag().isEmpty()) {
			// the tag the biomes come from, right aligned next to the label
			int width = PageParts.WIDTH - font.getWidth(label) - 8;
			Text tag = Text.literal("#" + WorldFormat.shortId(row.biomeTag()));
			if (width > 24) {
				String shown = font.getWidth(tag) <= width ? tag.getString() : font.trimToWidth(tag, width - font.getWidth("...")).getString() + "...";
				widgets.addText(Text.literal(shown), PageParts.WIDTH, y, PageParts.TEXT_LIGHT, false)
					.horizontalAlign(TextWidget.Alignment.END);
				widgets.addTooltipText(List.of(Text.translatable("emi_world.structure.biome_tag", row.biomeTag())), PageParts.WIDTH - width, y, width, PageParts.LINE - 2);
			}
		}
		return PageParts.addIcons(widgets, label, icons, y);
	}

	private void addLoot(WidgetHolder widgets, TextRenderer font, int y) {
		List<LootLink> links = loot();
		widgets.addText(Text.translatable("emi_world.structure.loot", links.size()), 0, y, PageParts.TEXT, false);
		int top = y + PageParts.LINE;
		int shown = Math.min(WorldSettings.maxLootLinks(), links.size());
		for (int i = 0; i < shown; i++) {
			LootLink link = links.get(i);
			Text name = WorldTexts.lootTableName(link.table(), row.id());
			int x = (i % 2) * LINK_COLUMN;
			int linkY = top + (i / 2) * LINK_ROW;
			Text label = name;
			if (font.getWidth(name) > LINK_COLUMN - 4) {
				label = Text.literal(font.trimToWidth(name, LINK_COLUMN - 4 - font.getWidth("...")).getString() + "...");
			}
			widgets.add(new LinkWidget(label, x, linkY, true, () -> EmiApi.displayRecipe(link.recipe()),
				List.of(name, Text.translatable("emi_world.structure.loot.tooltip").formatted(Formatting.GRAY),
					Text.literal(link.table()).formatted(Formatting.DARK_GRAY))));
		}
		if (links.size() > shown) {
			MutableText more = Text.translatable("emi_world.more_loot", links.size() - shown).formatted(Formatting.ITALIC);
			widgets.addText(more, 0, top + ((shown + 1) / 2) * LINK_ROW, PageParts.TEXT, false);
			List<Text> tooltip = new ArrayList<>();
			for (int i = shown; i < links.size() && i < shown + 20; i++) {
				tooltip.add(WorldTexts.lootTableName(links.get(i).table(), row.id()));
			}
			widgets.addTooltipText(tooltip, 0, top + ((shown + 1) / 2) * LINK_ROW, font.getWidth(more), 9);
		}
	}

	/** The biome registry of the client world, null before a world is joined. */
	static @Nullable Registry<Biome> biomeRegistry() {
		ClientWorld world = MinecraftClient.getInstance().world;
		return world == null ? null : world.getRegistryManager().get(RegistryKeys.BIOME);
	}
}
