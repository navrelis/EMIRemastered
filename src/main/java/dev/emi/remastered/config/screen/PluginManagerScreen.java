package dev.emi.remastered.config.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.widget.config.EmiNameWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.module.ModuleEmiPlugin;
import dev.emi.remastered.plugin.PluginFilter;
import dev.emi.remastered.plugin.PluginManager;
import dev.emi.remastered.plugin.PluginManager.PluginEntry;
import dev.emi.remastered.plugin.PluginManager.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Lists every EMI plugin found in the last reload with its owning mod, the numbers of the last reload and an on/off
 * toggle. Changes are saved to {@code plugins.disabled} and start an EMI reload when the screen closes.
 * Opened from the "Plugins" button of EMI's config screen.
 */
public class PluginManagerScreen extends Screen {
	private static final String KEY = "screen.emi_remastered.plugins";
	private static final int MAX_TOOLTIP_WIDTH = 240;
	private final Screen last;
	private final Set<String> disabledWhenOpened;
	private ListWidget list;

	public PluginManagerScreen(Screen last) {
		super(EmiPort.translatable(KEY));
		this.last = last;
		this.disabledWhenOpened = PluginManager.getDisabled();
	}

	@Override
	protected void init() {
		super.init();
		double scroll = list == null ? 0 : list.getScrollAmount();
		this.addDrawable(new EmiNameWidget(width / 2, 16));
		int w = Math.min(400, width - 40);
		int x = (width - w) / 2;
		this.addDrawableChild(EmiPort.newButton(x + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable(KEY + ".enable_all"), button -> {
			for (PluginEntry entry : PluginManager.getPlugins()) {
				PluginManager.setDisabled(entry.id(), false);
			}
		}));
		this.addDrawableChild(EmiPort.newButton(x + w / 2 + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable("gui.done"), button -> {
			this.close();
		}));
		list = new ListWidget(client, width, height, 40, height - 56);
		for (PluginEntry entry : PluginManager.getPlugins()) {
			list.addEntry(new PluginWidget(entry));
		}
		this.addSelectableChild(list);
		list.setScrollAmount(scroll);
	}

	@Override
	public void render(DrawContext raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		list.setScrollAmount(list.getScrollAmount());
		super.render(context.raw(), mouseX, mouseY, delta);
		list.render(context.raw(), mouseX, mouseY, delta);
		int w = Math.min(400, width - 40);
		context.drawCenteredTextWithShadow(fit(getSummary(), width - 20), width / 2, 28, 0xFFFFFF);
		context.drawCenteredTextWithShadow(fit(EmiPort.translatable(KEY + ".note"), w), width / 2, height - 48, 0xA0A0A0);
		if (list.getHoveredEntry() != null) {
			EmiRenderHelper.drawTooltip(this, context, list.getHoveredEntry().getTooltip(mouseX, mouseY), mouseX, mouseY,
				Math.min(width / 2 - 16, MAX_TOOLTIP_WIDTH));
		}
	}

	private Text getSummary() {
		List<PluginEntry> plugins = PluginManager.getPlugins();
		int off = 0;
		for (PluginEntry entry : plugins) {
			if (PluginManager.isDisabled(entry.id())) {
				off++;
			}
		}
		if (PluginManager.hasReloaded()) {
			return EmiPort.translatable(KEY + ".summary", plugins.size(), off, PluginManager.getLastReloadMs());
		}
		return EmiPort.translatable(KEY + ".summary.no_reload", plugins.size(), off);
	}

	private Text fit(Text text, int width) {
		if (textRenderer.getWidth(text) <= width) {
			return text;
		}
		return EmiPort.literal(textRenderer.trimToWidth(text.getString(), Math.max(0, width - textRenderer.getWidth("..."))) + "...",
			text.getStyle());
	}

	@Override
	public void close() {
		Set<String> now = PluginManager.getDisabled();
		if (!now.equals(disabledWhenOpened)) {
			EmiRemastered.LOGGER.info("Disabled EMI plugins changed to [{}], reloading EMI", PluginFilter.format(now));
			PluginManager.save();
			RemasteredConfigScreens.requestReload();
		}
		MinecraftClient.getInstance().setScreen(last);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		} else if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
			this.close();
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_TAB) {
			return false;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private static String getModName(PluginEntry entry) {
		if (entry.plugin() instanceof ModuleEmiPlugin modulePlugin) {
			return EmiRemastered.NAME + ": " + EmiPort.translatable(modulePlugin.getModule().getNameKey()).getString();
		}
		try {
			return EmiAgnos.getModName(entry.id());
		} catch (Throwable t) {
			return entry.id();
		}
	}

	/** One row: [!] id  mod name ........ time [On/Off] */
	private class PluginWidget extends ListWidget.Entry {
		private static final int BUTTON_WIDTH = 64;
		private final PluginEntry entry;
		private final String modName;
		private final ButtonWidget button;

		PluginWidget(PluginEntry entry) {
			this.entry = entry;
			this.modName = getModName(entry);
			this.button = EmiPort.newButton(0, 0, BUTTON_WIDTH, 20, getButtonText(), b -> {
				PluginManager.setDisabled(entry.id(), !PluginManager.isDisabled(entry.id()));
			});
			this.button.active = PluginFilter.canDisable(entry.id());
		}

		private Text getButtonText() {
			if (!PluginFilter.canDisable(entry.id())) {
				return EmiPort.translatable(KEY + ".locked", Formatting.GRAY);
			} else if (PluginManager.isDisabled(entry.id())) {
				return EmiPort.translatable(KEY + ".off", Formatting.RED);
			}
			return EmiPort.translatable(KEY + ".on", Formatting.GREEN);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(button);
		}

		@Override
		public void render(DrawContext raw, int index, int y, int x, int width, int height, int mouseX, int mouseY,
				boolean hovered, float delta) {
			EmiDrawContext context = EmiDrawContext.wrap(raw);
			context.fill(x, y, width, height, 0x66000000);
			Stats stats = PluginManager.getStats(entry.id());
			boolean disabled = PluginManager.isDisabled(entry.id());
			int textY = y + 10 - textRenderer.fontHeight / 2;
			int left = x + 6;
			if (stats != null && stats.error != null) {
				context.drawTextWithShadow(EmiPort.literal("!", Formatting.RED, Formatting.BOLD), left, textY, 0xFFFFFF);
				left += 8;
			}
			button.setMessage(getButtonText());
			button.setX(x + width - BUTTON_WIDTH);
			button.setY(y);
			String time = "";
			if (stats != null && stats.registerMs >= 0) {
				time = Math.max(0, stats.initMs) + stats.registerMs + " ms";
			} else if (disabled || PluginManager.hasReloaded()) {
				time = "-";
			}
			int timeWidth = textRenderer.getWidth(time);
			int right = x + width - BUTTON_WIDTH - 6;
			context.drawTextWithShadow(EmiPort.literal(time), right - timeWidth, textY, 0xA0A0A0);
			int available = right - timeWidth - 8 - left;
			MutableText name = EmiPort.literal(entry.id(), disabled ? Formatting.GRAY : Formatting.WHITE);
			String idText = textRenderer.trimToWidth(entry.id(), available);
			if (!idText.equals(entry.id())) {
				name = EmiPort.literal(idText, disabled ? Formatting.GRAY : Formatting.WHITE);
			} else {
				int rest = available - textRenderer.getWidth(entry.id()) - 6;
				if (rest > 12) {
					String mod = textRenderer.getWidth(modName) <= rest ? modName
						: textRenderer.trimToWidth(modName, rest - textRenderer.getWidth("...")) + "...";
					name = EmiPort.append(name, EmiPort.literal("  " + mod, Formatting.DARK_GRAY));
				}
			}
			context.drawTextWithShadow(name, left, textY, 0xFFFFFF);
			button.render(raw, mouseX, mouseY, delta);
		}

		@Override
		public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
			List<Text> lines = new ArrayList<>();
			String id = entry.id();
			lines.add(EmiPort.literal(id, Formatting.WHITE));
			lines.add(EmiPort.translatable(KEY + ".tooltip.mod", modName).formatted(Formatting.GRAY));
			if (!PluginFilter.canDisable(id)) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.core").formatted(Formatting.GRAY));
			}
			boolean disabledNow = PluginManager.isDisabled(id);
			if (disabledNow) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.disabled").formatted(Formatting.RED));
			}
			if (disabledNow != disabledWhenOpened.contains(id)) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.pending").formatted(Formatting.YELLOW));
			}
			if (entry.plugin() instanceof ModuleEmiPlugin modulePlugin && !modulePlugin.getModule().isEnabled()) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.module_off").formatted(Formatting.GOLD));
			}
			Stats stats = PluginManager.getStats(id);
			if (stats != null) {
				if (stats.initMs >= 0) {
					lines.add(EmiPort.translatable(KEY + ".tooltip.initialize", stats.initMs).formatted(Formatting.GRAY));
				}
				if (stats.registerMs >= 0) {
					lines.add(EmiPort.translatable(KEY + ".tooltip.register", stats.registerMs).formatted(Formatting.GRAY));
					lines.add(EmiPort.translatable(KEY + ".tooltip.content", stats.recipes, stats.stacks).formatted(Formatting.GRAY));
				}
				if (stats.error != null) {
					lines.add(EmiPort.translatable(KEY + ".tooltip.error",
						EmiPort.translatable(KEY + ".phase." + stats.errorPhase.key), stats.error).formatted(Formatting.RED));
				}
			} else if (PluginManager.hasReloaded() && !disabledWhenOpened.contains(id)) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.not_run").formatted(Formatting.GRAY));
			} else if (!PluginManager.hasReloaded()) {
				lines.add(EmiPort.translatable(KEY + ".tooltip.no_reload").formatted(Formatting.GRAY));
			}
			List<TooltipComponent> list = new ArrayList<>();
			for (Text line : lines) {
				list.add(EmiTooltipComponents.of(line));
			}
			return list;
		}

		@Override
		public int getHeight() {
			return 20;
		}
	}
}
