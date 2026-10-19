package dev.emi.remastered.enchanting.page;

import java.util.List;

import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.widget.PagedWidgets;
import dev.emi.remastered.enchanting.widget.TextLineWidget;
import net.minecraft.text.Text;

/** The enchantments that cannot be combined with this one, five per page. */
public class ExclusionsPage implements InfoPage {
	private static final int PER_PAGE = 5;

	@Override
	public void addPages(EnchantmentInfo info, PagedWidgets pages) {
		if (!EnchantingSettings.showExclusions) {
			return;
		}
		List<EnchantmentInfo.Exclusion> exclusions = info.exclusions();
		for (int start = 0; start < exclusions.size(); start += PER_PAGE) {
			WidgetHolder page = pages.newPage();
			page.addText(Text.translatable("emi_enchanting.exclusions"), 0, 0, EnchantmentTexts.BLACK, false);
			for (int i = 0; i < PER_PAGE && start + i < exclusions.size(); i++) {
				EnchantmentInfo.Exclusion exclusion = exclusions.get(start + i);
				int y = 14 + 20 * i;
				page.addSlot(exclusion.books(), 0, y);
				page.add(new TextLineWidget(() -> EnchantmentTexts.name(exclusion.entry()), 20, y + 5, EnchantmentTexts.BLACK));
			}
		}
	}
}
