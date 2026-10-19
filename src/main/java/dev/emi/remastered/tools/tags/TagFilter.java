package dev.emi.remastered.tools.tags;

import org.jetbrains.annotations.Nullable;

/**
 * Filter of the tag browser.
 *
 * @param query    search text; whitespace separated tokens that must all match. A token matches the tag id or any member
 *                 id (substring, case insensitive). {@code #token} matches the tag id only, {@code @token} members only.
 * @param registry registry id the tags must belong to, or null for all registries
 * @param member   a member the tags must contain, or null
 */
public record TagFilter(String query, @Nullable String registry, @Nullable MemberRef member) {
	public static final TagFilter ALL = new TagFilter("", null, null);

	public TagFilter {
		query = query == null ? "" : query;
	}

	public TagFilter withQuery(String query) {
		return new TagFilter(query, registry, member);
	}

	public TagFilter withRegistry(@Nullable String registry) {
		return new TagFilter(query, registry, member);
	}

	public TagFilter withMember(@Nullable MemberRef member) {
		return new TagFilter(query, registry, member);
	}

	/**
	 * A member to look for.
	 *
	 * @param registry registry id, or null to accept the member in any registry (an item id is often also a block id)
	 * @param id       member id, e.g. {@code minecraft:oak_log}
	 */
	public record MemberRef(@Nullable String registry, String id) {
	}
}
