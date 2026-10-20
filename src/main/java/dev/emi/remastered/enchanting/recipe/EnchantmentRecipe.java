package dev.emi.remastered.enchanting.recipe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
	private final List<EmiStack> books;
	private final List<EmiIngredient> inputs = new ArrayList<>();
	private PagedWidgets pages;

	public EnchantmentRecipe(EnchantmentInfo info) {
		this(info, new IdentityHashMap<>());
	}

	/**
	 * @param columnIngredients ingredients of the item columns built so far, shared by the recipes of one reload: the
	 * enchantments that accept the same items have the same column lists (see {@link EnchantmentCatalog}), so each
	 * column becomes an EMI ingredient once
	 */
	public EnchantmentRecipe(EnchantmentInfo info, Map<List<EmiStack>, EmiIngredient> columnIngredients) {
		this.info = info;
		this.books = allBooks(info);
		// One input per own book: EMI sorts a "uses" lookup by the size of the smallest input holding the stack, so
		// the page of the book itself (size 1) comes before the pages that list the book as an exclusion
		for (EmiStack book : books) {
			inputs.add(book);
		}
		if (EnchantingSettings.showForItems) {
			for (List<EmiStack> column : info.validItems()) {
				if (!column.isEmpty()) {
					inputs.add(columnIngredients.computeIfAbsent(column, EmiIngredient::of));
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
		return books;
	}

	/** Own books also count as catalysts: the lookup of a book's uses lists its own page next to the pages that exclude it. */
	@Override
	public List<EmiIngredient> getCatalysts() {
		return List.of(EmiIngredient.of(books));
	}

	/** The books of all listed levels, including the ones above the maximum level. */
	private static List<EmiStack> allBooks(EnchantmentInfo info) {
		if (info.aboveMaxBooks().isEmpty()) {
			return info.books();
		}
		List<EmiStack> books = new ArrayList<>(info.books());
		books.addAll(info.aboveMaxBooks());
		return books;
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
