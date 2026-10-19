package dev.emi.remastered.tools;

import dev.emi.emi.config.EmiConfig.Comment;
import dev.emi.emi.config.EmiConfig.ConfigValue;
import dev.emi.remastered.module.Module;

/**
 * Module {@code tools}: client-only helpers for pack developers, new in EMI Remastered.
 * <ul>
 * <li>Tag browser ({@link dev.emi.remastered.tools.client.TagBrowserScreen}): every tag of every registry with
 * its members, search, reverse lookup.</li>
 * <li>Copy recipe: copies the hovered recipe of EMI's recipe screen to the clipboard as text or JSON.</li>
 * </ul>
 * No EMI plugin and no server part. Both features hook EMI's input handling with the mixin
 * {@link dev.emi.remastered.tools.mixin.EmiScreenManagerMixin}, which does nothing while the switch is off.
 * Settings: {@link ToolsSettings} (group {@code tools}); lang files in {@code assets/emi_remastered_tools/lang/}.
 */
public final class ToolsModule extends Module {

	/** Module switch, shown in the config group "modules". The default is the {@code defaultEnabled} argument below. */
	@Comment("Tag browser and copy recipe as text or JSON (tools for pack developers).")
	@ConfigValue("modules.tools")
	public static boolean enabled = true;

	public ToolsModule() {
		super("tools", "emi_remastered_tools", ToolsSettings.class, true, false, ApplyMode.IMMEDIATE);
	}

	/** Both sides, always (also on dedicated servers, whatever the client switch says): nothing to do, client only. */
	@Override
	public void init() {
	}

	/** Client only, called when the module switch is on at startup. */
	@Override
	public void initClient() {
	}
}
