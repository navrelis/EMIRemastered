package dev.emi.emi.runtime;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.data.IndexStackData;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.remastered.runtime.PreservedEntries;
import net.minecraft.client.MinecraftClient;

public class EmiHidden {
	// Data loaded
	public static Set<EmiIngredient> disabledStacks = Sets.newHashSet();
	public static List<IndexStackData.Filter> disabledFilters = Lists.newArrayList();
	public static Map<String, Boolean> disabledFilterLookup = Maps.newHashMap();
	// Plugin defined
	public static Set<EmiIngredient> pluginDisabledStacks = Sets.newHashSet();
	public static List<Predicate<EmiStack>> pluginDisabledFilters = Lists.newArrayList();
	// User edited
	public static Set<EmiIngredient> hiddenStacks = new LinkedHashSet<>();
	// EMI Remastered: hidden stacks that could not be read stay in emi.json (KB3)
	private static final PreservedEntries unreadHidden = new PreservedEntries();
	// EMI Remastered: one filtered index rebuild per client tick, however many stacks changed (KB14)
	private static final AtomicBoolean bakePending = new AtomicBoolean();

	public static void clear() {
		bakePending.set(false); // EMI Remastered: a queued rebuild may have been dropped by a disconnect
		disabledStacks.clear();
		disabledFilters.clear();
		disabledFilterLookup.clear();
		pluginDisabledStacks.clear();
		pluginDisabledFilters.clear();
	}

	public static void reload() {
		List<IndexStackData> isds = EmiData.stackData.stream().map(i -> i.get()).filter(i -> i.disable() && (!i.filters().isEmpty() || !i.removed().isEmpty())).toList();
		for (IndexStackData data : isds) {
			for (EmiIngredient stack : data.removed()) {
				disabledStacks.add(stack);
				disabledStacks.addAll(stack.getEmiStacks());
			}
			disabledFilters.addAll(data.filters());
		}
	}

	public static JsonArray save() {
		JsonArray arr = new JsonArray();
		for (EmiIngredient stack : hiddenStacks) {
			JsonElement el = EmiIngredientSerializer.getSerialized(stack);
			if (el != null && !el.isJsonNull()) {
				arr.add(el);
			}
		}
		return unreadHidden.mergeInto(arr); // EMI Remastered
	}

	public static void load(JsonArray arr) {
		hiddenStacks.clear();
		unreadHidden.clear(); // EMI Remastered
		JsonElement previous = null;
		int index = 0;
		for (JsonElement el : arr) {
			EmiIngredient stack = EmiIngredientSerializer.getDeserialized(el.deepCopy()).copy();
			if (!stack.isEmpty()) {
				for (EmiStack es : stack.getEmiStacks()) {
					es.comparison(c -> EmiPort.compareStrict());
				}
				hiddenStacks.add(stack);
			} else {
				unreadHidden.keep(el, previous, index); // EMI Remastered (KB3)
			}
			previous = el;
			index++;
		}
		if (!unreadHidden.isEmpty()) {
			EmiLog.warn("Keeping " + unreadHidden.size() + " hidden stacks that could not be read (missing mod or invalid data)");
		}
		EmiStackList.bakeFiltered();
	}

	public static boolean isHidden(EmiIngredient stack) {
		return hiddenStacks.contains(stack);
	}

	public static boolean isDisabled(EmiIngredient stack) {
		outer:
		for (EmiStack s : stack.getEmiStacks()) {
			if (disabledStacks.contains(s) || pluginDisabledStacks.contains(s)) {
				continue;
			}
			for (Predicate<EmiStack> predicate : pluginDisabledFilters) {
				if (predicate.test(s)) {
					continue outer;
				}
			}
			boolean filtered = disabledFilterLookup.computeIfAbsent("" + s.getId(), id -> {
				for (IndexStackData.Filter filter : disabledFilters) {
					if (filter.filter().test(id)) {
						return true;
					}
				}
				return false;
			});
			if (filtered) {
				continue;
			}
			return false;
		}
		return !stack.isEmpty();
	}

	public static void setVisibility(EmiIngredient stack, boolean hide, boolean similar) {
		if (similar && stack.getEmiStacks().size() == 1) {
			EmiStack es = stack.getEmiStacks().get(0);
			for (EmiStack i : EmiStackList.stacks) {
				if (es.getId().equals(i.getId())) {
					if (hide) {
						hiddenStacks.add(i.copy().comparison(c -> EmiPort.compareStrict()));
					} else {
						hiddenStacks.remove(i);
					}
				}
			}
		} else {
			if (hide) {
				stack = stack.copy();
				for (EmiStack es : stack.getEmiStacks()) {
					es.comparison(c -> EmiPort.compareStrict());
				}
				hiddenStacks.add(stack);
			} else {
				hiddenStacks.remove(stack);
			}
		}
		EmiPersistentData.save();
		requestBakeFiltered(); // EMI Remastered: was EmiStackList.bakeFiltered() right here (KB14)
	}

	/** EMI Remastered: coalesces filtered index rebuilds of one tick into one (KB14). */
	private static void requestBakeFiltered() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			EmiStackList.bakeFiltered();
		} else if (bakePending.compareAndSet(false, true)) {
			client.send(() -> {
				bakePending.set(false);
				EmiStackList.bakeFiltered();
			});
		}
	}
}
