package dev.emi.remastered.professions.trade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.remastered.professions.ProfessionEntry;
import dev.emi.remastered.professions.ProfessionsCategories;
import dev.emi.remastered.professions.mixin.SellMapFactoryAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerType;

/**
 * Turns the trade factories of {@link TradeOffers} (which include everything mods added through Fabric API's
 * {@code TradeOfferHelper}) into concrete trades. Every factory is rolled several times with a fixed seed on a dummy
 * entity of the client world, equal results are merged. Client only.
 */
public final class TradeCollector {
	private static final long SEED = 0x7E1A5EEDL;
	private final int rolls;
	private final Stats stats = new Stats();
	private final Set<Class<?>> failedClasses = new HashSet<>();

	private TradeCollector(int rolls) {
		this.rolls = rolls;
	}

	/**
	 * Counters of one collection run.
	 */
	public static final class Stats {
		public int factories;
		public int rolls;
		public int failures;
		public int placeholderMaps;
		public final Map<String, Integer> tradesPerOwner = new LinkedHashMap<>();
	}

	public record Result(List<TradeEntry> villager, List<TradeEntry> wanderer, Stats stats, boolean rebalanced) {
	}

	/**
	 * @param professions All professions with their workstations (may be empty)
	 * @param rolls       Rolls per factory and villager type
	 * @return The trades, empty if the world cannot provide dummy entities
	 */
	public static Result collect(ClientWorld world, List<ProfessionEntry> professions, int rolls) {
		TradeCollector collector = new TradeCollector(Math.max(1, Math.min(64, rolls)));
		boolean rebalanced = world.getEnabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
		List<TradeEntry> villagerTrades = new ArrayList<>();
		List<TradeEntry> wandererTrades = new ArrayList<>();
		VillagerEntity villager = EntityType.VILLAGER.create(world);
		if (villager != null) {
			collector.collectVillagers(villager, professions, rebalanced, villagerTrades);
		}
		WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(world);
		if (trader != null) {
			collector.collectWanderer(trader, rebalanced, wandererTrades);
		}
		return new Result(villagerTrades, wandererTrades, collector.stats, rebalanced);
	}

	private void collectVillagers(VillagerEntity villager, List<ProfessionEntry> professions, boolean rebalanced, List<TradeEntry> out) {
		List<VillagerType> types = Registries.VILLAGER_TYPE.stream().toList();
		for (ProfessionEntry profession : professions) {
			Int2ObjectMap<TradeOffers.Factory[]> levels = null;
			if (rebalanced) {
				levels = TradeOffers.REBALANCED_PROFESSION_TO_LEVELED_TRADE.get(profession.profession());
			}
			if (levels == null) {
				levels = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(profession.profession());
			}
			if (levels == null || levels.isEmpty()) {
				continue;
			}
			String owner = TradeIds.ownerKey(profession.id().getNamespace(), profession.id().getPath());
			int[] keys = levels.keySet().toIntArray();
			Arrays.sort(keys);
			int before = out.size();
			for (int level : keys) {
				TradeBucket<Draft> bucket = new TradeBucket<>();
				TradeOffers.Factory[] factories = levels.get(level);
				VillagerApiCompat.Plan plan = VillagerApiCompat.plan(profession.id(), level);
				Consumer<VillagerType> prepare = type -> villager.setVillagerData(new VillagerData(type, profession.profession(), level));
				if (plan == null || plan.includeOriginal()) {
					for (TradeOffers.Factory factory : factories) {
						roll(factory, villager, types, prepare, false, bucket);
					}
				}
				if (plan != null) {
					for (TradeOffers.Factory factory : plan.factories()) {
						roll(factory, villager, types, prepare, true, bucket);
					}
				}
				Text levelText = level >= 1 && level <= 5 ? Text.translatable("merchant.level." + level)
					: Text.translatable("emiprofessions.trade.level", level);
				emit(bucket, owner, level, false, profession.displayName(), levelText, profession.workstations(), types.size(), out);
			}
			stats.tradesPerOwner.put(owner, out.size() - before);
		}
	}

	private void collectWanderer(WanderingTraderEntity trader, boolean rebalanced, List<TradeEntry> out) {
		List<VillagerType> single = new ArrayList<>();
		single.add(null);
		Consumer<VillagerType> prepare = type -> { };
		Text name = EntityType.WANDERING_TRADER.getName();
		int before = out.size();
		if (rebalanced) {
			int pool = 0;
			for (Pair<TradeOffers.Factory[], Integer> pair : TradeOffers.REBALANCED_WANDERING_TRADER_TRADES) {
				pool++;
				TradeBucket<Draft> bucket = new TradeBucket<>();
				for (TradeOffers.Factory factory : pair.getLeft()) {
					roll(factory, trader, single, prepare, false, bucket);
				}
				emit(bucket, TradeIds.WANDERING_TRADER, pool, true, name,
					Text.translatable("emiprofessions.trade.pool", pool, pair.getRight()), List.of(), 1, out);
			}
		} else {
			int[] keys = TradeOffers.WANDERING_TRADER_TRADES.keySet().toIntArray();
			Arrays.sort(keys);
			for (int pool : keys) {
				TradeBucket<Draft> bucket = new TradeBucket<>();
				for (TradeOffers.Factory factory : TradeOffers.WANDERING_TRADER_TRADES.get(pool)) {
					roll(factory, trader, single, prepare, false, bucket);
				}
				// Vanilla picks 5 trades of pool 1 and 1 trade of pool 2
				Text poolText = pool == 1 || pool == 2
					? Text.translatable("emiprofessions.trade.pool", pool, pool == 1 ? 5 : 1)
					: Text.translatable("emiprofessions.trade.pool_plain", pool);
				emit(bucket, TradeIds.WANDERING_TRADER, pool, true, name, poolText, List.of(), 1, out);
			}
		}
		stats.tradesPerOwner.put(TradeIds.WANDERING_TRADER, out.size() - before);
	}

	private void emit(TradeBucket<Draft> bucket, String owner, int level, boolean wanderer, Text ownerName, Text levelText,
			List<EmiStack> workstations, int typeCount, List<TradeEntry> out) {
		int index = 0;
		for (TradeBucket.Entry<Draft> entry : bucket.entries()) {
			Draft draft = entry.value();
			index++;
			Identifier id = Identifier.of(ProfessionsCategories.NAMESPACE, TradeIds.recipePath(owner, level, index));
			List<String> types = typeCount > 1 && entry.tags().size() < typeCount ? entry.tags() : List.of();
			out.add(new TradeEntry(id, wanderer, ownerName, levelText, workstations, EmiStack.of(draft.first),
				EmiStack.of(draft.second), EmiStack.of(draft.output), draft.maxUses, draft.experience, types,
				draft.mapName, draft.mapStructure, draft.villagerApi));
		}
	}

	private void roll(TradeOffers.Factory factory, Entity entity, List<VillagerType> types, Consumer<VillagerType> prepare,
			boolean villagerApi, TradeBucket<Draft> bucket) {
		if (factory == null) {
			return;
		}
		stats.factories++;
		try {
			for (VillagerType type : types) {
				prepare.accept(type);
				String tag = type == null ? null : typeName(type);
				Random random = Random.create(SEED);
				for (int i = 0; i < rolls; i++) {
					stats.rolls++;
					TradeOffer offer = factory.create(entity, random);
					Draft draft;
					if (offer != null) {
						draft = Draft.of(offer, villagerApi);
					} else if (factory instanceof TradeOffers.SellMapFactory) {
						draft = placeholder((SellMapFactoryAccessor) factory, villagerApi);
						stats.placeholderMaps++;
						i = rolls;
					} else {
						continue;
					}
					if (draft != null) {
						bucket.add(draft.key(), draft, tag);
					}
				}
			}
		} catch (Exception | LinkageError e) {
			stats.failures++;
			if (failedClasses.add(factory.getClass())) {
				EmiLog.LOG.debug("[EMI] [Professions] Trade factory {} failed: {}", factory.getClass().getName(), e.toString());
			}
		}
	}

	private static Draft placeholder(SellMapFactoryAccessor map, boolean villagerApi) {
		Text name = Text.translatable(map.emiProfessions$getNameKey());
		ItemStack output = new ItemStack(Items.FILLED_MAP);
		output.set(DataComponentTypes.ITEM_NAME, name);
		return new Draft(new ItemStack(Items.EMERALD, map.emiProfessions$getPrice()), new ItemStack(Items.COMPASS), output,
			map.emiProfessions$getMaxUses(), map.emiProfessions$getExperience(), name,
			"#" + map.emiProfessions$getStructure().id(), villagerApi);
	}

	private static String typeName(VillagerType type) {
		String path = Registries.VILLAGER_TYPE.getId(type).getPath();
		return path.isEmpty() ? path : path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
	}

	private record StackKey(Item item, int count, ComponentChanges changes) {
		static StackKey of(ItemStack stack) {
			return new StackKey(stack.getItem(), stack.getCount(), stack.getComponentChanges());
		}
	}

	private record Draft(ItemStack first, ItemStack second, ItemStack output, int maxUses, int experience, Text mapName,
			String mapStructure, boolean villagerApi) {

		static Draft of(TradeOffer offer, boolean villagerApi) {
			ItemStack first = display(offer.getFirstBuyItem());
			ItemStack second = offer.getSecondBuyItem().map(Draft::display).orElse(ItemStack.EMPTY);
			ItemStack output = offer.getSellItem().copy();
			if (first.isEmpty() || output.isEmpty()) {
				return null;
			}
			return new Draft(first, second, output, offer.getMaxUses(), offer.getMerchantExperience(), null, null, villagerApi);
		}

		private static ItemStack display(TradedItem traded) {
			ItemStack stack = traded.itemStack().copy();
			stack.setCount(traded.count());
			return stack;
		}

		TradeKey key() {
			return new TradeKey(StackKey.of(first), second.isEmpty() ? null : StackKey.of(second), StackKey.of(output));
		}
	}
}
