package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.JsonOps;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.remastered.tools.recipe.RecipeSnapshot;
import dev.emi.remastered.tools.recipe.RecipeSnapshot.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Converts an {@link EmiRecipe} into the plain-data {@link RecipeSnapshot}. */
final class RecipeSnapshots {
	private RecipeSnapshots() {
	}

	static RecipeSnapshot of(EmiRecipe recipe) {
		EmiRecipeCategory category = recipe.getCategory();
		Identifier id = recipe.getId();
		return new RecipeSnapshot(id == null ? null : id.toString(),
			category.getId().toString(), category.getName().getString(),
			ingredients(recipe.getInputs()), stacks(recipe.getOutputs()), ingredients(recipe.getCatalysts()));
	}

	private static List<Slot> ingredients(List<? extends EmiIngredient> list) {
		List<Slot> slots = new ArrayList<>(list.size());
		for (EmiIngredient ingredient : list) {
			slots.add(slot(ingredient));
		}
		return slots;
	}

	private static List<Slot> stacks(List<EmiStack> list) {
		return ingredients(list);
	}

	static Slot slot(EmiIngredient ingredient) {
		if (ingredient.isEmpty()) {
			return Slot.empty();
		}
		if (ingredient instanceof TagEmiIngredient tag) {
			return tag(tag);
		}
		List<EmiStack> stacks = ingredient.getEmiStacks();
		if (ingredient instanceof EmiStack || stacks.size() == 1) {
			return stack(stacks.get(0), ingredient.getAmount(), ingredient.getChance());
		}
		List<Slot> options = new ArrayList<>(stacks.size());
		for (EmiStack stack : stacks) {
			options.add(stack(stack, 1, 1));
		}
		return Slot.anyOf(options, ingredient.getAmount(), ingredient.getChance());
	}

	private static Slot tag(TagEmiIngredient tag) {
		TagKey<?> key = tag.key;
		Identifier registry = key.registry().getValue();
		String type = registry.getNamespace().equals("minecraft") ? registry.getPath() : registry.toString();
		return Slot.tag(type, key.id().toString(), EmiTagKey.of(key).getTagName().getString(), tag.getEmiStacks().size(),
			tag.getAmount(), tag.getChance());
	}

	private static Slot stack(EmiStack stack, long amount, float chance) {
		String type = stack.getKey() instanceof Item ? "item" : stack.getKey() instanceof Fluid ? "fluid" : "stack";
		return Slot.stack(type, stack.getId().toString(), stack.getName().getString(), amount, chance, components(stack));
	}

	private static @Nullable String components(EmiStack stack) {
		ComponentChanges changes = stack.getComponentChanges();
		if (changes == null || changes.isEmpty()) {
			return null;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world != null) {
			return ComponentChanges.CODEC.encodeStart(client.world.getRegistryManager().getOps(JsonOps.INSTANCE), changes)
				.result().map(Object::toString).orElse(changes.toString());
		}
		return changes.toString();
	}
}
