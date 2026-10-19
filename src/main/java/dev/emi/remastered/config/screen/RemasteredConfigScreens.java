package dev.emi.remastered.config.screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import dev.emi.emi.screen.widget.config.GroupNameWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import dev.emi.remastered.config.JumpFit;
import dev.emi.remastered.config.ModuleConfigs;
import dev.emi.remastered.module.Module;
import dev.emi.remastered.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-side hooks of EMI's {@code ConfigScreen} for the module config groups and the plugin manager. */
public final class RemasteredConfigScreens {

	/** Number of jump entries EMI's own {@code ConfigScreen} starts with. */
	private static final int EMI_JUMPS = 11;

	private RemasteredConfigScreens() {
	}

	/** EMI's own fields (unchanged order) followed by the visible module fields. */
	public static List<Field> getScreenFields() {
		List<Field> list = new ArrayList<>(Arrays.asList(EmiConfig.class.getFields()));
		list.addAll(ModuleConfigs.getScreenFields());
		return list;
	}

	/** Widgets for types EMI has setters for but no widget ({@code double}, {@code String}); null otherwise. */
	public static ConfigEntryWidget createExtraWidget(ConfigScreen screen, Field field, Text translation,
			List<TooltipComponent> tooltip, Supplier<String> search) {
		if (field.getType() == double.class) {
			return new DoubleWidget(translation, tooltip, search, screen.objectMutator(field));
		} else if (field.getType() == String.class) {
			return new StringWidget(translation, tooltip, search, screen.objectMutator(field));
		}
		return null;
	}

	/** Appends "when does this apply" to the tooltip of a module switch. */
	public static List<TooltipComponent> decorateTooltip(Field field, List<TooltipComponent> tooltip) {
		Module module = ModuleConfigs.getModuleForSwitch(field);
		if (module == null) {
			return tooltip;
		}
		List<TooltipComponent> list = new ArrayList<>(tooltip);
		for (String line : I18n.translate(module.getApplyMode().langKey).split("\n")) {
			list.add(EmiTooltipComponents.of(EmiPort.literal(line, Formatting.GRAY)));
		}
		return list;
	}

	/** Called when EMI's config screen closes: reload EMI if a module switch with apply mode RELOAD changed. */
	public static void onConfigClosed(Map<String, Boolean> switchesWhenOpened) {
		boolean reload = false;
		for (Module module : Modules.all()) {
			Boolean before = switchesWhenOpened.get(module.getId());
			if (before != null && before != module.isEnabled()) {
				if (module.getApplyMode() == Module.ApplyMode.RELOAD) {
					reload = true;
				}
				EmiLog.info("Module " + module.getId() + " switched " + (module.isEnabled() ? "on" : "off"));
			}
		}
		if (reload) {
			requestReload();
		}
	}

	/** Starts an EMI reload if a world is loaded (same condition as EMI's resource reload hook). */
	public static void requestReload() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world != null && client.world.getRecipeManager() != null) {
			EmiReloadManager.reload();
		}
	}

	// ------------------------------------------------------------------ jump buttons

	/** {@code modules} followed by every module group present in the list, in registry order. */
	public static List<String> getJumpTargets(ListWidget list) {
		Set<String> present = new HashSet<>();
		for (ListWidget.Entry e : list.children()) {
			if (e instanceof GroupNameWidget g) {
				present.add(g.id);
			}
		}
		List<String> jumps = new ArrayList<>();
		if (present.contains(ModuleConfigs.MODULES_GROUP)) {
			jumps.add(ModuleConfigs.MODULES_GROUP);
		}
		for (Module module : Modules.all()) {
			if (present.contains(module.getId())) {
				jumps.add(module.getId());
			}
		}
		return jumps;
	}

	/**
	 * Called by EMI's {@code ConfigScreen} when the jump buttons do not all fit: keeps as many module jumps as fit
	 * (same space calculation as EMI: logical list height minus 10, 16 pixels per entry, EMI's own 11 jumps).
	 */
	public static void trimJumpTargets(List<String> jumps) {
		if (MinecraftClient.getInstance().currentScreen instanceof ConfigScreen screen && screen.list != null) {
			trimJumpTargets(jumps, EMI_JUMPS, screen.list.getLogicalHeight() - 10);
		} else {
			trimJumpTargets(jumps, EMI_JUMPS, 0);
		}
	}

	/**
	 * Shortens the module jumps to what fits next to EMI's own {@code emiJumps} entries within {@code space} pixels:
	 * {@code modules} stays first (as when everything fits), followed by the first module groups in registry order.
	 */
	static void trimJumpTargets(List<String> jumps, int emiJumps, int space) {
		int keep = JumpFit.fittingModuleJumps(space, emiJumps, jumps.size(), JumpFit.ICON_SIZE);
		if (keep >= jumps.size()) {
			return;
		}
		boolean modules = jumps.remove(ModuleConfigs.MODULES_GROUP);
		while (jumps.size() > Math.max(0, keep - 1)) {
			jumps.remove(jumps.size() - 1);
		}
		if (modules) {
			jumps.add(0, ModuleConfigs.MODULES_GROUP);
		}
	}

	/**
	 * Adds the jump buttons below EMI's own ones ({@code modules} at EMI's group indent, module groups indented like
	 * EMI's sub groups).
	 *
	 * @return the next free y
	 */
	public static int addJumpButtons(List<String> jumps, int y, Consumer<String> jump, Consumer<ButtonWidget> add) {
		for (String s : jumps) {
			boolean top = s.equals(ModuleConfigs.MODULES_GROUP);
			int icon = 0;
			if (!top) {
				icon = Modules.all().indexOf(Modules.byId(s).orElse(null)) + 1;
			}
			add.accept(new ModuleJumpButton(2 + (top ? 0 : 8), y, icon, w -> jump.accept(s),
				List.of(EmiPort.translatable("config.emi.group." + s.replace('-', '_')))));
			y += 16;
		}
		return y;
	}

	/** The "Plugins" button next to EMI's presets button; opens the plugin manager. */
	public static ButtonWidget createPluginsButton(int x, int y, int w, ConfigScreen screen) {
		return EmiPort.newButton(x, y, w, 20, EmiPort.translatable("screen.emi_remastered.plugins.button"), button -> {
			MinecraftClient.getInstance().setScreen(new PluginManagerScreen(screen));
		});
	}
}
