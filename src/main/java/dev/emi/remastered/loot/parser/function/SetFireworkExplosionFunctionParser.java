package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetFireworkExplosionLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.ListProcessors;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.SetFireworkExplosionLootFunction;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SetFireworkExplosionFunctionParser implements FunctionParser {

    @Override
    public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
        List<MutableText> texts = new ArrayList<>();
        Optional<FireworkExplosionComponent.Type> shape = ((SetFireworkExplosionLootFunctionAccessor) function).emi_loot$getShape();
        if (shape.isPresent()) {
            Text s = shape.get().getName();
            texts.add(LText.translatable("emi_loot.function.firework_explosion.shape", s));
        }
        Optional<IntList> colors = ((SetFireworkExplosionLootFunctionAccessor) function).emi_loot$getColors();
        if (colors.isPresent()) {
            List<MutableText> cs = colors.get().intStream().mapToObj(i -> LText.literal(Integer.toString(i))).toList();
            texts.add(LText.translatable("emi_loot.function.firework_explosion.colors", ListProcessors.buildAndList(cs)));
        }
        Optional<IntList> fadeColors = ((SetFireworkExplosionLootFunctionAccessor) function).emi_loot$getFadeColors();
        if (fadeColors.isPresent()) {
            List<MutableText> cs = fadeColors.get().intStream().mapToObj(i -> LText.literal(Integer.toString(i))).toList();
            texts.add(LText.translatable("emi_loot.function.firework_explosion.fade_colors", ListProcessors.buildAndList(cs)));
        }
        Optional<Boolean> trail = ((SetFireworkExplosionLootFunctionAccessor) function).emi_loot$getTrail();
        if (trail.isPresent()) {
            if (trail.get()) {
                texts.add(LText.translatable("emi_loot.function.firework_explosion.trail"));
            } else {
                texts.add(LText.translatable("emi_loot.function.firework_explosion.no_trail"));
            }
        }
        Optional<Boolean> twinkle = ((SetFireworkExplosionLootFunctionAccessor) function).emi_loot$getTwinkle();
        if (twinkle.isPresent()) {
            if (twinkle.get()) {
                texts.add(LText.translatable("emi_loot.function.firework_explosion.twinkle"));
            } else {
                texts.add(LText.translatable("emi_loot.function.firework_explosion.no_twinkle"));
            }
        }

        if (!stack.isEmpty())
            stack.apply(DataComponentTypes.FIREWORK_EXPLOSION, SetFireworkExplosionLootFunction.DEFAULT_EXPLOSION, ((SetFireworkExplosionLootFunctionAccessor) function)::emi_loot$callApply);

        return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.firework_explosion", ListProcessors.buildAndList(texts)), ItemStack.EMPTY, conditionTexts);
    }
}