package dev.emi.remastered.stats;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.emi.remastered.stats.core.StatsMath;
import dev.emi.remastered.stats.core.StatsMath.Mod;
import dev.emi.remastered.stats.core.StatsMath.Op;

class StatsMathTest {

	@Test
	void swordDamageAndSpeedAddToThePlayerBase() {
		// diamond sword: +6 damage, -2.4 speed on top of the bare hand
		double damage = StatsMath.apply(StatsMath.PLAYER_ATTACK_DAMAGE, List.of(new Mod(6, Op.ADD_VALUE)));
		double speed = StatsMath.apply(StatsMath.PLAYER_ATTACK_SPEED, List.of(new Mod(-2.4, Op.ADD_VALUE)));
		assertEquals(7.0, damage, 1e-9);
		assertEquals(1.6, speed, 1e-9);
		assertEquals(11.2, StatsMath.dps(damage, speed));
	}

	@Test
	void dpsIsRoundedToOneDecimalAndNeverNegative() {
		assertEquals(10.0, StatsMath.dps(8, 1.25));
		assertEquals(9.6, StatsMath.dps(9, 1.0666666));
		assertEquals(0.0, StatsMath.dps(-3, 2));
		assertEquals(0.0, StatsMath.dps(5, -1));
		assertEquals(11.2, StatsMath.round1(11.200000000000001));
		assertEquals(0.0, StatsMath.round1(-0.04));
	}

	@Test
	void modifierOperationsApplyInMinecraftsOrder() {
		List<Mod> mods = List.of(new Mod(2, Op.ADD_VALUE), new Mod(0.5, Op.ADD_MULTIPLIED_BASE), new Mod(0.1, Op.ADD_MULTIPLIED_TOTAL),
			new Mod(1, Op.ADD_VALUE));
		// base 4 + 3 = 7, + 7 * 0.5 = 10.5, * 1.1 = 11.55
		assertEquals(11.55, StatsMath.apply(4, mods), 1e-9);
		assertEquals(4.0, StatsMath.apply(4, List.of()), 1e-9);
	}

	@Test
	void cooldownIsTheInverseOfTheSpeed() {
		assertEquals(0.625, StatsMath.cooldownSeconds(1.6), 1e-9);
		assertEquals(0.0, StatsMath.cooldownSeconds(0));
	}

	@Test
	void saturationModifierIsRecoveredFromAbsoluteSaturation() {
		// cooked beef: nutrition 8, saturation 12.8 -> modifier 0.8
		assertEquals(0.8, StatsMath.saturationModifier(8, 12.8f), 1e-6);
		// golden carrot: nutrition 6, saturation 14.4 -> 1.2
		assertEquals(1.2, StatsMath.saturationModifier(6, 14.4f), 1e-6);
		assertEquals(0.0, StatsMath.saturationModifier(0, 3f));
	}

	@Test
	void fractionIsClamped() {
		assertEquals(0.4f, StatsMath.fraction(8, 20));
		assertEquals(1f, StatsMath.fraction(30, 20));
		assertEquals(0f, StatsMath.fraction(-1, 20));
		assertEquals(0f, StatsMath.fraction(5, 0));
	}

	@Test
	void shanksAlwaysAddUpToTen() {
		assertArrayEquals(new int[] {3, 1, 6}, StatsMath.shanks(7));
		assertArrayEquals(new int[] {10, 0, 0}, StatsMath.shanks(20));
		assertArrayEquals(new int[] {10, 0, 0}, StatsMath.shanks(35));
		assertArrayEquals(new int[] {0, 0, 10}, StatsMath.shanks(-2));
		for (int n = -3; n < 40; n++) {
			assertEquals(10, java.util.Arrays.stream(StatsMath.shanks(n)).sum());
		}
	}

	@Test
	void tierIsInferredFromTheDeniedTag() {
		assertEquals(Optional.of("diamond"), StatsMath.tierOfIncorrectTag("minecraft:incorrect_for_diamond_tool"));
		assertEquals(Optional.of("gold"), StatsMath.tierOfIncorrectTag("minecraft:incorrect_for_gold_tool"));
		assertEquals(Optional.of("ruby_alloy"), StatsMath.tierOfIncorrectTag("mymod:incorrect_for_ruby_alloy_tool"));
		assertEquals(Optional.of("wooden"), StatsMath.tierOfIncorrectTag("incorrect_for_wooden_tool"));
		assertEquals(Optional.empty(), StatsMath.tierOfIncorrectTag("minecraft:mineable/pickaxe"));
		assertEquals(Optional.empty(), StatsMath.tierOfIncorrectTag("minecraft:incorrect_for_tool"));
		assertEquals(Optional.empty(), StatsMath.tierOfIncorrectTag("minecraft:needs_iron_tool"));
	}

	@Test
	void tierOfASetOfTags() {
		assertEquals(Optional.of("iron"), StatsMath.tier(List.of("minecraft:mineable/axe", "minecraft:incorrect_for_iron_tool")));
		assertEquals(Optional.empty(), StatsMath.tier(List.of("minecraft:mineable/axe")));
		assertEquals(Optional.empty(), StatsMath.tier(List.of()));
	}

	@Test
	void keysAndFallbackNames() {
		assertEquals("emi_stats.target.minecraft.mineable.pickaxe", StatsMath.targetKey("minecraft", "mineable/pickaxe"));
		assertEquals("Ruby Alloy", StatsMath.humanize("ruby_alloy"));
		assertEquals("Diamond", StatsMath.humanize("diamond"));
		assertTrue(StatsMath.humanize("").isEmpty());
	}
}
