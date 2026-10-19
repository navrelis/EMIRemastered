package dev.emi.remastered.loot.emi;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.client.ClientBuiltPool;
import dev.emi.remastered.loot.client.ClientGameplayLootTable;
import dev.emi.remastered.loot.util.ConditionalStack;
import dev.emi.remastered.loot.util.FloatTrimmer;
import dev.emi.remastered.loot.util.stack.GameplayLootEmiStack;
import dev.emi.remastered.loot.util.IconGroupEmiWidget;
import dev.emi.remastered.loot.util.InteractableTextWidget;
import dev.emi.remastered.loot.util.ClientText;
import dev.emi.remastered.loot.util.ConditionIcons;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.WidgetRowBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GameplayLootRecipe implements EmiRecipe {

    public GameplayLootRecipe(GameplayLootRecipeData data) {
        this.loot = data.loot;

        List<EmiStack> list = new ArrayList<>();
        data.loot.builtItems.forEach((builtPool)-> {
                builtPool.stacks().forEach(stack -> {
                    list.addAll(stack.getStacks());
                });
                addWidgetBuilders(builtPool, false);
            }
        );
        outputStacks = list;
        inputStack = new GameplayLootEmiStack(loot.id);
    }

    private final ClientGameplayLootTable loot;
    private final List<EmiStack> outputStacks;
    private final GameplayLootEmiStack inputStack;
    private final List<WidgetRowBuilder> rowBuilderList = new ArrayList<>();

    private void addWidgetBuilders(ClientBuiltPool newPool, boolean recursive) {
        if (recursive || rowBuilderList.isEmpty()) {
            rowBuilderList.add(new WidgetRowBuilder(154));
        }
        boolean added = false;
        for (WidgetRowBuilder builder : rowBuilderList) {
            if (builder.canAddPool(newPool)) {
                builder.addAndTrim(newPool);
                added = true;
                break;
            }
        }
        if (!added) {
            @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
            Optional<ClientBuiltPool> opt = rowBuilderList.get(rowBuilderList.size() - 1).addAndTrim(newPool);
            opt.ifPresent(clientMobBuiltPool -> addWidgetBuilders(clientMobBuiltPool, true));
        }


    }

    @Override
    public EmiRecipeCategory getCategory() {
        return LootEmiRegistration.GAMEPLAY_CATEGORY;
    }

    @Override
    public @Nullable Identifier getId() {
        return Identifier.of(EMILoot.MOD_ID, "/" + getCategory().id.getPath() + "/" + loot.id.getNamespace() + "/" + loot.id.getPath());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return new ArrayList<>();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
                    return EmiRecipe.super.getCatalysts();
                }

    @Override
    public List<EmiStack> getOutputs() {
        return outputStacks;
    }

    @Override
    public int getDisplayWidth() {
        return LootSettings.isTooltipStyle() ? 144 : 154;
    }

    @Override
    public int getDisplayHeight() {
        if (LootSettings.isTooltipStyle()) {
            int stacks = outputStacks.size();
            if (stacks <= 8) {
                return 18 + 11;
            } else {
                if (EMILoot.Type.GAMEPLAY.isCompact()) {
                    int ingredients = 0;
                    for (WidgetRowBuilder builder: rowBuilderList) {
                        ingredients += builder.ingredientCount();
                    }
                    if (ingredients <= 8) {
                        return 29;
                    } else {
                        return 11 + 18 * ((ingredients + 7) / 8);
                    }
                } else {
                    return 11 + 18 * ((stacks + 7) / 8);
                }
            }
        } else {
            return rowBuilderList.size() * 29 + 11;
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        int y = 0;

        //draw the gameplay name
        widgets.add(new InteractableTextWidget(inputStack, 1, 0, 0x404040, false).recipeContext(this));
        if (EMILoot.isModLoaded(loot.id.getNamespace())) {
            widgets.addTooltip(ClientText.components(inputStack.getName(), loot.id.getNamespace()), 0, 0, 144, 10);
        } else {
            widgets.addTooltipText(List.of(inputStack.getName()), 0, 0, 144, 10);
        }
        if (LootSettings.isTooltipStyle()) {
            List<ConditionalStack> stacks = (outputStacks.size() <= 4 || !EMILoot.Type.GAMEPLAY.isCompact())
                    ?
                    rowBuilderList.stream().map(WidgetRowBuilder::stacks).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
                    :
                    rowBuilderList.stream().map(WidgetRowBuilder::ingredients).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
            int i = 0;
            int j = 0;
            for (ConditionalStack stack: stacks) {
                SlotWidget widget = widgets.addSlot(stack.getIngredient(), i * 18, 11 + (18 * j));
                String rounded = FloatTrimmer.trimFloatString(stack.weight() / 100f, LootSettings.chanceDecimalPlaces());
                widget.appendTooltip(LText.translatable("emi_loot.rolls", rounded).formatted(Formatting.GRAY));
                if (LootSettings.showConditionsInTooltip()) {
                    for (Pair<Integer, Text> pair : stack.conditions()) {
                        for (Text line : ConditionIcons.tooltipLines(pair)) {
                            widget.appendTooltip(line);
                        }
                    }
                }
                ++i;
                if (i > 7) {
                    i = 0;
                    ++j;
                }
            }
        } else {
            y += 11;
            for (WidgetRowBuilder builder : rowBuilderList) {
                for (ClientBuiltPool pool : builder.getPoolList()) {
                    IconGroupEmiWidget widget = new IconGroupEmiWidget(x, y, pool, this);
                    widgets.add(widget);
                    x += widget.getWidth() + 6;
                }
                y += 29;
                x = 0;
            }
        }
    }

    //may revisit later
    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return EmiRecipe.super.hideCraftable();
    }

    public record GameplayLootRecipeData(ClientGameplayLootTable loot) {

        public static GameplayLootRecipeData of(ClientGameplayLootTable loot) {
            loot.build(MinecraftClient.getInstance().world, Blocks.AIR);
            return new GameplayLootRecipeData(loot);
        }

    }
}