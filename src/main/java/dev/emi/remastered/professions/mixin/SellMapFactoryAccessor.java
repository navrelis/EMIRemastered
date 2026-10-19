package dev.emi.remastered.professions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.gen.structure.Structure;

/**
 * Map trades need a server world to locate their structure and return null on the client. The accessors expose what
 * is needed to show a placeholder offer instead.
 */
@Mixin(TradeOffers.SellMapFactory.class)
public interface SellMapFactoryAccessor {
	@Accessor("price")
	int emiProfessions$getPrice();

	@Accessor("structure")
	TagKey<Structure> emiProfessions$getStructure();

	@Accessor("nameKey")
	String emiProfessions$getNameKey();

	@Accessor("decoration")
	RegistryEntry<MapDecorationType> emiProfessions$getDecoration();

	@Accessor("maxUses")
	int emiProfessions$getMaxUses();

	@Accessor("experience")
	int emiProfessions$getExperience();
}
