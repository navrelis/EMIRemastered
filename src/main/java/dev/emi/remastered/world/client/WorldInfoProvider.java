package dev.emi.remastered.world.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.world.World;
import dev.emi.remastered.world.data.StructureRow;
import dev.emi.remastered.world.data.WorldFormat;
import dev.emi.remastered.world.data.WorldSnapshot;
import dev.emi.remastered.world.emi.BiomeInfoRecipe;
import dev.emi.remastered.world.emi.StructureEmiStack;
import dev.emi.remastered.world.emi.StructureInfoRecipe;
import dev.emi.remastered.world.emi.WorldTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Item Info provider of the world module: the Item Info page of a biome or structure stack gets a Details section with
 * the key facts of its page (biome: temperature, downfall, dimensions; structure: dimensions, generation step, number
 * of biomes) and a link to the page. Items get nothing here.
 */
public final class WorldInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = World.id("world");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "world";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return stack.getKey() instanceof Biome || stack instanceof StructureEmiStack;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		if (stack.getKey() instanceof Biome biome) {
			collectBiome(stack, biome, collector);
		} else if (stack instanceof StructureEmiStack) {
			collectStructure(stack, collector);
		}
	}

	private static void collectBiome(EmiStack stack, Biome biome, InfoCollector collector) {
		Identifier id = stack.getId();
		if (id == null) {
			return;
		}
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, Text.translatable("emi_world.info.biome")).icon(stack);
		section.text(line("emi_world.biome.temperature", Text.literal(WorldFormat.climate(biome.getTemperature()))));
		float downfall = BiomeInfoRecipe.downfall(biome);
		section.text(line("emi_world.biome.downfall", Text.literal(Float.isNaN(downfall) ? "?" : WorldFormat.climate(downfall))));
		WorldSnapshot world = ClientWorldData.snapshot();
		section.text(line("emi_world.biome.dimensions", !world.isComplete() ? noData()
			: world.dimensionsOfBiome(id.toString()).isEmpty() ? Text.translatable("emi_world.biome.dimensions.none")
			: WorldTexts.dimensionNames(world.dimensionsOfBiome(id.toString()))));
		link(section, "emi_world.info.open_biome", stack, BiomeInfoRecipe.recipeId(id));
	}

	private static void collectStructure(EmiStack stack, InfoCollector collector) {
		Identifier id = stack.getId();
		StructureRow row = ClientWorldData.snapshot().structure(id.toString());
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, Text.translatable("emi_world.info.structure")).icon(stack);
		if (row == null) {
			section.text(noData());
		} else {
			section.text(line("emi_world.biome.dimensions", row.dimensions().isEmpty() ? Text.translatable("emi_world.structure.dimensions.none")
				: WorldTexts.dimensionNames(row.dimensions())));
			section.text(line("emi_world.structure.step", WorldTexts.stepName(row.step())));
			section.text(Text.translatable("emi_world.info.biomes", row.biomes().size()));
		}
		link(section, "emi_world.info.open_structure", stack, StructureInfoRecipe.recipeId(id.toString()));
	}

	private static void link(InfoSection section, String key, EmiStack stack, Identifier pageId) {
		EmiRecipe page = EmiApi.getRecipeManager().getRecipe(pageId);
		if (page != null) {
			section.link(Text.translatable(key, stack.getName()), InfoLink.recipe(page));
		}
	}

	private static Text line(String label, Text value) {
		return Text.empty().append(Text.translatable(label)).append(" ").append(value);
	}

	private static Text noData() {
		return Text.translatable("emi_world.no_data.server").formatted(Formatting.GRAY, Formatting.ITALIC);
	}
}
