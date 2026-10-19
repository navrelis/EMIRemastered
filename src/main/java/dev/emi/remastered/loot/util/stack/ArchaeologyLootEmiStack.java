package dev.emi.remastered.loot.util.stack;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.EMILoot;
import net.minecraft.util.Identifier;

public class ArchaeologyLootEmiStack extends EmiStackWithTitle {
    public ArchaeologyLootEmiStack(Identifier id) {
        super(id, "archaeology", "unknown_archaeology", EMILoot.Type.ARCHAEOLOGY, LootSettings.isTooltipStyle() ? 138 : 148, LootSettings.linkFormatting());
    }

    @Override
    public EmiStack copy() {
        return new ArchaeologyLootEmiStack(this.id);
    }
}