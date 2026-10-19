package dev.emi.remastered.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.loot.structure.StructureMatcher;
import dev.emi.remastered.loot.structure.StructureNames;
import dev.emi.remastered.loot.structure.StructureRef;
import dev.emi.remastered.loot.util.ConditionKind;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

class LootExtrasTest {
	private static final List<String> VANILLA = List.of(
		"minecraft:pillager_outpost", "minecraft:mineshaft", "minecraft:mineshaft_mesa", "minecraft:mansion",
		"minecraft:jungle_pyramid", "minecraft:desert_pyramid", "minecraft:igloo", "minecraft:shipwreck",
		"minecraft:shipwreck_beached", "minecraft:swamp_hut", "minecraft:stronghold", "minecraft:monument",
		"minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm", "minecraft:fortress", "minecraft:end_city",
		"minecraft:buried_treasure", "minecraft:bastion_remnant", "minecraft:village_plains", "minecraft:village_desert",
		"minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga", "minecraft:ruined_portal",
		"minecraft:ruined_portal_nether", "minecraft:ancient_city", "minecraft:trail_ruins", "minecraft:trial_chambers");

	@Test
	void matchesVillageTablesToAllVillageVariants() {
		assertEquals(
			List.of("minecraft:village_desert", "minecraft:village_plains", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"),
			StructureMatcher.match("minecraft:chests/village/village_armorer", VANILLA));
	}

	@Test
	void matchesVanillaNamesThatDiffer() {
		assertEquals(List.of("minecraft:bastion_remnant"), StructureMatcher.match("minecraft:chests/bastion_treasure", VANILLA));
		assertEquals(List.of("minecraft:fortress"), StructureMatcher.match("minecraft:chests/nether_bridge", VANILLA));
		assertEquals(List.of("minecraft:jungle_pyramid"), StructureMatcher.match("minecraft:chests/jungle_temple_dispenser", VANILLA));
		assertEquals(List.of("minecraft:mansion"), StructureMatcher.match("minecraft:chests/woodland_mansion", VANILLA));
		assertEquals(List.of("minecraft:igloo"), StructureMatcher.match("minecraft:chests/igloo_chest", VANILLA));
		assertEquals(List.of("minecraft:mineshaft", "minecraft:mineshaft_mesa"), StructureMatcher.match("minecraft:chests/abandoned_mineshaft", VANILLA));
		assertEquals(List.of("minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"), StructureMatcher.match("minecraft:chests/underwater_ruin_big", VANILLA));
		assertEquals(List.of("minecraft:shipwreck", "minecraft:shipwreck_beached"), StructureMatcher.match("minecraft:chests/shipwreck_supply", VANILLA));
		assertEquals(List.of("minecraft:stronghold"), StructureMatcher.match("minecraft:chests/stronghold_library", VANILLA));
		assertEquals(List.of("minecraft:ancient_city"), StructureMatcher.match("minecraft:chests/ancient_city_ice_box", VANILLA));
		assertEquals(List.of("minecraft:ruined_portal", "minecraft:ruined_portal_nether"), StructureMatcher.match("minecraft:chests/ruined_portal", VANILLA));
		assertEquals(List.of("minecraft:trial_chambers"), StructureMatcher.match("minecraft:chests/trial_chambers/reward_ominous", VANILLA));
		assertEquals(List.of("minecraft:buried_treasure"), StructureMatcher.match("minecraft:chests/buried_treasure", VANILLA));
	}

	@Test
	void findsNothingForTablesWithoutAStructure() {
		assertTrue(StructureMatcher.match("minecraft:chests/simple_dungeon", VANILLA).isEmpty());
		assertTrue(StructureMatcher.match("minecraft:chests/spawn_bonus_chest", VANILLA).isEmpty());
		assertTrue(StructureMatcher.match("minecraft:entities/zombie", VANILLA).isEmpty());
		assertTrue(StructureMatcher.match("minecraft:gameplay/fishing", VANILLA).isEmpty());
	}

	@Test
	void matchesModdedTablesByNameInTheirOwnNamespace() {
		List<String> structures = List.of("mod:skeleton_dungeon", "other:skeleton_dungeon", "minecraft:village_plains");
		assertEquals(List.of("mod:skeleton_dungeon"), StructureMatcher.match("mod:chests/skeleton_dungeon", structures));
		assertEquals(List.of("mod:skeleton_dungeon"), StructureMatcher.match("mod:chests/skeleton_dungeon/loot", structures));
		assertTrue(StructureMatcher.match("mod:chests/village", structures).isEmpty());
	}

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
}
