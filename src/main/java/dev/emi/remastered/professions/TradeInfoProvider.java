package dev.emi.remastered.professions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoEntry;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.module.Modules;
import dev.emi.remastered.professions.trade.TradeEntry;
import dev.emi.remastered.professions.trade.TradeIds;
import dev.emi.remastered.professions.trade.TradeIndex;
import dev.emi.remastered.professions.trade.TradeRecipe;
import dev.emi.remastered.professions.trade.TradeRows;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Item Info provider of the professions module: for a stack the villager and wandering trader trades that sell it
 * (How to Get) and that buy it (Used For), with profession, level and price. The trades are the recipes of the two trade
 * categories, indexed once per EMI reload when the first page is opened.
 * <p>
 * It covers the two trade categories (see {@link #coveredCategories}), so the built-in recipe provider leaves its
 * "Villager Trades" rows out for stacks that are listed here. Client only.
 */
public final class TradeInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = Identifier.of(ProfessionsCategories.NAMESPACE, "trades");
	private static final Identifier SOLD_ID = Identifier.of(ProfessionsCategories.NAMESPACE, "sold_by");
	private static final Identifier BOUGHT_ID = Identifier.of(ProfessionsCategories.NAMESPACE, "bought_by");
	private static final int EXAMPLES = 12;
	private static volatile Index index;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public String getModuleId() {
		return "professions";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return Modules.PROFESSIONS.isEnabled() && ProfessionsSettings.itemInfo && ProfessionsSettings.showTrades && !stack.isEmpty();
	}

	@Override
	public void onReload() {
		index = null;
	}

	/** Drops the index (disconnect). */
	public static void clear() {
		index = null;
	}

	/** The trades that sell and buy a stack. */
	private record Matches(List<TradeRecipe> sold, List<TradeRecipe> bought) {
	}

	private static Matches match(EmiStack stack) {
		Index index = index();
		List<TradeRecipe> sold = new ArrayList<>();
		for (TradeRecipe recipe : index.byOutput.get(stack.getKey())) {
			if (recipe.getEntry().output().isEqual(stack)) {
				sold.add(recipe);
			}
		}
		List<TradeRecipe> bought = new ArrayList<>();
		for (TradeRecipe recipe : index.byInput.get(stack.getKey())) {
			TradeEntry entry = recipe.getEntry();
			if (entry.first().isEqual(stack) || entry.second().isEqual(stack)) {
				bought.add(recipe);
			}
		}
		return new Matches(sold, bought);
	}

	/** The trade categories this page lists itself: the built-in recipe rows for them would repeat the trades. */
	@Override
	public Set<Identifier> coveredCategories(EmiStack stack) {
		Matches matches = match(stack);
		Set<Identifier> covered = new HashSet<>();
		for (TradeRecipe recipe : matches.sold()) {
			covered.add(recipe.getCategory().getId());
		}
		for (TradeRecipe recipe : matches.bought()) {
			covered.add(recipe.getCategory().getId());
		}
		return covered;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		Matches matches = match(stack);
		List<TradeRecipe> sold = matches.sold();
		List<TradeRecipe> bought = matches.bought();
		// emeralds are the trade currency: villagers hand them out when they buy and take them when they sell
		boolean currency = stack.getKey() == Items.EMERALD;
		if (!sold.isEmpty()) {
			InfoSection section = collector.addSection(SOLD_ID, InfoSection.Kind.OBTAIN,
				Text.translatable(currency ? "emiprofessions.info.earned" : "emiprofessions.info.sold", sold.size()))
				.icon(EmiStack.of(Items.EMERALD));
			rows(section, stack, sold, true);
			showAll(section, sold);
		}
		if (!bought.isEmpty()) {
			InfoSection section = collector.addSection(BOUGHT_ID, InfoSection.Kind.USE,
				Text.translatable(currency ? "emiprofessions.info.spent" : "emiprofessions.info.bought", bought.size()))
				.icon(EmiStack.of(Items.EMERALD));
			rows(section, stack, bought, false);
			showAll(section, bought);
		}
	}

	private static Index index() {
		Index current = index;
		if (current == null) {
			current = build(EmiApi.getRecipeManager());
			index = current;
		}
		return current;
	}

	private static Index build(EmiRecipeManager manager) {
		List<TradeRecipe> trades = new ArrayList<>();
		for (EmiRecipeCategory category : List.of(ProfessionsCategories.VILLAGER_TRADES, ProfessionsCategories.WANDERING_TRADER)) {
			for (EmiRecipe recipe : manager.getRecipes(category)) {
				if (recipe instanceof TradeRecipe trade) {
					trades.add(trade);
				}
			}
		}
		// villagers before the wandering trader, then by profession, level and position
		trades.sort(Comparator.<TradeRecipe, Boolean>comparing(t -> t.getEntry().wanderer())
			.thenComparing((a, b) -> TradeIds.compare(a.getEntry().id().getPath(), b.getEntry().id().getPath())));
		Index result = new Index();
		for (TradeRecipe trade : trades) {
			TradeEntry entry = trade.getEntry();
			result.byOutput.add(entry.output().getKey(), trade);
			result.byInput.add(entry.first().getKey(), trade);
			if (!entry.second().isEmpty()) {
				result.byInput.add(entry.second().getKey(), trade);
			}
		}
		return result;
	}

	/** Trades by the key (item) of their output and of their inputs. */
	private static final class Index {
		final TradeIndex<Object, TradeRecipe> byOutput = new TradeIndex<>();
		final TradeIndex<Object, TradeRecipe> byInput = new TradeIndex<>();
	}

	/** One row per trade while there are few, otherwise one row per profession. */
	private static void rows(InfoSection section, EmiStack stack, List<TradeRecipe> trades, boolean sold) {
		if (TradeRows.detailed(trades.size())) {
			for (TradeRecipe trade : trades) {
				TradeEntry entry = trade.getEntry();
				long amount = (sold ? entry.output() : matching(entry, stack)).getAmount();
				InfoEntry row = section.entry(icon(entry), Text.translatable("emiprofessions.trade.title", entry.owner(), entry.levelText()))
					.stacks(sold ? prices(entry, stack) : List.of(entry.output()))
					.link(InfoLink.recipe(entry.id()))
					.tooltip(tooltip(entry, stack, sold));
				if (amount > 1) {
					row.count(amount);
				}
			}
			return;
		}
		List<TradeRows.Group<TradeRecipe>> groups = TradeRows.groupByOwner(trades, t -> t.getEntry().owner().getString());
		int shown = 0;
		for (TradeRows.Group<TradeRecipe> group : groups) {
			if (shown++ == TradeRows.GROUP_LIMIT) {
				break;
			}
			TradeEntry first = group.trades().get(0).getEntry();
			List<EmiRecipe> recipes = new ArrayList<>(group.trades());
			section.entry(icon(first), first.owner())
				.count(group.trades().size())
				.stacks(TradeRows.<TradeRecipe, EmiStack>distinct(group.trades(),
					t -> sold ? prices(t.getEntry(), stack) : List.of(t.getEntry().output()), EXAMPLES))
				.link(InfoLink.recipes(recipes))
				.tooltip(List.of(Text.translatable("emiprofessions.info.levels", levels(group.trades())).formatted(Formatting.GRAY)));
		}
	}

	/** The built-in category row opened all trades of the stack in EMI; this link takes its place. */
	private static void showAll(InfoSection section, List<TradeRecipe> trades) {
		if (trades.size() > 1) {
			section.link(Text.translatable("emiprofessions.info.all", trades.size()), InfoLink.recipes(new ArrayList<>(trades)));
		}
	}

	private static EmiStack matching(TradeEntry entry, EmiStack stack) {
		return entry.first().isEqual(stack) ? entry.first() : entry.second();
	}

	/**
	 * What the player pays. EMI treats items such as suspicious stew as equal whatever their data, so when the trade's
	 * output differs from the page's stack it is shown as well, which keeps otherwise identical rows apart.
	 */
	private static List<EmiStack> prices(TradeEntry entry, EmiStack stack) {
		List<EmiStack> stacks = new ArrayList<>(3);
		stacks.add(entry.first());
		if (!entry.second().isEmpty()) {
			stacks.add(entry.second());
		}
		if (!entry.output().getComponentChanges().equals(stack.getComponentChanges())) {
			stacks.add(entry.output());
		}
		return stacks;
	}

	private static EmiRenderable icon(TradeEntry entry) {
		return entry.workstations().isEmpty() ? EmiStack.of(Items.WANDERING_TRADER_SPAWN_EGG) : EmiIngredient.of(entry.workstations());
	}

	private static List<Text> tooltip(TradeEntry entry, EmiStack stack, boolean sold) {
		List<Text> lines = new ArrayList<>();
		if (!sold) {
			EmiStack other = entry.first().isEqual(stack) ? entry.second() : entry.first();
			if (!other.isEmpty()) {
				lines.add(Text.translatable("emiprofessions.info.together", other.getAmount(), other.getName()).formatted(Formatting.GRAY));
			}
		}
		lines.add(Text.translatable("emiprofessions.trade.max_uses", entry.maxUses()).formatted(Formatting.GRAY));
		lines.add(Text.translatable("emiprofessions.trade.experience", entry.experience()).formatted(Formatting.GRAY));
		if (!entry.villagerTypes().isEmpty()) {
			lines.add(Text.translatable("emiprofessions.trade.types", String.join(", ", entry.villagerTypes())).formatted(Formatting.GRAY));
		}
		if (entry.mapPlaceholder()) {
			lines.add(Text.translatable("emiprofessions.trade.map_placeholder").formatted(Formatting.DARK_GRAY));
		}
		return lines;
	}

	private static String levels(List<TradeRecipe> trades) {
		List<String> names = new ArrayList<>();
		for (TradeRecipe trade : trades) {
			String name = trade.getEntry().levelText().getString();
			if (!names.contains(name)) {
				names.add(name);
			}
		}
		return String.join(", ", names);
	}
}
