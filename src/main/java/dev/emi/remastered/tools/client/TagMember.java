package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.tools.tags.TagEntry;
import dev.emi.remastered.tools.tags.TagNames;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A member of a tag as shown by the tag browser: its id and, where there is one, an EMI stack that stands for it
 * (items, blocks and fluids as themselves, entity types as their spawn egg).
 *
 * @param stack the stack, or null when the member has no item form (biomes, damage types, ...)
 * @param label the name to show: the translated name where the registry has one (biomes), else the id
 */
record TagMember(String id, @Nullable EmiStack stack, String label) {

	static List<TagMember> of(TagEntry tag) {
		List<TagMember> members = new ArrayList<>(tag.size());
		for (String id : tag.members()) {
			EmiStack stack = stackOf(tag.registry(), id);
			members.add(new TagMember(id, stack, stack == null ? labelOf(tag.registry(), id) : id));
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

	/** The translated name of a member that has no stack (a biome is {@code biome.<namespace>.<path>}), else its id. */
	static String labelOf(String registryId, String memberId) {
		String key = TagNames.memberKey(registryId, memberId);
		return key != null && I18n.hasTranslation(key) ? I18n.translate(key) : memberId;
	}
}
