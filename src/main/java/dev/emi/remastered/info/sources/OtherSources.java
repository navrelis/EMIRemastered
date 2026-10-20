package dev.emi.remastered.info.sources;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.info.InfoSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/**
 * The category {@code emi_info:other_sources} ("Other Sources") with the entries of the {@code emi_sources} resource
 * files, for sources that mods hand out in code and no loot table, recipe or world feature shows (docs/SOURCES.md).
 */
public final class OtherSources {
	public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(Identifier.of("emi_info", "other_sources"),
		EmiStack.of(Items.WRITABLE_BOOK), EmiStack.of(Items.WRITABLE_BOOK));

	private OtherSources() {
	}

	/** EMI registration, only while the {@code info} module is on. */
	public static void register(EmiRegistry registry) {
		if (!InfoSettings.extraSources) {
			return;
		}
		long start = System.nanoTime();
		SourceLoader.Result result = SourceLoader.load(MinecraftClient.getInstance().getResourceManager());
		if (!result.recipes().isEmpty()) {
			registry.addCategory(CATEGORY);
			for (EmiRecipe recipe : result.recipes()) {
				registry.addRecipe(recipe);
			}
		}
		EmiLog.info("emi_info: " + result.recipes().size() + " other sources from " + result.files() + " emi_sources files ("
			+ result.skipped() + " skipped) in " + (System.nanoTime() - start) / 1_000_000 + " ms");
	}
}
