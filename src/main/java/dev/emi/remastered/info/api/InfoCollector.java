package dev.emi.remastered.info.api;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Receives the sections of one provider for one stack. Only valid during {@link ItemInfoProvider#collect}. */
public interface InfoCollector {

	/** The stack the page is about (amount 1). */
	EmiStack getStack();

	/**
	 * Adds a section and returns it for filling. The screen shows sections grouped by {@code kind}, then in provider
	 * order, then in the order they were added. A section without content is not shown.
	 *
	 * @param id    id of the section, e.g. {@code emi_loot:chest_loot} (used for debugging and exports)
	 * @param kind  where the section goes
	 * @param title section title (one line)
	 */
	InfoSection addSection(Identifier id, InfoSection.Kind kind, Text title);

	/**
	 * Whether a non-empty section of kind {@link InfoSection.Kind#OBTAIN} that counts as a source (see
	 * {@link InfoSection#source(boolean)}) was added so far, by this or an earlier provider.
	 */
	boolean hasSource();
}
