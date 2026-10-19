package dev.emi.remastered.world.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.ores.emi.stack.BiomeEmiStack;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.client.ClientWorldData;
import dev.emi.remastered.world.client.CrossData;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.WorldFormat;
import dev.emi.remastered.world.data.WorldSnapshot;
import dev.emi.remastered.world.emi.PageParts.Icon;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * The page of one biome: climate (temperature, downfall, precipitation), tags and dimensions, then the mobs that spawn
 * there, the structures that generate there and the ores that generate there, each as icons that open their own
 * pages. Climate and tags come from the client registry; dimensions and structures from the server's payload, mobs and
 * ores from the mobs and ores modules. Sections of a module that is off are left out; with the module on but without
 * data they say so.
 */
public class BiomeInfoRecipe implements EmiRecipe {
	private static final int HEADER_HEIGHT = 22;
	private static final int COLUMN_2 = 96;
	private static final int MAX_TAG_TOOLTIP_LINES = 24;
	private static final BlockPos SEA_LEVEL = new BlockPos(0, 64, 0);
	private static final Map<Biome, Float> DOWNFALL = new WeakHashMap<>();

	private final Identifier biomeId;
	private final Biome biome;
	private final EmiStack stack;
	private final Identifier id;
	private final List<String> tags;
	private final float downfall;
	private Model model;
	private int modelKey = Integer.MIN_VALUE;

	public BiomeInfoRecipe(Identifier biomeId, Biome biome, List<String> tags, float downfall) {
		this.biomeId = biomeId;
		this.biome = biome;
		this.stack = BiomeEmiStack.of(biome);
		this.id = recipeId(biomeId);
		this.tags = tags;
		this.downfall = downfall;
	}

	/** Recipe id of the page of a biome. */
	public static Identifier recipeId(Identifier biomeId) {
		return World.id("/biome/" + biomeId.getNamespace() + "/" + biomeId.getPath());
	}

	/** The page of a biome of the client registry; the downfall is read through the biome codec, which is the only public way to it. */
	public static BiomeInfoRecipe of(Registry<Biome> registry, Identifier biomeId, Biome biome) {
		List<String> tags = registry.getEntry(biomeId).map(entry -> entry.streamTags()
			.map(tag -> tag.id().toString()).sorted(Comparator.comparing((String tag) -> tag.startsWith("minecraft:") ? 0 : 1).thenComparing(tag -> tag)).toList()).orElse(List.of());
		return new BiomeInfoRecipe(biomeId, biome, tags, downfall(biome));
	}

	private static float downfall(Biome biome) {
		synchronized (DOWNFALL) {
			Float cached = DOWNFALL.get(biome);
			if (cached != null) {
				return cached;
			}
		}
		float value = Float.NaN;
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world != null) {
			DynamicRegistryManager registries = world.getRegistryManager();
			try {
				JsonElement json = Biome.NETWORK_CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), biome).getOrThrow();
				if (json instanceof JsonObject object && object.has("downfall")) {
					value = object.get("downfall").getAsFloat();
				}
			} catch (RuntimeException e) {
				World.LOGGER.warn("Could not read the downfall of a biome: {}", e.toString());
			}
		}
		synchronized (DOWNFALL) {
			DOWNFALL.put(biome, value);
		}
		return value;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return WorldEmiRegistration.BIOME;
	}

	@Override
	public @Nullable Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(stack);
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

	@Override
	public int getDisplayHeight() {
		return model().height();
	}

	/** One section of mob, structure or ore icons; {@code note} replaces the icons when the data is missing. */
	private record Section(Text label, List<Icon> icons, @Nullable Text note) {
		int height() {
			return icons.isEmpty() ? PageParts.LINE + 2 : PageParts.sectionHeight(icons.size());
		}
	}

	private record Model(List<Section> sections, List<String> dimensions, boolean worldData, int height) {
	}

	/** The content of the page; rebuilt when data that arrived after EMI's reload changes what it shows. */
	private synchronized Model model() {
		int key = ClientWorldData.version() * 4 + (CrossData.hasMobData() ? 2 : 0) + (CrossData.hasOreData() ? 1 : 0);
		if (model == null || key != modelKey) {
			model = buildModel();
			modelKey = key;
		}
		return model;
	}

	private Model buildModel() {
		String biome = biomeId.toString();
		WorldSnapshot world = ClientWorldData.snapshot();
		List<Section> sections = new ArrayList<>();
		sections.add(mobSection(biome));
		sections.add(structureSection(biome, world));
		sections.add(oreSection(biome));
		sections.removeIf(section -> section == null);
		List<String> dimensions = world.isComplete() ? world.dimensionsOfBiome(biome) : List.of();
		int height = HEADER_HEIGHT + 3 + 3 * PageParts.LINE + 2 * PageParts.LINE + 3;
		for (Section section : sections) {
			height += section.height();
		}
		return new Model(sections, dimensions, world.isComplete(), height);
	}

	private @Nullable Section mobSection(String biome) {
		if (!Modules.MOBS.isEnabled()) {
			return null;
		}
		Text title = Text.translatable("emi_world.biome.mobs");
		if (!CrossData.hasMobData()) {
			return new Section(title, List.of(), Text.translatable("emi_world.no_data.mobs"));
		}
		List<Icon> icons = new ArrayList<>();
		for (CrossData.Mob mob : CrossData.mobsOf(biome)) {
			var egg = mob.egg();
			if (egg == null) {
				continue;
			}
			List<Text> tooltip = List.of(
				Text.translatable("emi_world.biome.mob.entry", Text.translatable("emi_mobs.group." + mob.group()), mob.weight(),
					WorldFormat.groupSize(mob.minGroup(), mob.maxGroup())).formatted(Formatting.GRAY));
			icons.add(new Icon(EmiStack.of(egg), tooltip));
		}
		return new Section(Text.translatable("emi_world.biome.mobs.count", icons.size()), icons, icons.isEmpty() ? Text.translatable("emi_world.biome.none") : null);
	}

	private @Nullable Section structureSection(String biome, WorldSnapshot world) {
		Text title = Text.translatable("emi_world.biome.structures");
		if (!world.isComplete()) {
			return new Section(title, List.of(), Text.translatable("emi_world.no_data.server"));
		}
		List<Icon> icons = new ArrayList<>();
		for (StructureRow structure : world.structuresOfBiome(biome)) {
			icons.add(new Icon(StructureEmiStack.of(structure.id())));
		}
		icons.sort(Comparator.comparing(icon -> icon.stack().getName().getString(), String.CASE_INSENSITIVE_ORDER));
		return new Section(Text.translatable("emi_world.biome.structures.count", icons.size()), icons,
			icons.isEmpty() ? Text.translatable("emi_world.biome.none") : null);
	}

	private @Nullable Section oreSection(String biome) {
		if (!Modules.ORES.isEnabled()) {
			return null;
		}
		Text title = Text.translatable("emi_world.biome.ores");
		if (!CrossData.hasOreData()) {
			return new Section(title, List.of(), Text.translatable("emi_world.no_data.ores"));
		}
		List<Icon> icons = new ArrayList<>();
		for (Block block : CrossData.oresOf(biome)) {
			icons.add(new Icon(EmiStack.of(block)));
		}
		return new Section(Text.translatable("emi_world.biome.ores.count", icons.size()), icons, icons.isEmpty() ? Text.translatable("emi_world.biome.none") : null);
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		Model model = model();
		TextRenderer font = MinecraftClient.getInstance().textRenderer;

		widgets.addSlot(stack, 0, 0).recipeContext(this);
		Text name = stack.getName();
		PageParts.addTrimmed(widgets, font, name, 22, 1, PageParts.WIDTH - 22, PageParts.TEXT_DARK);
		widgets.addText(Text.literal(WorldFormat.shortId(biomeId.toString())), 22, 11, PageParts.TEXT_LIGHT, false);

		int y = HEADER_HEIGHT + 3;
		addClimate(widgets, font, y);
		y += 3 * PageParts.LINE;
		addTags(widgets, font, y);
		y += PageParts.LINE;
		addDimensions(widgets, font, model, y);
		y += PageParts.LINE + 3;

		for (Section section : model.sections()) {
			if (section.icons().isEmpty()) {
				MutableText line = Text.empty().append(section.label().copy().formatted(Formatting.DARK_GRAY));
				if (section.note() != null) {
					line.append(" ").append(section.note().copy().formatted(Formatting.GRAY, Formatting.ITALIC));
				}
				PageParts.addTrimmed(widgets, font, line, 0, y, PageParts.WIDTH, PageParts.TEXT);
				y += section.height();
			} else {
				y = PageParts.addIcons(widgets, section.label(), section.icons(), y);
			}
		}
	}

	private void addClimate(WidgetHolder widgets, TextRenderer font, int y) {
		Text temperature = Text.empty().append(Text.translatable("emi_world.biome.temperature").formatted(Formatting.DARK_GRAY))
			.append(" ").append(WorldFormat.climate(biome.getTemperature()));
		widgets.addText(temperature, 0, y, PageParts.TEXT_DARK, false);
		widgets.addTooltipText(List.of(Text.translatable("emi_world.biome.temperature.tooltip"),
			Text.translatable("emi_world.biome.temperature.hint").formatted(Formatting.GRAY)), 0, y, COLUMN_2 - 4, PageParts.LINE - 2);

		Text downfallText = Float.isNaN(downfall) ? Text.literal("?") : Text.literal(WorldFormat.climate(downfall));
		Text downfallLine = Text.empty().append(Text.translatable("emi_world.biome.downfall").formatted(Formatting.DARK_GRAY)).append(" ").append(downfallText);
		widgets.addText(downfallLine, COLUMN_2, y, PageParts.TEXT_DARK, false);
		widgets.addTooltipText(List.of(Text.translatable("emi_world.biome.downfall.tooltip")), COLUMN_2, y, PageParts.WIDTH - COLUMN_2, PageParts.LINE - 2);

		Biome.Precipitation precipitation = biome.getPrecipitation(SEA_LEVEL);
		PageParts.addLine(widgets, font, Text.translatable("emi_world.biome.precipitation"),
			Text.translatable("emi_world.biome.precipitation." + precipitation.asString()), y + PageParts.LINE,
			List.of(Text.translatable("emi_world.biome.precipitation.tooltip")));
		PageParts.addLine(widgets, font, Text.translatable("emi_world.biome.has_precipitation"),
			Text.translatable(biome.hasPrecipitation() ? "emi_world.yes" : "emi_world.no"), y + 2 * PageParts.LINE,
			List.of(Text.translatable("emi_world.biome.has_precipitation.tooltip")));
	}

	private void addTags(WidgetHolder widgets, TextRenderer font, int y) {
		Text label = Text.translatable("emi_world.biome.tags").formatted(Formatting.DARK_GRAY);
		if (tags.isEmpty()) {
			widgets.addText(Text.empty().append(label).append(" ").append(Text.translatable("emi_world.biome.none")), 0, y, PageParts.TEXT_DARK, false);
			return;
		}
		StringBuilder list = new StringBuilder();
		for (String tag : tags) {
			if (!list.isEmpty()) {
				list.append(", ");
			}
			list.append('#').append(WorldFormat.shortId(tag));
		}
		Text line = Text.empty().append(label).append(" ").append(list.toString());
		PageParts.addTrimmed(widgets, font, line, 0, y, PageParts.WIDTH, PageParts.TEXT_DARK);
		List<Text> tooltip = new ArrayList<>();
		tooltip.add(Text.translatable("emi_world.biome.tags.tooltip", tags.size()).formatted(Formatting.GOLD));
		for (String tag : tags) {
			if (tooltip.size() > MAX_TAG_TOOLTIP_LINES) {
				tooltip.add(Text.literal("...").formatted(Formatting.GRAY));
				break;
			}
			tooltip.add(Text.literal("#" + tag).formatted(Formatting.GRAY));
		}
		widgets.addTooltipText(tooltip, 0, y, PageParts.WIDTH, PageParts.LINE - 2);
	}

	private void addDimensions(WidgetHolder widgets, TextRenderer font, Model model, int y) {
		Text label = Text.translatable("emi_world.biome.dimensions").formatted(Formatting.DARK_GRAY);
		Text value;
		if (!model.worldData()) {
			value = Text.translatable("emi_world.no_data.server").formatted(Formatting.GRAY, Formatting.ITALIC);
		} else if (model.dimensions().isEmpty()) {
			value = Text.translatable("emi_world.biome.dimensions.none");
		} else {
			value = WorldTexts.dimensionNames(model.dimensions());
		}
		PageParts.addTrimmed(widgets, font, Text.empty().append(label).append(" ").append(value), 0, y, PageParts.WIDTH, PageParts.TEXT_DARK);
	}
}
