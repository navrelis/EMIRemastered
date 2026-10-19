package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.enchanting.EnchantingContent;
import dev.emi.remastered.enchanting.EnchantingSettings;
import dev.emi.remastered.enchanting.page.EnchantmentTexts;
import dev.emi.remastered.enchanting.page.InfoPage;
import dev.emi.remastered.enchanting.page.InfoPages;
import dev.emi.remastered.enchanting.widget.PagedWidgets;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The info recipe of one enchantment: its enchanted books are the outputs, the items it applies to and the books of
 * the enchantments it excludes are inputs, so the page opens from either side. The content comes from the pages in
 * {@link InfoPages}.
 */
public class EnchantmentRecipe implements EmiRecipe {
	public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
		Identifier.of(EnchantingContent.NAMESPACE, "enchantments"), EmiStack.of(Items.ENCHANTED_BOOK));
	public static final int WIDTH = 144;
	public static final int HEIGHT = 124;

	private final EnchantmentInfo info;
	private final List<EmiIngredient> inputs = new ArrayList<>();
	private PagedWidgets pages;

	public EnchantmentRecipe(EnchantmentInfo info) {
		this.info = info;
		if (EnchantingSettings.showForItems) {
			for (List<EmiStack> column : info.validItems()) {
				if (!column.isEmpty()) {
					inputs.add(EmiIngredient.of(column));
				}
			}
		}
		if (EnchantingSettings.showExclusions) {
			for (EnchantmentInfo.Exclusion exclusion : info.exclusions()) {
				inputs.add(exclusion.books());
			}
		}
	}

	/** @return the recipe id of the info page of the enchantment with the given registry id */
	public static Identifier idOf(Identifier enchantment) {
		return Identifier.of(EnchantingContent.NAMESPACE, "/" + enchantment.toTranslationKey() + "/enchanting_info");
	}

	public EnchantmentInfo getInfo() {
		return info;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return CATEGORY;
	}

	@Override
	public @Nullable Identifier getId() {
		return idOf(info.id());
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return info.books();
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return HEIGHT;
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		PagedWidgets pages = new PagedWidgets(widgets, this.pages == null ? 0 : this.pages.getCurrent());
		for (InfoPage page : InfoPages.all()) {
			int mark = pages.mark();
			try {
				page.addPages(info, pages);
			} catch (Throwable t) {
				pages.rollback(mark);
				EmiLog.error("EMI Enchanting: a page of " + info.id() + " failed (" + page.getClass().getSimpleName() + ")", t);
			}
		}
		if (pages.getPageCount() == 0) {
			widgets.addText(Text.translatable("emi_enchanting.page_error"), 0, 0, EnchantmentTexts.BLACK, false);
		}
		pages.finish();
		this.pages = pages;
	}
}
