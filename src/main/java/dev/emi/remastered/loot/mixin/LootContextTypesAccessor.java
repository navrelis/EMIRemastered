package dev.emi.remastered.loot.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootContextTypes.class)
public interface LootContextTypesAccessor {
	@Accessor("MAP")
	static BiMap<Identifier, LootContextType> emi_loot$getMAP() {
		throw new UnsupportedOperationException();
	}
}