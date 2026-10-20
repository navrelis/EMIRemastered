package dev.emi.remastered.loot.emi;

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
import net.minecraft.client.MinecraftClient;
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

    private static final Function<LootReceiver, MobLootRecipe.MobLootRecipeData> MOB_DATA = lr -> MobLootRecipe.MobLootRecipeData.of((ClientMobLootTable) lr);
    /** Mob recipe data started early by {@link #prestart()}, with the tables it was started for; only touched by EMI's reload thread. */
    private static Prestarted<MobLootRecipe.MobLootRecipeData> mobPrestart;

    private record Prestarted<D>(List<LootReceiver> tables, CompletableFuture<List<D>> data) {
    }

    /**
     * Called from the plugin's initialize step, seconds before {@link #register}: starts the mob recipe data off the
     * reload thread already. It creates one entity per mob table, which on the first load of a game session is by far
     * the slowest part of the loot plugin (entity classes are loaded and transformed); started here it runs while the
     * other plugins register. {@link #register} only uses the result if the received mob tables are still exactly the
     * same; otherwise it waits for it and builds the data again (then with the entity classes already loaded).
     */
    public static void prestart() {
        Prestarted<MobLootRecipe.MobLootRecipeData> previous = mobPrestart;
        mobPrestart = null;
        if (previous != null) {
            // never two builds of the same tables at once (a reload that restarted before it registered)
            previous.data().join();
        }
        if (worldGone()) {
            return;
        }
        List<LootReceiver> tables = new ArrayList<>(ClientLootTables.INSTANCE.mobLoots);
        if (!tables.isEmpty()) {
            mobPrestart = new Prestarted<>(tables, startData("Mob", tables, MOB_DATA));
        }
    }

    /**
     * Starts converting the received loot tables into recipe data off the reload thread.
     */
    private static void startAll() {
        ClientLootTables tables = ClientLootTables.INSTANCE;
        chestFuture = startData("Chest", new ArrayList<>(tables.chestLoots), lr -> ChestLootRecipe.ChestLootRecipeData.of((ClientChestLootTable) lr));
        blockFuture = startData("Block", new ArrayList<>(tables.blockLoots), lr -> new BlockLootRecipe(BlockLootRecipe.BlockLootRecipeData.of((ClientBlockLootTable) lr)));
        mobFuture = mobData(new ArrayList<>(tables.mobLoots));
        gameplayFuture = startData("Gameplay", new ArrayList<>(tables.gameplayLoots), lr -> GameplayLootRecipe.GameplayLootRecipeData.of((ClientGameplayLootTable) lr));
        archaeologyFuture = startData("Archaeology", new ArrayList<>(tables.archaeologyLoots), lr -> ArchaeologyLootRecipe.ArchaeologyLootRecipeData.of((ClientArchaeologyLootTable) lr));
    }

    private static CompletableFuture<List<MobLootRecipe.MobLootRecipeData>> mobData(List<LootReceiver> tables) {
        Prestarted<MobLootRecipe.MobLootRecipeData> early = mobPrestart;
        mobPrestart = null;
        if (early != null) {
            if (sameTables(early.tables(), tables)) {
                EMILoot.LOGGER.info("Using the mob loot recipe data of {} tables started while the EMI plugins were initialized", tables.size());
                return early.data();
            }
            early.data().join();
            EMILoot.LOGGER.info("The mob loot tables changed after the EMI plugins were initialized ({} -> {}); building their recipe data again", early.tables().size(), tables.size());
        }
        return startData("Mob", tables, MOB_DATA);
    }

    /** Same table objects in the same order. */
    private static boolean sameTables(List<?> a, List<?> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static <D> CompletableFuture<List<D>> startData(String name, List<LootReceiver> snapshot, Function<LootReceiver, D> factory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<D> result = new ArrayList<>(snapshot.size());
                BatchFailureLog failures = new BatchFailureLog(EMILoot.LOGGER, name + " loot recipe data");
                for (LootReceiver lr : snapshot) {
                    if (worldGone()) {
                        EMILoot.LOGGER.warn("The world is gone while creating {} loot recipes; the rest of the batch is skipped ({} of {} done)", name, result.size(), snapshot.size());
                        break;
                    }
                    try {
                        result.add(factory.apply(lr));
                    } catch (Throwable e) {
                        failures.fail("creating a " + name + " loot recipe: " + lr.getId(), e);
                    }
                }
                failures.summarize();
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
            BatchFailureLog failures = new BatchFailureLog(EMILoot.LOGGER, name + " loot recipes");
            for (D d : data) {
                if (worldGone()) {
                    EMILoot.LOGGER.warn("The world is gone while building {} loot recipes; the rest of the batch is skipped ({} of {} done)", name, recipes.size(), data.size());
                    break;
                }
                try {
                    recipes.add(factory.apply(d));
                } catch (Throwable e) {
                    failures.fail("building a " + name + " loot recipe", e);
                }
            }
            failures.summarize();
            EMILoot.LOGGER.info("Built {} {} loot recipes in {}ms", recipes.size(), name.toLowerCase(), (Util.getMeasuringTimeNano() - t)/1000000);
            return recipes;
        }).exceptionally(e -> {
            EMILoot.LOGGER.error("Critical error encountered while registering all {} Recipes. Recipes completely skipped!", name, e);
            return null;
        });
    }

    /**
     * The loot recipes need the registries of the client world; without a world every recipe would fail.
     */
    private static boolean worldGone() {
        return MinecraftClient.getInstance().world == null;
    }

    public static void register(EmiRegistry registry) {
        registry.addCategory(TRIAL_CATEGORY);
        registry.addCategory(CHEST_CATEGORY);
        registry.addCategory(BLOCK_CATEGORY);
        registry.addCategory(MOB_CATEGORY);
        registry.addCategory(GAMEPLAY_CATEGORY);
        registry.addCategory(ARCHAEOLOGY_CATEGORY);
        if (worldGone()) {
            EMILoot.LOGGER.warn("The world is gone; all loot recipes are skipped for this reload");
            return;
        }
        startAll();

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
        BatchFailureLog failures = new BatchFailureLog(EMILoot.LOGGER, name + " loot recipe registration");
        for (R recipe : recipes) {
            try {
                registry.addRecipe(recipe);
            } catch (Throwable e) {
                failures.fail("registering a " + name + " loot recipe: " + recipe.getId(), e);
            }
        }
        failures.summarize();
    }
}
