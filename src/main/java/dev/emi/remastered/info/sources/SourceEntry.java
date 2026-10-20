package dev.emi.remastered.info.sources;

import java.util.List;

/**
 * One validated entry of an {@code emi_sources} file (see docs/SOURCES.md), still plain text: item ids are resolved
 * against the registries later by {@link SourceLoader}.
 *
 * @param fileId      resource id of the file it came from, for log lines
 * @param index       position in the file's {@code sources} array
 * @param id          entry id as {@code namespace:path}, never null (default: file id plus index)
 * @param output      item id of the output
 * @param count       amount of the output, 1 to {@link SourceParser#MAX_COUNT}
 * @param from        item ids or {@code #tag} ids shown as the source, may be empty
 * @param text        translation key or literal text
 * @param chance      0 to 1, or null when not given
 * @param requiresMod mod id the entry needs, or null (already checked by the parser)
 */
public record SourceEntry(String fileId, int index, String id, String output, int count, List<String> from, String text,
		Double chance, String requiresMod) {

	/** The recipe id path: {@code /other_sources/<namespace>/<path>} (the namespace keeps equal paths apart). */
	public String recipePath() {
		int colon = id.indexOf(':');
		return "/other_sources/" + id.substring(0, colon) + "/" + id.substring(colon + 1);
	}
}
