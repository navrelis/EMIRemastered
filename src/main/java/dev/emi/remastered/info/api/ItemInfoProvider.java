package dev.emi.remastered.info.api;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.util.Identifier;

/**
 * Adds sections to the Item Info screen of a stack. Register with {@link ItemInfoRegistry#register}.
 * <p>
 * Lifecycle: the screen asks every provider lazily, the first time a stack's page is opened, and caches the result
 * until the next EMI reload (or until a provider is (un)registered or a module is switched on or off). Providers
 * must not keep the {@link InfoCollector} after {@link #collect} returns.
 * <p>
 * Threading: {@link #appliesTo}, {@link #collect} and {@link #onReload} are only called on the client render thread,
 * after EMI finished loading. {@link #getId}, {@link #getOrder} and {@link #getModuleId} may be called from any
 * thread and must return constant values.
 * <p>
 * Errors: an exception thrown by a provider is logged (once per reload) and shown as one error line on the page;
 * the other providers still run.
 */
public interface ItemInfoProvider {
	/** Order of the built-in EMI recipe sections (recipes producing and using the stack). */
	int ORDER_RECIPES = 0;
	/** Default order; lower orders come first within a section kind. */
	int ORDER_DEFAULT = 1000;
	/** Order of providers that look at what the others added (e.g. the "no known source" marker). */
	int ORDER_LAST = Integer.MAX_VALUE;

	/** Unique id, e.g. {@code emi_loot:chest_loot}. Registering another provider with the same id replaces this one. */
	Identifier getId();

	/** Sort key among providers (ascending, ties by id). Sections are grouped by {@link InfoSection.Kind} first. */
	default int getOrder() {
		return ORDER_DEFAULT;
	}

	/**
	 * Id of the EMI Remastered module this provider belongs to (e.g. {@code "loot"}), or null for none. While that
	 * module is switched off the provider is skipped. Unknown ids count as switched on.
	 */
	default @Nullable String getModuleId() {
		return null;
	}

	/** Cheap pre-check without recipe lookups; {@link #collect} is only called when this returns true. */
	default boolean appliesTo(EmiStack stack) {
		return true;
	}

	/**
	 * Ids of EMI recipe categories whose recipes this provider already lists for {@code stack}, so the built-in recipe
	 * provider leaves their rows ("Produced by", "Used in") out and the page does not show the same recipes twice.
	 * Only declare a category while this provider really shows that data for the stack: return an empty set when its
	 * setting is off or it has nothing for the stack, so the built-in row stays. The set is asked once per page build
	 * and only of providers that are active (module on, {@link #appliesTo} true); keep it cheap.
	 */
	default Set<Identifier> coveredCategories(EmiStack stack) {
		return Set.of();
	}

	/**
	 * Adds zero or more sections for {@code stack} (a copy with amount 1, never empty). Called at most once per stack
	 * and EMI reload; still keep it fast: use indexes such as {@code EmiApi.getRecipeManager().getRecipesByOutput},
	 * never a scan over every recipe.
	 */
	void collect(EmiStack stack, InfoCollector collector);

	/** Called once after every EMI reload, before the first {@link #collect} of that reload. Drop own caches here. */
	default void onReload() {
	}
}
