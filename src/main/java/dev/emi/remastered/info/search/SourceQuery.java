package dev.emi.remastered.info.search;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.search.Query;

/**
 * {@code ~text}: stacks with a source label containing the text (see {@link SourceIndex}), or with {@code ~nosource}
 * the stacks the Item Info screen marks as "No Known Source".
 */
public class SourceQuery extends Query {
	private final String needle;
	private final boolean noSourceKeyword;

	public SourceQuery(String text) {
		this.needle = SourceText.normalize(text);
		this.noSourceKeyword = SourceText.isNoSource(text);
	}

	@Override
	public boolean matches(EmiStack stack) {
		SourceIndex.Snapshot snapshot = SourceIndex.current();
		return snapshot.active() && SourceMatch.matches(snapshot.labelsOf(stack), snapshot.isNoSource(stack), needle, noSourceKeyword);
	}

	@Override
	public boolean matchesUnbaked(EmiStack stack) {
		return SourceIndex.matchesUnbaked(stack, needle, noSourceKeyword);
	}
}
