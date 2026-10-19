package dev.emi.remastered.loot.emi;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.client.ClientArchaeologyLootTable;
import dev.emi.remastered.loot.client.ClientBlockLootTable;
import dev.emi.remastered.loot.client.ClientChestLootTable;
import dev.emi.remastered.loot.client.ClientGameplayLootTable;
import dev.emi.remastered.loot.client.ClientLootTables;
import dev.emi.remastered.loot.client.ClientMobLootTable;
import dev.emi.remastered.loot.client.LootReceiver;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The EMI side of the loot module: categories, and recipes built from the tables the server sent.
 */
public class LootEmiRegistration {
    private static final Identifier TRIAL_ID = Identifier.of(EMILoot.MOD_ID, "trial_chamber");
    private static final Identifier LOOT_ID = Identifier.of(EMILoot.MOD_ID, "chest_loot");
    private static final Identifier BLOCK_ID = Identifier.of(EMILoot.MOD_ID, "block_drops");
    private static final Identifier MOB_ID = Identifier.of(EMILoot.MOD_ID, "mob_drops");
    private static final Identifier GAMEPLAY_ID = Identifier.of(EMILoot.MOD_ID, "gameplay_drops");
    private static final Identifier ARCHAEOLOGY_ID = Identifier.of(EMILoot.MOD_ID, "archaeology_drops");

    public static final EmiRecipeCategory TRIAL_CATEGORY = new EmiRecipeCategory(TRIAL_ID, EmiStack.of(Blocks.TRIAL_SPAWNER.asItem()), new LootSimplifiedRenderer(16, 16));
    public static final EmiRecipeCategory CHEST_CATEGORY = new EmiRecipeCategory(LOOT_ID, EmiStack.of(Blocks.CHEST.asItem()), new LootSimplifiedRenderer(0, 0));
    public static final EmiRecipeCategory BLOCK_CATEGORY = new EmiRecipeCategory(BLOCK_ID, EmiStack.of(Blocks.DIAMOND_ORE.asItem()), new LootSimplifiedRenderer(16, 0));
    public static final EmiRecipeCategory MOB_CATEGORY = new EmiRecipeCategory(MOB_ID, EmiStack.of(Blocks.ZOMBIE_HEAD.asItem()), new LootSimplifiedRenderer(0, 16));
    public static final EmiRecipeCategory GAMEPLAY_CATEGORY = new EmiRecipeCategory(GAMEPLAY_ID, EmiStack.of(Items.FISHING_ROD));
    public static final EmiRecipeCategory ARCHAEOLOGY_CATEGORY = new EmiRecipeCategory(ARCHAEOLOGY_ID, EmiStack.of(Items.BRUSH));

    private static CompletableFuture<List<ChestLootRecipe.ChestLootRecipeData>> chestFuture;
    private static CompletableFuture<List<BlockLootRecipe>> blockFuture;
    private static CompletableFuture<List<MobLootRecipe.MobLootRecipeData>> mobFuture;
    private static CompletableFuture<List<GameplayLootRecipe.GameplayLootRecipeData>> gameplayFuture;
    private static CompletableFuture<List<ArchaeologyLootRecipe.ArchaeologyLootRecipeData>> archaeologyFuture;

    /**
     * Starts converting the received loot tables into recipe data off the reload thread.
     */
    public static void initialize(EmiInitRegistry registry) {
        ClientLootTables tables = ClientLootTables.INSTANCE;
        chestFuture = startData("Chest", tables.chestLoots, lr -> ChestLootRecipe.ChestLootRecipeData.of((ClientChestLootTable) lr));
        blockFuture = startData("Block", tables.blockLoots, lr -> new BlockLootRecipe(BlockLootRecipe.BlockLootRecipeData.of((ClientBlockLootTable) lr)));
        mobFuture = startData("Mob", tables.mobLoots, lr -> MobLootRecipe.MobLootRecipeData.of((ClientMobLootTable) lr));
        gameplayFuture = startData("Gameplay", tables.gameplayLoots, lr -> GameplayLootRecipe.GameplayLootRecipeData.of((ClientGameplayLootTable) lr));
        archaeologyFuture = startData("Archaeology", tables.archaeologyLoots, lr -> ArchaeologyLootRecipe.ArchaeologyLootRecipeData.of((ClientArchaeologyLootTable) lr));
    }

    private static <D> CompletableFuture<List<D>> startData(String name, List<LootReceiver> source, Function<LootReceiver, D> factory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<LootReceiver> snapshot = new ArrayList<>(source);
                List<D> result = new ArrayList<>(snapshot.size());
                for (LootReceiver lr : snapshot) {
                    try {
                        result.add(factory.apply(lr));
                    } catch (Throwable e) {
                        EMILoot.LOGGER.error("Critical error encountered while creating a {} loot recipe; recipe skipped: {}", name, lr.getId(), e);
                    }
                }
                return result;
            } catch (Throwable e) {
                EMILoot.LOGGER.error("Critical error encountered while creating all {} Recipes. Recipes completely skipped!", name, e);
                return new ArrayList<>();
            }
        });
    }

    private static <D, R> CompletableFuture<List<R>> startRecipes(String name, CompletableFuture<List<D>> dataFuture, Function<D, R> factory) {
        if (dataFuture == null) {
            return null;
        }
        return dataFuture.thenApplyAsync(data -> {
            long t = Util.getMeasuringTimeNano();
            List<R> recipes = new ArrayList<>(data.size());
            for (D d : data) {
                try {
                    recipes.add(factory.apply(d));
                } catch (Throwable e) {
                    EMILoot.LOGGER.error("Critical error encountered while building a {} loot recipe; recipe skipped", name, e);
                }
            }
            EMILoot.LOGGER.info("Built {} {} loot recipes in {}ms", recipes.size(), name.toLowerCase(), (Util.getMeasuringTimeNano() - t)/1000000);
            return recipes;
        }).exceptionally(e -> {
            EMILoot.LOGGER.error("Critical error encountered while registering all {} Recipes. Recipes completely skipped!", name, e);
            return null;
        });
    }

    public static void register(EmiRegistry registry) {
        registry.addCategory(TRIAL_CATEGORY);
        registry.addCategory(CHEST_CATEGORY);
        registry.addCategory(BLOCK_CATEGORY);
        registry.addCategory(MOB_CATEGORY);
        registry.addCategory(GAMEPLAY_CATEGORY);
        registry.addCategory(ARCHAEOLOGY_CATEGORY);

        CompletableFuture<List<ChestLootRecipe>> chestRecipeFuture = startRecipes("Chest", chestFuture, ChestLootRecipe::new);
        CompletableFuture<List<BlockLootRecipe>> blockRecipeFuture = startRecipes("Block", blockFuture, BlockLootRecipe::init);
        CompletableFuture<List<MobLootRecipe>> mobRecipeFuture = startRecipes("Mob", mobFuture, MobLootRecipe::new);
        CompletableFuture<List<GameplayLootRecipe>> gameplayRecipeFuture = startRecipes("Gameplay", gameplayFuture, GameplayLootRecipe::new);
        CompletableFuture<List<ArchaeologyLootRecipe>> archaeologyRecipeFuture = startRecipes("Archaeology", archaeologyFuture, ArchaeologyLootRecipe::new);

        long t = Util.getMeasuringTimeNano();
        registerAll(registry, gameplayRecipeFuture, "Gameplay");
        EMILoot.LOGGER.info("Registered gameplay loot recipes after {}ms", (Util.getMeasuringTimeNano() - t)/1000000);
        registerAll(registry, archaeologyRecipeFuture, "Archaeology");
        EMILoot.LOGGER.info("Registered archaeology loot recipes after {}ms", (Util.getMeasuringTimeNano() - t)/1000000);
        registerAll(registry, mobRecipeFuture, "Mob");
        EMILoot.LOGGER.info("Registered mob loot recipes after {}ms", (Util.getMeasuringTimeNano() - t)/1000000);
        registerAll(registry, chestRecipeFuture, "Chest");
        EMILoot.LOGGER.info("Registered chest loot recipes after {}ms", (Util.getMeasuringTimeNano() - t)/1000000);
        registerAll(registry, blockRecipeFuture, "Block");
        EMILoot.LOGGER.info("Registered all loot recipes after {}ms", (Util.getMeasuringTimeNano() - t)/1000000);
        chestFuture = null;
        blockFuture = null;
        mobFuture = null;
        gameplayFuture = null;
        archaeologyFuture = null;
    }

    private static <R extends EmiRecipe> void registerAll(EmiRegistry registry, CompletableFuture<List<R>> future, String name) {
        if (future == null) {
            EMILoot.LOGGER.error("{} loot recipe future null for an unknown reason; there may have been a problem during EMI initialization", name);
            EMILoot.LOGGER.error("{} recipes skipped!", name);
            return;
        }
        List<R> recipes;
        try {
            recipes = future.join();
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while registering all {} Recipes. Recipes completely skipped!", name, e);
            return;
        }
        if (recipes == null) {
            // already logged where the future failed
            return;
        }
        for (R recipe : recipes) {
            try {
                registry.addRecipe(recipe);
            } catch (Throwable e) {
                EMILoot.LOGGER.error("Failed to register a {} loot recipe: {}", name, recipe.getId(), e);
            }
        }
    }
}
