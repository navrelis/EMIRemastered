package dev.emi.remastered.info.search;

import java.util.List;

/**
 * Implemented by an {@code EmiRecipe} that knows more about where its outputs come from than its category name does
 * (the structures a chest is found in, the mob that drops an item, the villager that sells it, ...). The {@code ~}
 * search query ({@link SourceQuery}) matches these labels in addition to the category name and id.
 * <p>
 * Called on EMI's reload thread (search bake) and on a background thread when the index is refreshed, often once per
 * stack the recipe produces: keep it cheap (cache what is not trivial) and thread safe. Labels are translated text as
 * the player sees it, or ids; case does not matter.
 */
public interface SourceLabels {

	List<String> sourceLabels();
}
