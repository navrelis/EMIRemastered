package dev.emi.remastered.loot.parser.function;

import dev.emi.remastered.loot.mixin.SetBookCoverLootFunctionAccessor;
import dev.emi.remastered.loot.parser.LootTableParser;
import dev.emi.remastered.loot.parser.processor.ListProcessors;
import dev.emi.remastered.loot.util.LText;
import dev.emi.remastered.loot.util.TextKey;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SetBookCoverFunctionParser implements FunctionParser {

	@Override
	public LootTableParser.LootFunctionResult parseFunction(LootFunction function, ItemStack stack, boolean parentIsAlternative, List<TextKey> conditionTexts) {
		List<MutableText> texts = new ArrayList<>();
		Optional<String> author = ((SetBookCoverLootFunctionAccessor)function).emi_loot$getAuthor();
		author.ifPresent(s -> texts.add(LText.literal(s)));
		Optional<RawFilteredPair<String>> title = ((SetBookCoverLootFunctionAccessor)function).emi_loot$getTitle();
		title.ifPresent(t -> texts.add(LText.literal(t.raw())));
		Optional<Integer> generation = ((SetBookCoverLootFunctionAccessor)function).emi_loot$getGeneration();
		generation.ifPresent(g -> texts.add(LText.literal(g.toString())));
		if (!stack.isEmpty()) {
			stack.apply(DataComponentTypes.WRITTEN_BOOK_CONTENT, WrittenBookContentComponent.DEFAULT, ((SetBookCoverLootFunctionAccessor)function)::emi_loot$callApply);
		}
		return new LootTableParser.LootFunctionResult(TextKey.of("emi_loot.function.set_book_cover", ListProcessors.buildAndList(texts)), ItemStack.EMPTY, conditionTexts);
	}
}