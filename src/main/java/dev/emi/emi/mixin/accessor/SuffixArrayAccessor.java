package dev.emi.emi.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.search.SuffixArray;

/**
 * EMI Remastered: lets {@code dev.emi.remastered.search.SuffixArrays} build EMI's search arrays with vanilla's result
 * in less time (X-PERF2).
 */
@Mixin(SuffixArray.class)
public interface SuffixArrayAccessor {

	@Accessor("characters")
	IntList emi$getCharacters();

	@Accessor("maxTextLength")
	int emi$getMaxTextLength();

	@Accessor("suffixIndexToObjectIndex")
	IntList emi$getSuffixIndexToObjectIndex();

	@Accessor("suffixIndexToObjectIndex")
	void emi$setSuffixIndexToObjectIndex(IntList list);

	@Accessor("offsetInText")
	IntList emi$getOffsetInText();

	@Accessor("offsetInText")
	void emi$setOffsetInText(IntList list);
}
