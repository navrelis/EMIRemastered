package dev.emi.remastered.loot.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class LText {

    public static HashSet<String> tablePrefixes = new HashSet<>(List.of(
            "spawners",
            "spawner",
            "shearing",
            "pots",
            "pot",
            "gameplay",
            "equipment",
            "entities",
            "entity",
            "dispensers",
            "dispenser",
            "chests",
            "chest",
            "blocks",
            "block",
            "archaeology"
    ));

    public static MutableText translatable(String key) {
        return Text.translatable(key);
    }

    public static MutableText translatable(String key, Object ... args) {
        return Text.translatable(key, args);
    }

    public static MutableText literal(String msg) {
        return Text.literal(msg);
    }

    public static MutableText empty() {
        return Text.empty();
    }

    public static MutableText enchant(RegistryEntry<Enchantment> entry) {
        MutableText mutableText = entry.value().description().copy();
        if (entry.isIn(EnchantmentTags.CURSE)) {
            Texts.setStyleIfAbsent(mutableText, Style.EMPTY.withColor(Formatting.RED));
        } else {
            Texts.setStyleIfAbsent(mutableText, Style.EMPTY.withColor(Formatting.GRAY));
        }
        return mutableText;
    }

}