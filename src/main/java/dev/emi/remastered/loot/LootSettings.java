package dev.emi.remastered.loot;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigGroup;
import dev.emi.emi.config.EmiConfig.ConfigGroupEnd;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Settings of the loot module, shown in the EMI config screen. The fields are read directly.
 * <p>
 * The "parsing" settings are read where the loot tables are parsed, i.e. on the (integrated) server.
 * A dedicated server does not load the EMI config and always uses the defaults.
 */
public class LootSettings {

	public static final String DEFAULT_SKIPPED_KEYS = "emi_loot.function.set_count_add,emi_loot.function.set_count_set,"
		+ "emi_loot.function.fill_player_head,emi_loot.function.limit_count,emi_loot.no_conditions";

	// Parsing (server side)

	@ConfigGroup("loot.parsing")
	@Comment("Whether chest loot tables are parsed and sent to clients.")
	@ConfigValue("loot.parse-chest-loot")
	public static boolean parseChestLoot = true;

	@Comment("Whether block loot tables are parsed and sent to clients.")
	@ConfigValue("loot.parse-block-loot")
	public static boolean parseBlockLoot = true;

	@Comment("Whether mob loot tables are parsed and sent to clients.")
	@ConfigValue("loot.parse-mob-loot")
	public static boolean parseMobLoot = true;

	@Comment("Whether gameplay loot tables (fishing, gifts, bartering) are parsed and sent to clients.")
	@ConfigValue("loot.parse-gameplay-loot")
	public static boolean parseGameplayLoot = true;

	@Comment("Whether archaeology loot tables are parsed and sent to clients.")
	@ConfigValue("loot.parse-archaeology-loot")
	public static boolean parseArchaeologyLoot = true;

	@Comment("Whether the built-in direct drops (things mobs and blocks drop in code) are added to chest loot.")
	@ConfigValue("loot.direct-drops-chest")
	public static boolean directDropsChest = true;

	@Comment("Whether the built-in direct drops (things mobs and blocks drop in code) are added to block loot.")
	@ConfigValue("loot.direct-drops-block")
	public static boolean directDropsBlock = true;

	@Comment("Whether the built-in direct drops (things mobs and blocks drop in code) are added to mob loot.")
	@ConfigValue("loot.direct-drops-mob")
	public static boolean directDropsMob = true;

	@Comment("Whether the built-in direct drops (things mobs and blocks drop in code) are added to gameplay loot.")
	@ConfigValue("loot.direct-drops-gameplay")
	public static boolean directDropsGameplay = true;

	@Comment("Whether the built-in direct drops (things mobs and blocks drop in code) are added to archaeology loot.")
	@ConfigValue("loot.direct-drops-archaeology")
	public static boolean directDropsArchaeology = true;

	@Comment("Whether chest loot is sent together with the structures it is found in (and their dimensions).")
	@ConfigValue("loot.chest-structures")
	public static boolean chestStructures = true;

	@ConfigGroupEnd
	@Comment("Comma separated condition keys that are not shown because they say nothing useful. "
		+ "Remove a key to show it. 'emi_loot.no_conditions' only matters in the tooltip and plain condition styles.")
	@ConfigValue("loot.skipped-keys")
	public static String skippedKeys = DEFAULT_SKIPPED_KEYS;

	// Display (client side)

	@ConfigGroup("loot.display")
	@Comment("How the conditions of a drop are displayed. Default shows icons, tooltip shows text lines in the drop tooltip, plain shows none.")
	@ConfigValue("loot.condition-style")
	public static LootConditionStyle conditionStyle = LootConditionStyle.DEFAULT;

	@Comment("How many decimal places are shown in drop chances (0 to 10).")
	@ConfigValue("loot.chance-decimal-places")
	public static int chanceDecimalPlaces = 1;

	@Comment("Whether the clickable loot table names are underlined.")
	@ConfigValue("loot.underline-links")
	public static boolean underlineLinks = true;

	@Comment("Always stack identical items in chest and archaeology loot instead of listing every chance.")
	@ConfigValue("loot.chest-loot-always-stack-same")
	public static boolean chestLootAlwaysStackSame = false;

	@Comment("Put trial chamber loot in its own category instead of the chest loot category. Needs an EMI reload.")
	@ConfigValue("loot.split-chest-loot-by-trial")
	public static boolean splitChestLootByTrial = false;

	@Comment("Tighter rows for chest loot.")
	@ConfigValue("loot.compact-chest")
	public static boolean compactChest = true;

	@Comment("Group block drops that share chance and conditions into one slot (tooltip condition style only).")
	@ConfigValue("loot.compact-block")
	public static boolean compactBlock = true;

	@Comment("Group mob drops that share chance and conditions into one slot (tooltip condition style only).")
	@ConfigValue("loot.compact-mob")
	public static boolean compactMob = true;

	@Comment("Group gameplay drops that share chance and conditions into one slot (tooltip condition style only).")
	@ConfigValue("loot.compact-gameplay")
	public static boolean compactGameplay = true;

	@Comment("Show where a chest is found (structures and dimensions) on chest loot pages.")
	@ConfigValue("loot.show-structures")
	public static boolean showStructures = true;

	@Comment("Show special icons and a level scaling hint for drops that depend on Looting, Fortune, Silk Touch, fire or a player kill.")
	@ConfigValue("loot.condition-hints")
	public static boolean conditionHints = true;

	@Comment("Show a note on chest loot pages when Lootr is installed, because every player gets their own loot there.")
	@ConfigValue("loot.lootr-note")
	public static boolean lootrNote = true;

	@ConfigGroupEnd
	@Comment("Tighter rows for archaeology loot.")
	@ConfigValue("loot.compact-archaeology")
	public static boolean compactArchaeology = true;

	// Debug

	@ConfigGroup("loot.debug")
	@Comment("Log what the loot parser does. Very noisy, only for troubleshooting.")
	@ConfigValue("loot.debug")
	public static boolean debug = false;

	@Comment("Log the block loot tables the client receives.")
	@ConfigValue("loot.debug-block")
	public static boolean debugBlock = false;

	@Comment("Log the chest loot tables the client receives.")
	@ConfigValue("loot.debug-chest")
	public static boolean debugChest = false;

	@Comment("Log the mob loot tables the client receives and the mob display fixers that are loaded.")
	@ConfigValue("loot.debug-mob")
	public static boolean debugMob = false;

	@Comment("Log the gameplay loot tables the client receives.")
	@ConfigValue("loot.debug-gameplay")
	public static boolean debugGameplay = false;

	@Comment("Log the archaeology loot tables the client receives.")
	@ConfigValue("loot.debug-archaeology")
	public static boolean debugArchaeology = false;

	@Comment("Log chest loot tables that have no translated name. Always on in a development environment.")
	@ConfigValue("loot.log-untranslated-chest")
	public static boolean logUntranslatedChest = false;

	@Comment("Log gameplay loot tables that have no translated name. Always on in a development environment.")
	@ConfigValue("loot.log-untranslated-gameplay")
	public static boolean logUntranslatedGameplay = false;

	@ConfigGroupEnd
	@Comment("Log archaeology loot tables that have no translated name. Always on in a development environment.")
	@ConfigValue("loot.log-untranslated-archaeology")
	public static boolean logUntranslatedArchaeology = false;

	private static volatile String parsedSkippedFrom = null;
	private static volatile Set<String> parsedSkipped = Set.of();

	public static Set<String> parseKeys(String keys) {
		Set<String> set = new HashSet<>();
		if (keys != null) {
			for (String key : keys.split("[,\\s]+")) {
				if (!key.isEmpty()) {
					set.add(key);
				}
			}
		}
		return set;
	}

	public static boolean isSkippedKey(String key) {
		String current = skippedKeys;
		if (!Objects.equals(current, parsedSkippedFrom)) {
			parsedSkipped = parseKeys(current);
			parsedSkippedFrom = current;
		}
		return parsedSkipped.contains(key);
	}

	public static boolean isDevelopment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	private static final Formatting[] UNDERLINE = {Formatting.UNDERLINE};
	private static final Formatting[] NO_FORMATTING = {};

	public static Formatting[] linkFormatting() {
		return underlineLinks ? UNDERLINE : NO_FORMATTING;
	}

	public static boolean isTooltipStyle() {
		return conditionStyle != LootConditionStyle.DEFAULT;
	}

	public static boolean showConditionsInTooltip() {
		return conditionStyle == LootConditionStyle.TOOLTIP;
	}

	public static int chanceDecimalPlaces() {
		return Math.max(0, Math.min(10, chanceDecimalPlaces));
	}
}
