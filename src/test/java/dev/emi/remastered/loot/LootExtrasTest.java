package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.loot.structure.StructureNames;
import dev.emi.remastered.loot.structure.StructureRef;
import dev.emi.remastered.loot.util.ConditionKind;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

class LootExtrasTest {
	@Test
	void prettifiesAndMergesNames() {
		assertEquals("Ocean Ruin Cold", StructureNames.prettify("ocean_ruin_cold"));
		assertEquals("Skeleton Dungeon Main", StructureNames.prettify("skeleton_dungeon/main"));
		Map<String, List<String>> merged = StructureNames.merge(List.of(
			Map.entry("Village", List.of("Overworld")),
			Map.entry("Bastion Remnant", List.of("Nether")),
			Map.entry("Village", List.of("Overworld"))));
		assertEquals(List.of("Village", "Bastion Remnant"), List.copyOf(merged.keySet()));
		assertEquals(List.of("Overworld"), merged.get("Village"));
	}

	@Test
	void classifiesConditionIcons() {
		Set<String> silk = Set.of("emi_loot.condition.match_tool", "emi_loot.item_predicate.enchant", "enchantment.minecraft.silk_touch");
		assertEquals(ConditionKind.SILK_TOUCH, ConditionKind.classify("emi_loot.condition.match_tool", silk));
		assertEquals(ConditionKind.NO_SILK_TOUCH, ConditionKind.classify("emi_loot.condition.invert", Set.of("emi_loot.condition.invert", "enchantment.minecraft.silk_touch")));
		assertNull(ConditionKind.classify("emi_loot.condition.match_tool", Set.of("emi_loot.condition.match_tool", "item.minecraft.shears")));

		assertEquals(ConditionKind.LOOTING, ConditionKind.classify("emi_loot.function.looting", Set.of("emi_loot.function.looting")));
		assertEquals(ConditionKind.LOOTING, ConditionKind.classify("emi_loot.condition.chance_looting", Set.of("emi_loot.condition.chance_looting", "enchantment.minecraft.looting")));
		assertEquals(ConditionKind.FORTUNE, ConditionKind.classify("emi_loot.function.bonus", Set.of("emi_loot.function.bonus", "enchantment.minecraft.fortune")));
		assertEquals(ConditionKind.FORTUNE, ConditionKind.classify("emi_loot.condition.table_bonus", Set.of("emi_loot.condition.table_bonus", "enchantment.minecraft.fortune")));
		assertNull(ConditionKind.classify("emi_loot.function.bonus", Set.of("emi_loot.function.bonus", "enchantment.minecraft.unbreaking")));

		assertEquals(ConditionKind.KILLED_BY_PLAYER, ConditionKind.classify("emi_loot.condition.killed_player", Set.of("emi_loot.condition.killed_player")));
		assertEquals(ConditionKind.SMELTED, ConditionKind.classify("emi_loot.function.smelt", Set.of("emi_loot.function.smelt")));
		assertEquals(ConditionKind.ON_FIRE, ConditionKind.classify("emi_loot.condition.entity_props", Set.of("emi_loot.condition.entity_props", "emi_loot.entity_predicate.fire_true")));
		assertNull(ConditionKind.classify("emi_loot.condition.entity_props", Set.of("emi_loot.condition.entity_props", "emi_loot.entity_predicate.fire_false")));
		assertNull(ConditionKind.classify(null, Set.of()));
		assertNull(ConditionKind.classify("emi_loot.condition.location", Set.of("enchantment.minecraft.silk_touch")));
	}

	@Test
	void everyConditionKindHasAHintKey() {
		for (ConditionKind kind : ConditionKind.values()) {
			assertTrue(kind.hintKey().startsWith("emi_loot.hint."));
			assertTrue(!kind.texture().isEmpty());
		}
	}

	@Test
	void structureBlockRoundTripsAndOldPayloadsHaveNone() {
		List<StructureRef> refs = List.of(
			new StructureRef(Identifier.of("minecraft", "village_plains"), List.of(Identifier.of("minecraft", "overworld"))),
			new StructureRef(Identifier.of("minecraft", "bastion_remnant"), List.of(Identifier.of("minecraft", "the_nether"))));
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeVarInt(42);
		StructureRef.write(buf, refs);
		assertEquals(42, buf.readVarInt());
		assertEquals(refs, StructureRef.read(buf));

		PacketByteBuf old = new PacketByteBuf(Unpooled.buffer());
		old.writeVarInt(42);
		assertEquals(42, old.readVarInt());
		assertTrue(StructureRef.read(old).isEmpty());

		PacketByteBuf none = new PacketByteBuf(Unpooled.buffer());
		StructureRef.write(none, List.of());
		assertEquals(0, none.readableBytes());
	}

	@Test
	void newTextsAreTranslated() throws Exception {
		List<String> keys = new java.util.ArrayList<>(List.of("emi_loot.found_in", "emi_loot.lootr_note", "emi_loot.info.structures",
			"emi_loot.info.in_chest", "emi_loot.info.more", "emi_loot.info.conditions", "emi_loot.dimension.minecraft:overworld",
			"emi_loot.dimension.minecraft:the_nether", "emi_loot.dimension.minecraft:the_end"));
		for (ConditionKind kind : ConditionKind.values()) {
			keys.add(kind.hintKey());
			keys.add(kind.labelKey());
		}
		for (String lang : List.of("en_us", "de_de")) {
			try (java.io.InputStream in = LootExtrasTest.class.getClassLoader().getResourceAsStream("assets/emi_loot/lang/" + lang + ".json")) {
				com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
				for (String key : keys) {
					assertTrue(json.has(key), lang + " " + key);
				}
			}
		}
	}
}
