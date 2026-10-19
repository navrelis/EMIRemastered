package dev.emi.remastered.mobs.data;

/**
 * One natural spawn entry of a biome: the mob (entity type id), the biome id, the spawn group name, the pool weight
 * and the group size range. Plain strings and ints so that the logic around it is testable without Minecraft.
 */
public record SpawnRow(String entity, String biome, String group, int weight, int minGroup, int maxGroup) {
}
