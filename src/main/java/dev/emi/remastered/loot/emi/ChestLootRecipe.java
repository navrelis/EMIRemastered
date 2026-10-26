package dev.emi.remastered.loot.emi;

import dev.emi.remastered.loot.LootSettings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.info.search.SourceLabels;
import dev.emi.remastered.info.search.SourceText;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.client.ClientChestLootTable;
import dev.emi.remastered.loot.structure.StructureRef;
import dev.emi.remastered.loot.util.StructureText;
import dev.emi.remastered.loot.util.stack.ChestLootEmiStack;
import dev.emi.remastered.loot.util.InteractableTextWidget;
import dev.emi.remastered.loot.util.ClientText;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.RollsText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.emi.remastered.loot.util.FloatTrimmer.trimFloatString;

public class ChestLootRecipe implements EmiRecipe, SourceLabels {

    public ChestLootRecipe(ChestLootRecipeData data) {
        this.loot = data.loot;
        this.isGuaranteedNonChance = data.guaranteed;

        this.lootStacksSorted = Suppliers.memoize(() -> {
            ArrayListMultimap<Float, EmiStack> map2 = ArrayListMultimap.create();
            for (Iterator<Map.Entry<Float, ItemStack>> it = data.itr; it.hasNext(); ) {
                Map.Entry<Float, ItemStack> entry = it.next();
                EmiStack stack = EmiStack.of(entry.getValue());
                map2.put(entry.getKey(), stack);
            }
            return map2;
        });
        this.lootStacksSortedSize = data.lootStacksSortedSize;
        List<EmiStack> list = new ArrayList<>();
        for (ItemStack stack: data.totalItemList) {
            list.add(EmiStack.of(stack));
        }
        this.outputs = list;
        inputStack = new ChestLootEmiStack(loot.id);
    }

    /** Built from the structures it names; rebuilt when the server sends them later. */
    private volatile Info info;

    private final ClientChestLootTable loot;
    private final Supplier<ArrayListMultimap<Float, EmiStack>> lootStacksSorted;
    private final int lootStacksSortedSize;
    private final List<EmiStack> outputs;
    private boolean isGuaranteedNonChance = false;
    private final ChestLootEmiStack inputStack;
    private final float columns = 8f;



    public List<StructureRef> getStructures() {
        return loot.getStructures();
    }

    private record SearchLabels(List<StructureRef> source, List<String> labels) {
    }

    private volatile SearchLabels searchLabels;

    /** Source search ({@code ~}): the table id and, once the server sent them, the structures and dimensions of the chest. */
    @Override
    public List<String> sourceLabels() {
        List<StructureRef> structures = loot.getStructures();
        SearchLabels current = searchLabels;
        if (current == null || current.source() != structures) {
            List<String> labels = new ArrayList<>();
            labels.add(loot.id.getPath());
            labels.add(SourceText.spaced(loot.id.getPath()));
            for (StructureRef ref : structures) {
                labels.add(StructureText.structureName(ref.structure()));
                labels.add(SourceText.spaced(ref.structure().getPath()));
                for (Identifier dimension : ref.dimensions()) {
                    labels.add(StructureText.dimensionName(dimension));
                }
            }
            current = new SearchLabels(structures, List.copyOf(labels));
            searchLabels = current;
        }
        return current.labels();
    }

    public Text getTitle() {
        return inputStack.getName();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return (LootSettings.splitChestLootByTrial && loot.id.getPath().contains("trial_chamber")) ? LootEmiRegistration.TRIAL_CATEGORY : LootEmiRegistration.CHEST_CATEGORY;
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
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 144;
    }

    /** The extra lines below the title: where the chest is found, and the Lootr note. */
    private record Info(List<StructureRef> source, ClientText.Wrapped structure, Text structureText, ClientText.Wrapped lootr) {
        int lineCount() {
            return structure.lines().size() + lootr.lines().size();
        }
    }

    private static final int INFO_WIDTH = 144;
    private static final int INFO_LINE_HEIGHT = 9;
    private static final int INFO_COLOR = 0x555555;

    private Info info() {
        List<StructureRef> structures = loot.getStructures();
        Info current = info;
        if (current == null || current.source() != structures) {
            current = buildInfo(structures);
            info = current;
        }
        return current;
    }

    private Info buildInfo(List<StructureRef> structures) {
        ClientText.Wrapped none = new ClientText.Wrapped(List.of(), false);
        Text found = LootSettings.showStructures ? StructureText.foundIn(structures) : null;
        ClientText.Wrapped structure = found == null ? none : ClientText.wrap(found, INFO_WIDTH, 2);
        ClientText.Wrapped lootr = LootSettings.lootrNote && EMILoot.isModLoaded("lootr") ? ClientText.wrap(LText.translatable("emi_loot.lootr_note"), INFO_WIDTH, 2) : none;
        return new Info(structures, structure, found, lootr);
    }

    @Override
    public int getDisplayHeight() {
        int titleHeight = 11 + info().lineCount() * INFO_LINE_HEIGHT;
        int boxesHeight = ((int) Math.ceil(lootStacksSortedSize/ columns) * (EMILoot.Type.CHEST.isCompact() ? 18 : 19)) - 1;
        return titleHeight + boxesHeight;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        final int titleSpace;
        final int finalRowHeight;
        Info extra = info();
        int infoHeight = extra.lineCount() * INFO_LINE_HEIGHT;
        if (widgets.getHeight() < getDisplayHeight()) {
            titleSpace = 9 + infoHeight;
            finalRowHeight = (widgets.getHeight() - titleSpace) / ((int) Math.ceil(lootStacksSortedSize / 8.0));
        } else {
            titleSpace = 11 + infoHeight;
            finalRowHeight =  18;
        }
        widgets.add(new InteractableTextWidget(inputStack, 1, 0, 0x404040, false).recipeContext(this));
        if (EMILoot.isModLoaded(loot.id.getNamespace())) {
            widgets.addTooltip(ClientText.components(inputStack.getName(), loot.id.getNamespace()), 0, 0, 144, 10);
        } else {
            widgets.addTooltipText(List.of(inputStack.getName()), 0, 0, 144, 10);
        }
        int infoY = 11;
        for (OrderedText line : extra.structure().lines()) {
            widgets.addText(line, 1, infoY, INFO_COLOR, false);
            infoY += INFO_LINE_HEIGHT;
        }
        if (extra.structure().truncated()) {
            widgets.addTooltipText(List.of(extra.structureText()), 0, 11, INFO_WIDTH, extra.structure().lines().size() * INFO_LINE_HEIGHT);
        }
        for (OrderedText line : extra.lootr().lines()) {
            widgets.addText(line, 1, infoY, INFO_COLOR, false);
            infoY += INFO_LINE_HEIGHT;
        }
        AtomicInteger index = new AtomicInteger(lootStacksSortedSize);
        for (var entry : lootStacksSorted.get().asMap().entrySet()) {
            float weight = entry.getKey();
            Collection<EmiStack> items = entry.getValue();
            if ((loot.items.size() <= 48) && !LootSettings.chestLootAlwaysStackSame) {
                for (EmiStack stack : items) {
                    int row = (int) Math.ceil(index.get() / columns) - 1;
                    int column = (index.get() - 1) % (int) columns;
                    index.getAndDecrement();
                    String fTrim = trimFloatString(weight, LootSettings.chanceDecimalPlaces());
                    SlotWidget slotWidget = new SlotWidget(stack, column * 18, titleSpace + row * finalRowHeight).recipeContext(this);
                    widgets.add(slotWidget.appendTooltip(LText.translatable("emi_loot.percentage", fTrim).formatted(Formatting.GRAY)));
                }
            } else {
                int row = (int) Math.ceil(index.get() / columns) - 1;
                int column = (int)((index.get() - 1) % columns);
                index.getAndDecrement();
                EmiIngredient ingredient = EmiIngredient.of(items.stream().toList());
                SlotWidget slotWidget = new SlotWidget(ingredient, column * 18, titleSpace + row * finalRowHeight).recipeContext(this);
                widgets.add(slotWidget.appendTooltip(RollsText.of(weight, Math.max(LootSettings.chanceDecimalPlaces() + 1, 2)).formatted(Formatting.GRAY)));
            }
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return EmiRecipe.super.supportsRecipeTree() && isGuaranteedNonChance;
    }

    @Override
    public boolean hideCraftable() {
        return EmiRecipe.super.hideCraftable();
    }

    public record ChestLootRecipeData(ClientChestLootTable loot, Iterator<Map.Entry<Float, ItemStack>> itr, List<ItemStack> totalItemList, boolean guaranteed, int lootStacksSortedSize) {

        public static ChestLootRecipeData of(ClientChestLootTable loot) {
            boolean isGuaranteedNonChance = false;
            if (loot.items.size() == 1) {
                if (loot.items.values().toFloatArray()[0] == 1f) {
                    isGuaranteedNonChance = true;
                }
            }

            List<ItemStack> totalItemList = new ArrayList<>();
            ArrayListMultimap<Float, ItemStack> map2 = ArrayListMultimap.create();
            loot.items.forEach((item, weight) -> {
                map2.put(weight, item);
                totalItemList.add(item);
            });
            for (float key : map2.keySet()) {
                map2.get(key).sort(Comparator.comparingInt(s -> Registries.ITEM.getRawId(s.getItem())));
            }

            Iterator<Map.Entry<Float, ItemStack>> itr = map2.entries().iterator();

            int lootStacksSortedSize;

            if (loot.items.size() > 48 || LootSettings.chestLootAlwaysStackSame) {
                lootStacksSortedSize = map2.keySet().size();
            } else {
                lootStacksSortedSize = loot.items.size();
            }

            return new ChestLootRecipeData(loot, itr, totalItemList, isGuaranteedNonChance, lootStacksSortedSize);
        }

    }
}