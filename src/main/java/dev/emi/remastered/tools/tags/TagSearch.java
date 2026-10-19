package dev.emi.remastered.tools.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filters and ranks tags for the tag browser. Pure logic, no Minecraft classes.
 * <p>
 * Ranking (best first): the tag path equals the token, the tag id or path starts with it, the tag id contains it, only a
 * member contains it. Ties keep the order of the input list (registry, then id).
 */
public final class TagSearch {
	private static final int MEMBER_SCORE = 3;

	private TagSearch() {
	}

	private record Token(String text, boolean idAllowed, boolean memberAllowed) {
	}

	public static List<TagEntry> filter(List<TagEntry> all, TagFilter filter) {
		List<Token> tokens = parse(filter.query());
		List<TagEntry> candidates = new ArrayList<>();
		List<int[]> scores = new ArrayList<>();
		for (TagEntry entry : all) {
			if (filter.registry() != null && !filter.registry().equals(entry.registry())) {
				continue;
			}
			TagFilter.MemberRef member = filter.member();
			if (member != null) {
				if (member.registry() != null && !member.registry().equals(entry.registry())) {
					continue;
				}
				if (!entry.hasMember(member.id())) {
					continue;
				}
			}
			int score = 0;
			for (Token token : tokens) {
				int tokenScore = score(entry, token);
				if (tokenScore < 0) {
					score = -1;
					break;
				}
				score = Math.max(score, tokenScore);
			}
			if (score >= 0) {
				candidates.add(entry);
				scores.add(new int[] {score, candidates.size() - 1});
			}
		}
		if (tokens.isEmpty()) {
			return candidates;
		}
		scores.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
		List<TagEntry> result = new ArrayList<>(candidates.size());
		for (int[] score : scores) {
			result.add(candidates.get(score[1]));
		}
		return result;
	}

	/** Score of one token against one tag: 0 is best, -1 means no match. */
	private static int score(TagEntry entry, Token token) {
		if (token.idAllowed()) {
			String needle = token.text();
			if (entry.pathLower().equals(needle) || entry.idLower().equals(needle)) {
				return 0;
			}
			if (entry.pathLower().startsWith(needle) || entry.idLower().startsWith(needle)) {
				return 1;
			}
			if (entry.idLower().contains(needle)) {
				return 2;
			}
		}
		if (token.memberAllowed() && entry.anyMemberContains(token.text())) {
			return MEMBER_SCORE;
		}
		return -1;
	}

	private static List<Token> parse(String query) {
		List<Token> tokens = new ArrayList<>();
		for (String raw : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
			if (raw.isEmpty()) {
				continue;
			}
			if (raw.charAt(0) == '#') {
				String text = raw.substring(1);
				if (!text.isEmpty()) {
					tokens.add(new Token(text, true, false));
				}
			} else if (raw.charAt(0) == '@') {
				String text = raw.substring(1);
				if (!text.isEmpty()) {
					tokens.add(new Token(text, false, true));
				}
			} else {
				tokens.add(new Token(raw, true, true));
			}
		}
		return tokens;
	}
}
