package dev.emi.remastered.professions.trade;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a trade plan of the Villager API mod ({@code com.frikinjay.villagerapi.villagerpack.VillagerPackTrades},
 * version 1.26.x) by reflection only. A plan is a record with {@code includeOriginal()} and {@code groups()}; a group
 * has {@code listings()}, each weighted listing has {@code listing()} (a trade factory). Contains no Minecraft classes.
 */
public final class VillagerApiPlans {
	private VillagerApiPlans() {
	}

	/**
	 * @param includeOriginal Whether the vanilla trades of the level stay next to the pack trades
	 * @param listings        The trade factories of all groups, as opaque objects in plan order
	 */
	public record Plan(boolean includeOriginal, List<Object> listings) {
	}

	/**
	 * @param plan A {@code LevelPlan} object, may be null
	 * @return The plan, or null if the object is null or does not have the expected shape
	 */
	public static Plan read(Object plan) {
		if (plan == null) {
			return null;
		}
		try {
			boolean includeOriginal = !Boolean.FALSE.equals(call(plan, "includeOriginal"));
			List<Object> listings = new ArrayList<>();
			if (call(plan, "groups") instanceof Iterable<?> groups) {
				for (Object group : groups) {
					if (!(call(group, "listings") instanceof Iterable<?> weighted)) {
						continue;
					}
					for (Object entry : weighted) {
						Object listing = call(entry, "listing");
						if (listing != null) {
							listings.add(listing);
						}
					}
				}
			}
			return new Plan(includeOriginal, listings);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static Object call(Object target, String name) throws ReflectiveOperationException {
		Method method = target.getClass().getMethod(name);
		method.setAccessible(true);
		return method.invoke(target);
	}
}
