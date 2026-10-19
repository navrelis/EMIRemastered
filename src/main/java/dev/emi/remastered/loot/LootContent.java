package dev.emi.remastered.loot;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.info.api.ItemInfoRegistry;
import dev.emi.remastered.loot.client.ClientLootTablesReceiver;
import dev.emi.remastered.loot.client.ClientResourceData;
import dev.emi.remastered.loot.emi.LootEmiRegistration;
import dev.emi.remastered.loot.info.LootInfoProvider;
import dev.emi.remastered.loot.networking.LootNetworking;
import dev.emi.remastered.loot.server.ChestStructureSync;
import dev.emi.remastered.loot.server.condition.BlownUpByCreeperLootCondition;
import dev.emi.remastered.loot.server.condition.KilledByWitherLootCondition;
import dev.emi.remastered.loot.server.condition.MobSpawnedWithLootCondition;
import dev.emi.remastered.loot.server.function.OminousBannerLootFunction;
import dev.emi.remastered.loot.server.function.SetAnyDamageLootFunction;
import dev.emi.remastered.loot.structure.StructureLookup;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * Entry points of the loot module (formerly EMI Loot). Nothing reachable from {@link #init()} touches client classes.
 */
public class LootContent {

	/** Runs on the client and on the dedicated server, also when the module is switched off on the client. */
	public static void init() {
		registerLootTypes();
		LootNetworking.registerPayloads();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (LootSettings.chestStructures) {
				StructureLookup.prefetch(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			StructureLookup.invalidate();
			ChestStructureSync.clear();
		});
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> EMILoot.parser.registerServer(player));
	}

	/** Client only, called when the module is enabled. */
	public static void initClient() {
		ClientLootTablesReceiver.register();
		ItemInfoRegistry.register(LootInfoProvider.INSTANCE);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new ClientResourceData.EntityOffsetsReloadListener());
	}

	public static void register(EmiRegistry registry) {
		LootEmiRegistration.register(registry);
	}

	/**
	 * Marker condition and function types the direct drop data files use. They live in the "lootify" namespace, as
	 * upstream, so both mods can read the same data. If another mod already registered them, theirs are reused.
	 */
	private static void registerLootTypes() {
		LootConditionType witherKill = registerCondition("wither_kill", KilledByWitherLootCondition.CODEC);
		LootConditionType spawnsWith = registerCondition("spawns_with", MobSpawnedWithLootCondition.CODEC);
		LootConditionType creeper = registerCondition("creeper", BlownUpByCreeperLootCondition.CODEC);
		LootFunctionType<?> setAnyDamage = registerFunction("set_any_damage", SetAnyDamageLootFunction.CODEC);
		LootFunctionType<?> ominousBanner = registerFunction("ominous_banner", OminousBannerLootFunction.CODEC);
		EMILoot.WITHER_KILL = supplier(witherKill);
		EMILoot.SPAWNS_WITH = supplier(spawnsWith);
		EMILoot.CREEPER = supplier(creeper);
		EMILoot.SET_ANY_DAMAGE = supplier(setAnyDamage);
		EMILoot.OMINOUS_BANNER = supplier(ominousBanner);
	}

	private static LootConditionType registerCondition(String path, MapCodec<? extends LootCondition> codec) {
		Identifier id = Identifier.of("lootify", path);
		if (Registries.LOOT_CONDITION_TYPE.containsId(id)) {
			return Registries.LOOT_CONDITION_TYPE.get(id);
		}
		return Registry.register(Registries.LOOT_CONDITION_TYPE, id, new LootConditionType(codec));
	}

	private static <T extends LootFunction> LootFunctionType<?> registerFunction(String path, MapCodec<T> codec) {
		Identifier id = Identifier.of("lootify", path);
		if (Registries.LOOT_FUNCTION_TYPE.containsId(id)) {
			return Registries.LOOT_FUNCTION_TYPE.get(id);
		}
		return Registry.register(Registries.LOOT_FUNCTION_TYPE, id, new LootFunctionType<>(codec));
	}

	private static <T> Supplier<T> supplier(T value) {
		return () -> value;
	}
}
