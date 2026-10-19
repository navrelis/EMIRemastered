package dev.emi.remastered.mobs.data;

import java.util.Map;

/**
 * Base attribute values that vanilla sets on the entity when it spawns, so the default attribute container of the
 * entity type does not show them. Only fixed values that were read from the 1.21.1 bytecode are listed; random ones
 * (horse health, jump strength, slime size, zombie reinforcements) and values that change with the mob's state (tamed
 * wolf, baby mobs) are not. Pure logic, no Minecraft classes.
 */
public final class SpawnOverrides {
	/**
	 * Attack damage set in {@code initialize}: {@code WitherSkeletonEntity} sets 4.0 (its attribute container says 2.0)
	 * and {@code PhantomEntity} sets 6.0 plus the phantom size, which is 0 on spawn (container: 2.0).
	 */
	private static final Map<String, Double> ATTACK_DAMAGE = Map.of(
		"minecraft:wither_skeleton", 4.0,
		"minecraft:phantom", 6.0);

	private SpawnOverrides() {
	}

	/** The attack damage a freshly spawned mob has: the override if there is one, else the container value (NaN stays NaN). */
	public static double attackDamage(String entityTypeId, double containerValue) {
		Double override = ATTACK_DAMAGE.get(entityTypeId);
		return override == null || Double.isNaN(containerValue) ? containerValue : override;
	}
}
