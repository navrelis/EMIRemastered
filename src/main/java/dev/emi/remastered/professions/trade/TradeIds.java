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
