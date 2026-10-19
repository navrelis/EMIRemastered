package dev.emi.remastered.enchanting.page;

import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import dev.emi.remastered.enchanting.logic.LevelCap;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.widget.PagedWidgets;
import net.minecraft.text.Text;

/** Where the enchantment can be found (from its tags) and, if a mod lifts it, the level cap of item stacks. */
public class SourcesPage implements InfoPage {
	private static final int LINE_HEIGHT = 11;

	@Override
	public void addPages(EnchantmentInfo info, PagedWidgets pages) {
		boolean sources = EnchantingSettings.showSources;
		boolean cap = LevelCap.isCustom(info.levelCap());
		if (!sources && !cap) {
			return;
		}
		WidgetHolder page = pages.newPage();
		int y = 0;
		if (sources) {
			page.addText(Text.translatable("emi_enchanting.sources"), 0, y, EnchantmentTexts.BLACK, false);
			y += 14;
			if (info.sources().isEmpty()) {
				page.addText(EnchantmentTexts.noSource(), 0, y, EnchantmentTexts.BLACK, false);
				y += LINE_HEIGHT;
			}
			for (EnchantmentSources.Source source : info.sources()) {
				page.addText(Text.literal("• ").append(EnchantmentTexts.source(source)), 0, y, EnchantmentTexts.BLACK, false);
				y += LINE_HEIGHT;
			}
			y += 7;
		}
		if (cap) {
			page.addText(Text.translatable("emi_enchanting.level_cap", EnchantmentTexts.levelCap(info.levelCap())), 0, y,
				EnchantmentTexts.BLACK, false);
		}
	}
}
