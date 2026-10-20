package dev.emi.remastered.info.sources;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.EmiRemastered;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Reads every {@code assets/<namespace>/emi_sources/**.json} of the resource manager, validates it
 * ({@link SourceParser}), resolves the item ids and builds the recipes. Client only, runs inside EMI's reload.
 */
public final class SourceLoader {
	public static final String FOLDER = "emi_sources";

	private SourceLoader() {
	}

	/** The recipes and how many files and entries were read or skipped. */
	public record Result(List<EmiRecipe> recipes, int files, int skipped) {
	}

	public static Result load(ResourceManager manager) {
		Map<String, Resource> files = new TreeMap<>();
		Map<Identifier, Resource> found = manager.findResources(FOLDER, id -> id.getPath().endsWith(".json"));
		for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
			files.put(e.getKey().toString(), e.getValue());
		}
		List<EmiRecipe> recipes = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		int[] skipped = {0};
		SourceParser.Problems problems = (file, index, reason) -> {
			skipped[0]++;
			EmiRemastered.LOGGER.warn("emi_sources: {}{}: {}", file, index < 0 ? "" : " entry " + index, reason);
		};
		Predicate<String> modLoaded = FabricLoader.getInstance()::isModLoaded;
		for (Map.Entry<String, Resource> file : files.entrySet()) {
			String json;
			try (BufferedReader reader = file.getValue().getReader()) {
				json = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
			} catch (IOException | RuntimeException e) {
				problems.report(file.getKey(), -1, "could not be read: " + e.getMessage());
				continue;
			}
			for (SourceEntry entry : SourceParser.parse(file.getKey(), json, modLoaded, problems)) {
				try {
					EmiRecipe recipe = build(entry);
					if (!ids.add(entry.id())) {
						problems.report(entry.fileId(), entry.index(), "id " + entry.id() + " is already used by another file");
					} else if (recipe != null) {
						recipes.add(recipe);
					}
				} catch (ResolveException e) {
					problems.report(entry.fileId(), entry.index(), e.getMessage());
				}
			}
		}
		return new Result(recipes, files.size(), skipped[0]);
	}

	private static EmiRecipe build(SourceEntry entry) {
		Item item = item(entry.output(), "output");
		EmiStack output = EmiStack.of(item, entry.count());
		if (entry.chance() != null) {
			output.setChance(entry.chance().floatValue());
		}
		List<EmiIngredient> inputs = new ArrayList<>();
		for (String from : entry.from()) {
			if (from.startsWith("#")) {
				Identifier tag = Identifier.tryParse(from.substring(1));
				EmiIngredient ingredient = tag == null ? EmiStack.EMPTY : EmiIngredient.of(TagKey.of(RegistryKeys.ITEM, tag));
				if (ingredient.isEmpty()) {
					throw new ResolveException("item tag " + from + " is unknown or empty");
				}
				inputs.add(ingredient);
			} else {
				inputs.add(EmiStack.of(item(from, "from")));
			}
		}
		Identifier id = EmiPort.id("emi_info", entry.recipePath());
		return new OtherSourceRecipe(id, List.copyOf(inputs), output, entry.text(), entry.chance());
	}

	private static Item item(String text, String what) {
		Identifier id = Identifier.tryParse(text);
		if (id == null || !Registries.ITEM.containsId(id)) {
			throw new ResolveException(what + " item " + text + " does not exist");
		}
		Item item = Registries.ITEM.get(id);
		if (item == Items.AIR) {
			throw new ResolveException(what + " item " + text + " is air");
		}
		return item;
	}

	private static final class ResolveException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		ResolveException(String message) {
			super(message, null, false, false);
		}
	}
}
