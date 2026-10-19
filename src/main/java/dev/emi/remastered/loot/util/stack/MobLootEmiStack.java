package dev.emi.remastered.loot.util.stack;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.util.LText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

public class MobLootEmiStack extends EmiStackWithTitle {
    public MobLootEmiStack(Identifier id) {
        super(id, "mob", "unknown_mob", EMILoot.Type.CHEST, 138, LootSettings.linkFormatting());
    }

    @Override
    public EmiStack copy() {
        return new MobLootEmiStack(this.id);
    }

    @Override
    protected MutableText getRawTitle(Identifier id, String unknownNamespace, String unknownPath, EMILoot.Type type) {
        return LText.empty();
    }
}