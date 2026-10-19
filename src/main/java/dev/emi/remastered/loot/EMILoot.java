package dev.emi.remastered.loot;

import com.google.gson.JsonElement;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.server.ServerResourceData;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryOps;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Shared constants and small helpers of the loot module. Everything here is safe to load on a dedicated server.
 */
public class EMILoot {

    public static final String MOD_ID = "emi_loot";
    public static final Logger LOGGER = LoggerFactory.getLogger("emi_loot");
    public static Random emiLootRandom = new LocalRandom(System.currentTimeMillis());
    public static LootTableParser parser = new LootTableParser();

    //conditions & functions will be used in Lootify also, copying the identifier here so both mods can serialize the same conditions separately
    public static Supplier<LootConditionType> WITHER_KILL;
    public static Supplier<LootConditionType> SPAWNS_WITH;
    public static Supplier<LootConditionType> CREEPER;
    public static Supplier<LootFunctionType<?>> SET_ANY_DAMAGE;
    public static Supplier<LootFunctionType<?>> OMINOUS_BANNER;

    public static Identifier identity(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static boolean isDebug() {
        return LootSettings.debug;
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @SuppressWarnings("deprecation")
    public static String getModName(String namespace) {
        if (namespace.equals("c")) {
            return "Common";
        }
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(namespace);
        if (container.isPresent()) {
            return container.get().getMetadata().getName();
        }
        container = FabricLoader.getInstance().getModContainer(namespace.replace('_', '-'));
        if (container.isPresent()) {
            return container.get().getMetadata().getName();
        }
        return WordUtils.capitalizeFully(namespace.replace('_', ' '));
    }

    public static void parseTables(ResourceManager resourceManager, Registry<LootTable> lootManager, RegistryOps<JsonElement> ops) {
        LootTableParser.registryOps = ops;
        ServerResourceData.loadDirectTables(resourceManager, ops);
        LootTableParser.parseLootTables(lootManager);
    }

    public enum Type {
        BLOCK(() -> LootSettings.compactBlock, () -> LootSettings.debugBlock, () -> false, () -> LootSettings.directDropsBlock),
        CHEST(() -> LootSettings.compactChest, () -> LootSettings.debugChest, () -> LootSettings.logUntranslatedChest || LootSettings.isDevelopment(), () -> LootSettings.directDropsChest),
        MOB(() -> LootSettings.compactMob, () -> LootSettings.debugMob, () -> false, () -> LootSettings.directDropsMob),
        GAMEPLAY(() -> LootSettings.compactGameplay, () -> LootSettings.debugGameplay, () -> LootSettings.logUntranslatedGameplay || LootSettings.isDevelopment(), () -> LootSettings.directDropsGameplay),
        ARCHAEOLOGY(() -> LootSettings.compactArchaeology, () -> LootSettings.debugArchaeology, () -> LootSettings.logUntranslatedArchaeology || LootSettings.isDevelopment(), () -> LootSettings.directDropsArchaeology);

        private final BooleanSupplier compactLootSupplier;
        private final BooleanSupplier debugSupplier;
        private final BooleanSupplier logUntranslatedTablesSupplier;
        private final BooleanSupplier doDirectDrops;

        Type(BooleanSupplier compactLootSupplier, BooleanSupplier debugSupplier, BooleanSupplier logUntranslatedTablesSupplier, BooleanSupplier doDirectDrops) {
            this.compactLootSupplier = compactLootSupplier;
            this.debugSupplier = debugSupplier;
            this.logUntranslatedTablesSupplier = logUntranslatedTablesSupplier;
            this.doDirectDrops = doDirectDrops;
        }

        public boolean isCompact() {
            return compactLootSupplier.getAsBoolean();
        }

        public boolean isDebug() {
            return debugSupplier.getAsBoolean();
        }

        public boolean isLogI18n() {
            return logUntranslatedTablesSupplier.getAsBoolean();
        }

        public boolean doDirectDrops() {
            return doDirectDrops.getAsBoolean();
        }
    }
}
