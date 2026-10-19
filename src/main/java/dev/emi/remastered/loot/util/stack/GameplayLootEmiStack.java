package dev.emi.remastered.loot.util.stack;

import dev.emi.remastered.loot.LootSettings;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.loot.EMILoot;
import net.minecraft.util.Identifier;

public class GameplayLootEmiStack extends EmiStackWithTitle {
    public GameplayLootEmiStack(Identifier id) {
        super(id, "gameplay", "unknown_gameplay", EMILoot.Type.GAMEPLAY, LootSettings.isTooltipStyle() ? 138 : 148, LootSettings.linkFormatting());
    }

    @Override
    public EmiStack copy() {
        return new GameplayLootEmiStack(this.id);
    }
}