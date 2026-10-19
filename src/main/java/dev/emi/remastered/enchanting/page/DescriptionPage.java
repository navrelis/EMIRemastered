package dev.emi.remastered.enchanting.page;

import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.widget.PagedWidgets;
import dev.emi.remastered.enchanting.widget.WrappedTextWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Util;

/**
 * The enchantment's description, for enchantments that have a translation under {@code enchantment.<namespace>.<name>.desc}
 * (provided by a mod or a resource pack; vanilla has none).
 */
public class DescriptionPage implements InfoPage {
	private static final int MAX_LINES = 8;

	@Override
	public void addPages(EnchantmentInfo info, PagedWidgets pages) {
		if (!EnchantingSettings.showDescription) {
			return;
		}
		String key = Util.createTranslationKey("enchantment", info.id()) + ".desc";
		if (!Language.getInstance().hasTranslation(key)) {
			return;
		}
		var page = pages.newPage();
		page.addText(Text.translatable("emi_enchanting.description"), 2, 2, EnchantmentTexts.BLACK, false);
		page.add(new WrappedTextWidget(() -> {
			MutableText description = Text.translatable(key).formatted(Formatting.ITALIC);
			// A disguised name hides the description as well
			return EnchantmentTexts.isObfuscated(EnchantmentTexts.name(info.entry())) ? description.formatted(Formatting.OBFUSCATED) : description;
		}, 2, 16, 140, MAX_LINES, 11, EnchantmentTexts.BLACK));
	}
}
