package dev.emi.remastered.professions.trade;

import java.util.Locale;

/**
 * Pure id helpers for trade recipes, no Minecraft classes so they can be unit tested.
 * Recipe ids are synthetic ({@code emiprofessions:/trade/<owner>/<level>/<index>}) and must stay stable between reloads.
 */
public final class TradeIds {
	/** Owner key of the wandering trader. */
	public static final String WANDERING_TRADER = "wandering_trader";

	private TradeIds() {
	}

	/**
	 * Key of a profession inside recipe ids: vanilla professions use their plain path, others {@code namespace/path}.
	 */
	public static String ownerKey(String namespace, String path) {
		String key = "minecraft".equals(namespace) ? path : namespace + "/" + path;
		return sanitize(key);
	}

	/**
	 * Path of a trade recipe id.
	 *
	 * @param owner Owner key, see {@link #ownerKey(String, String)}
	 * @param level Villager level 1..5 (pool number for the wandering trader)
	 * @param index Position of the trade inside its level, after de-duplication
	 */
	public static String recipePath(String owner, int level, int index) {
		return "/trade/" + owner + "/" + level + "/" + index;
	}

	/**
	 * Path of the synthetic id of a profession page: {@code /profession/<namespace>/<path>}. The leading slash tells EMI
	 * the id is not a vanilla recipe id, so it never looks it up in the recipe manager.
	 */
	public static String professionPath(String namespace, String path) {
		return "/profession/" + sanitize(namespace) + "/" + sanitize(path);
	}

	/**
	 * A parsed trade recipe path, see {@link #parse(String)}.
	 *
	 * @param owner Owner key
	 * @param level Villager level or pool number
	 * @param index Position inside the level
	 */
	public record Parsed(String owner, int level, int index) {
	}

	/**
	 * Reads a path made by {@link #recipePath(String, int, int)}: the last two segments are numbers, everything between
	 * {@code /trade/} and them is the owner key (it may contain a slash for modded professions).
	 *
	 * @return The parts, null if the path has another shape
	 */
	public static Parsed parse(String path) {
		if (path == null || !path.startsWith("/trade/")) {
			return null;
		}
		String rest = path.substring("/trade/".length());
		int last = rest.lastIndexOf('/');
		if (last <= 0) {
			return null;
		}
		int middle = rest.lastIndexOf('/', last - 1);
		if (middle <= 0) {
			return null;
		}
		try {
			return new Parsed(rest.substring(0, middle), Integer.parseInt(rest.substring(middle + 1, last)),
				Integer.parseInt(rest.substring(last + 1)));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Order of trades on a page: owner key, then level, then index (numerically). Paths that cannot be parsed come
	 * last, ordered as text.
	 */
	public static int compare(String a, String b) {
		Parsed pa = parse(a);
		Parsed pb = parse(b);
		if (pa == null || pb == null) {
			return pa == null && pb == null ? a.compareTo(b) : pa == null ? 1 : -1;
		}
		int c = pa.owner().compareTo(pb.owner());
		if (c == 0) {
			c = Integer.compare(pa.level(), pb.level());
		}
		if (c == 0) {
			c = Integer.compare(pa.index(), pb.index());
		}
		return c;
	}

	/**
	 * Characters an identifier path may contain are {@code a-z 0-9 / . _ -}, everything else becomes {@code _}.
	 */
	public static String sanitize(String path) {
		String lower = path.toLowerCase(Locale.ROOT);
		StringBuilder out = new StringBuilder(lower.length());
		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '/' || c == '.' || c == '_' || c == '-';
			out.append(valid ? c : '_');
		}
		return out.toString();
	}
}
