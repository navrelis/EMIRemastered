package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.tools.tags.TagNames;

class TagNamesTest {
	@Test
	void tagKeyUsesTheRegistryPathAndDottedTagId() {
		assertEquals("tag.item.minecraft.logs", TagNames.tagKey("minecraft:item", "minecraft:logs"));
		assertEquals("tag.entity_type.minecraft.can_breathe_under_water",
			TagNames.tagKey("minecraft:entity_type", "minecraft:can_breathe_under_water"));
		assertEquals("tag.worldgen.biome.minecraft.is_forest", TagNames.tagKey("minecraft:worldgen/biome", "minecraft:is_forest"));
		assertEquals("tag.block.minecraft.mineable.pickaxe", TagNames.tagKey("minecraft:block", "minecraft:mineable/pickaxe"));
		assertEquals("tag.item.c.ingots.iron", TagNames.tagKey("minecraft:item", "c:ingots/iron"));
	}

	@Test
	void readableTakesTheLastSegmentWithCapitalWords() {
		assertEquals("Can Breathe Under Water", TagNames.readable("minecraft:can_breathe_under_water"));
		assertEquals("Pickaxe", TagNames.readable("minecraft:mineable/pickaxe"));
		assertEquals("Cold", TagNames.readable("c:cold"));
		assertEquals("Logs", TagNames.readable("logs"));
	}

	@Test
	void readableKeepsTheIdWhenThereIsNothingToShow() {
		assertEquals("c:dead/", TagNames.readable("c:dead/"));
	}

	@Test
	void membersOfNamedRegistriesHaveATranslationKey() {
		assertEquals("biome.minecraft.forest", TagNames.memberKey("minecraft:worldgen/biome", "minecraft:forest"));
		assertEquals("biome.terralith.volcanic_peaks", TagNames.memberKey("minecraft:worldgen/biome", "terralith:volcanic_peaks"));
		assertEquals("entity.minecraft.husk", TagNames.memberKey("minecraft:entity_type", "minecraft:husk"));
	}

	@Test
	void registriesWithoutNamesHaveNoMemberKey() {
		assertNull(TagNames.memberKey("minecraft:damage_type", "minecraft:fall"));
	}
}
