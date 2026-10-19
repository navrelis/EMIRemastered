package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.tools.tags.TagEntry;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A member of a tag as shown by the tag browser: its id and, where there is one, an EMI stack that stands for it
 * (items, blocks and fluids as themselves, entity types as their spawn egg).
 *
 * @param stack the stack, or null when the member has no item form (biomes, damage types, ...)
 */
record TagMember(String id, @Nullable EmiStack stack) {

	static List<TagMember> of(TagEntry tag) {
		List<TagMember> members = new ArrayList<>(tag.size());
		for (String id : tag.members()) {
			members.add(new TagMember(id, stackOf(tag.registry(), id)));
		}
		return members;
	}

	static @Nullable EmiStack stackOf(String registryId, String memberId) {
		Identifier id = Identifier.tryParse(memberId);
		if (id == null) {
			return null;
		}
		EmiStack stack = null;
		switch (registryId) {
			case "minecraft:item" -> stack = Registries.ITEM.getOrEmpty(id).map(EmiStack::of).orElse(null);
			case "minecraft:block" -> stack = Registries.BLOCK.getOrEmpty(id).map(EmiStack::of).orElse(null);
			case "minecraft:fluid" -> stack = Registries.FLUID.getOrEmpty(id).map(fluid -> EmiStack.of(fluid, 1)).orElse(null);
			case "minecraft:entity_type" -> {
				EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
				SpawnEggItem egg = type == null ? null : SpawnEggItem.forEntity(type);
				stack = egg == null ? null : EmiStack.of(egg);
			}
			default -> {
			}
		}
		return stack == null || stack.isEmpty() ? null : stack;
	}
}
