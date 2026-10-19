package dev.emi.remastered.loot.util.stack;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.EMILoot;
import net.minecraft.util.Identifier;

public class ChestLootEmiStack extends EmiStackWithTitle {
    public ChestLootEmiStack(Identifier id) {
        super(id, "chest", "unknown_chest", EMILoot.Type.CHEST, 138, LootSettings.linkFormatting());
    }

    @Override
    public EmiStack copy() {
        return new ChestLootEmiStack(this.id);
    }
}