package dev.emi.remastered.info.search;

import java.util.regex.Pattern;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.search.Query;

/** {@code ~/regex/}: a source label of the stack matches the regular expression (case-insensitive). */
public class RegexSourceQuery extends Query {
	private final Pattern pattern;

	public RegexSourceQuery(String regex) {
		this.pattern = SourceMatch.compile(regex);
	}

	@Override
	public boolean matches(EmiStack stack) {
		SourceIndex.Snapshot snapshot = SourceIndex.current();
		return snapshot.active() && SourceMatch.matchesRegex(pattern, snapshot.labelsOf(stack));
	}

	@Override
	public boolean matchesUnbaked(EmiStack stack) {
		if (pattern == null || !SourceIndex.current().active()) {
			return false;
		}
		return SourceMatch.matchesRegex(pattern, new SourceIndexBuilder().compute(stack));
	}
}
