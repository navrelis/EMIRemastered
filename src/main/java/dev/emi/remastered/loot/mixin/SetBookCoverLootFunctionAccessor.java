package dev.emi.remastered.loot.mixin;

import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.loot.function.SetBookCoverLootFunction;
import net.minecraft.text.RawFilteredPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(SetBookCoverLootFunction.class)
public interface SetBookCoverLootFunctionAccessor {
	@Accessor("author")
	Optional<String> emi_loot$getAuthor();

	@Accessor("title")
	Optional<RawFilteredPair<String>> emi_loot$getTitle();

	@Accessor("generation")
	Optional<Integer> emi_loot$getGeneration();

	@Invoker("apply")
	WrittenBookContentComponent emi_loot$callApply(WrittenBookContentComponent current);
}