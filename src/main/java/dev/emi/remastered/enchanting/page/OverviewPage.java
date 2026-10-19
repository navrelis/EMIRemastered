package dev.emi.remastered.enchanting.page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.enchanting.logic.EnchantmentSources;
import dev.emi.remastered.enchanting.recipe.EnchantmentInfo;
import dev.emi.remastered.enchanting.widget.PagedWidgets;
import dev.emi.remastered.enchanting.widget.TextLineWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * First page: the book, the enchantment's name, a few yes/no facts and the items it can be applied to. More facts
 * can be added as lines with {@link #addLine}; the page has room for five lines, anything bigger belongs on a page
 * of its own.
 */
public class OverviewPage implements InfoPage {
	private static final int LINE_START = 20;
	private static final int LINE_HEIGHT = 11;
	private static final List<Function<EnchantmentInfo, Text>> LINES = new CopyOnWriteArrayList<>(List.of(
		info -> Text.translatable("emi_enchanting.curse", yesNo(info.sources().contains(EnchantmentSources.Source.CURSE), true)),
		info -> Text.translatable("emi_enchanting.treasure", yesNo(info.sources().contains(EnchantmentSources.Source.TREASURE), true)),
		info -> Text.translatable("emi_enchanting.tradeable", yesNo(isTradeable(info), false)),
		info -> Text.translatable("emi_enchanting.random", yesNo(info.sources().contains(EnchantmentSources.Source.ENCHANTING_TABLE)
			|| info.sources().contains(EnchantmentSources.Source.LOOT), false)),
		info -> Text.translatable("emi_enchanting.maxLvl", Text.literal(String.valueOf(info.maxLevel())).formatted(Formatting.BLACK))));

	/** Adds a line below the existing ones. The function is asked every time the page is built. */
	public static void addLine(Function<EnchantmentInfo, Text> line) {
		LINES.add(line);
	}

	@Override
	public void addPages(EnchantmentInfo info, PagedWidgets pages) {
		WidgetHolder page = pages.newPage();
		page.addSlot(info.bookIngredient(), 0, 0);
		page.add(new TextLineWidget(() -> EnchantmentTexts.name(info.entry()), 22, 2, EnchantmentTexts.BLACK));
		int y = LINE_START;
		for (Function<EnchantmentInfo, Text> line : LINES) {
			try {
				page.addText(line.apply(info), 0, y, EnchantmentTexts.BLACK, false);
				y += LINE_HEIGHT;
			} catch (Throwable t) {
				EmiLog.error("EMI Enchanting: could not build a line of the page of " + info.id(), t);
			}
		}
		y += 7;
		page.addText(Text.translatable("emi_enchanting.valid_items"), 0, y, EnchantmentTexts.BLACK, false);
		y += 10;
		for (int i = 0; i < info.validItems().size(); i++) {
			List<? extends EmiIngredient> column = info.validItems().get(i);
			if (!column.isEmpty()) {
				page.addSlot(EmiIngredient.of(column), i * 18, y);
			}
		}
	}

	/** "Yes" or "No", red when the answer is the bad one: yes for a curse, no for being tradeable. */
	private static Text yesNo(boolean yes, boolean badLine) {
		return Text.translatable("emi_enchanting." + (yes ? "yes" : "no") + (badLine ? "_bad" : "_good"));
	}

	private static boolean isTradeable(EnchantmentInfo info) {
		return info.sources().contains(EnchantmentSources.Source.LIBRARIAN_TRADES)
			|| info.sources().contains(EnchantmentSources.Source.TRADED_EQUIPMENT);
	}
}
