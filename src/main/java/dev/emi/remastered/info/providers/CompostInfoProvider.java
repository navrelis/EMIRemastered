package dev.emi.remastered.info.providers;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import net.minecraft.block.ComposterBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/** Built-in provider: chance that the item raises a composter's level ({@link ComposterBlock#ITEM_TO_LEVEL_INCREASE_CHANCE}). */
public final class CompostInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EmiPort.id("emi_info", "composting");
	private static final int COLOR = 0x5DA130;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return stack.getKey() instanceof Item;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		float chance = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat((Item) stack.getKey());
		if (!(chance > 0)) {
			return;
		}
		String percent = EmiRenderHelper.TEXT_FORMAT.format(chance * 100);
		collector.addSection(ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_info.section.composting"))
			.icon(EmiStack.of(Items.COMPOSTER))
			.bar(EmiPort.translatable("emi_info.composting.chance"), chance, COLOR, EmiPort.translatable("emi_info.percent", percent))
			.text(EmiPort.translatable("emi_info.composting.items", EmiRenderHelper.TEXT_FORMAT.format(1 + 6 / chance)))
			.link(EmiPort.translatable("emi_info.composting.open"), InfoLink.usesOf(stack, VanillaEmiRecipeCategories.COMPOSTING));
	}
}
