package dev.emi.remastered.info.client;

import java.util.List;

import dev.emi.emi.api.stack.EmiStack;

/**
 * The collected sections of one stack, sorted by kind and provider order.
 *
 * @param loading true while EMI is still loading (no sections, not cached)
 * @param nanos   time the providers took
 */
public record InfoPage(EmiStack stack, List<SectionImpl> sections, boolean loading, long nanos) {

	static InfoPage loading(EmiStack stack) {
		return new InfoPage(stack, List.of(), true, 0);
	}
}
