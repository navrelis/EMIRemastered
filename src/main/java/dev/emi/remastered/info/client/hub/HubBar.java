package dev.emi.remastered.info.client.hub;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.remastered.info.InfoSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Builds, draws and explains the item view's tab bar. Client render thread only. */
public final class HubBar {
	static final int PAD = 5;
	static final int ICON_PAD = 4;
	static final int ICON_GAP = 3;
	static final int COUNT_GAP = 4;
	private static final HubTab[] ORDER = HubTab.values();

	private HubBar() {
	}

	/**
	 * The tabs for a screen showing {@code context}, centred on {@code centerX}, standing on {@code plateTop} (the top
	 * of the plate or panel the selected tab merges into).
	 */
	public static List<HubTabWidget> create(Hub.Context context, int centerX, int plateTop) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		Text[] labels = new Text[ORDER.length];
		Text[] shortLabels = new Text[ORDER.length];
		int[] count = new int[ORDER.length];
		int[] labelW = new int[ORDER.length - 1];
		int[] countW = new int[ORDER.length - 1];
		int[] shortW = new int[ORDER.length - 1];
		for (int i = 0; i < ORDER.length; i++) {
			HubTab tab = ORDER[i];
			labels[i] = EmiPort.translatable("emi_info.hub." + key(tab));
			if (tab != HubTab.INFO) {
				shortLabels[i] = EmiPort.translatable("emi_info.hub." + key(tab) + ".short");
				count[i] = Hub.count(context.stack(), tab);
				labelW[i - 1] = PAD * 2 + font.getWidth(labels[i]);
				countW[i - 1] = labelW[i - 1] + (count[i] > 0 ? COUNT_GAP + font.getWidth(String.valueOf(count[i])) : 0);
				shortW[i - 1] = PAD * 2 + font.getWidth(shortLabels[i]);
			}
		}
		int infoIcon = ICON_PAD * 2 + 16;
		int infoFull = ICON_PAD + 16 + ICON_GAP + font.getWidth(labels[0]) + PAD;
		int available = Hub.barWidth();
		HubRules.Fit fit = HubRules.fit(available, infoIcon, infoFull, labelW, countW, shortW);
		int[] widths = new int[ORDER.length];
		widths[0] = fit == HubRules.Fit.FULL ? infoFull : infoIcon;
		for (int i = 1; i < ORDER.length; i++) {
			widths[i] = switch (fit) {
				case FULL, ICON_INFO -> countW[i - 1];
				case NO_COUNTS -> labelW[i - 1];
				case SHORT -> shortW[i - 1];
				case TRIMMED -> Math.max(PAD * 2 + 8, (available - infoIcon) / (ORDER.length - 1));
			};
		}
		int total = 0;
		for (int w : widths) {
			total += w;
		}
		int x = centerX - total / 2;
		int y = plateTop - HubRules.TAB_HEIGHT;
		List<HubTabWidget> tabs = new ArrayList<>();
		for (int i = 0; i < ORDER.length; i++) {
			HubTab tab = ORDER[i];
			boolean enabled = HubRules.enabled(tab, count[1], count[2]);
			Text label = labels[i];
			int shownCount = 0;
			if (tab == HubTab.INFO) {
				label = fit == HubRules.Fit.FULL ? label : null;
			} else if (fit == HubRules.Fit.SHORT) {
				label = shortLabels[i];
			} else if (fit == HubRules.Fit.TRIMMED) {
				label = trim(font, shortLabels[i], widths[i] - PAD * 2);
			} else if (fit != HubRules.Fit.NO_COUNTS) {
				shownCount = count[i];
			}
			Text message = tab == HubTab.INFO ? labels[i] : labels[i].copy().append(" " + count[i]);
			tabs.add(new HubTabWidget(x, y, widths[i], tab, tab == context.tab(), enabled,
				tab == HubTab.INFO ? context.stack() : null, label, shownCount, message,
				tooltip(context, tab, labels[i], count[i]), () -> Hub.switchTo(context.stack(), tab)));
			x += widths[i];
		}
		return tabs;
	}

	private static String key(HubTab tab) {
		return switch (tab) {
			case INFO -> "info";
			case RECIPES -> "recipes";
			case USES -> "uses";
		};
	}

	private static Text trim(TextRenderer font, Text text, int width) {
		if (font.getWidth(text) <= width) {
			return text;
		}
		String dots = "...";
		return EmiPort.literal(font.trimToWidth(text.getString(), Math.max(0, width - font.getWidth(dots))) + dots);
	}

	/** "Recipes: what makes this · Keys: R, Left Button", then the count (or why the tab is empty). */
	private static List<Text> tooltip(Hub.Context context, HubTab tab, Text label, int count) {
		MutableText title = tab == HubTab.INFO ? label.copy()
			: EmiPort.translatable("emi_info.hub.tooltip." + key(tab));
		Text keys = keys(tab);
		if (keys != null) {
			title.append(EmiPort.literal(" · ", Formatting.DARK_GRAY)).append(keys);
		}
		Text detail;
		if (tab == HubTab.INFO) {
			detail = EmiPort.translatable("emi_info.hub.tooltip.info", context.stack().getName());
		} else if (count <= 0) {
			detail = EmiPort.translatable("emi_info.hub.tooltip." + key(tab) + ".empty");
		} else if (count == 1) {
			detail = EmiPort.translatable("emi_info.hub.tooltip.count.one");
		} else {
			detail = EmiPort.translatable("emi_info.hub.tooltip.count", count);
		}
		return List.of(title, detail.copy().formatted(Formatting.GRAY));
	}

	/** "Key: I" / "Keys: R, Left Button": every bound key of the binds that open the tab, or null. */
	private static Text keys(HubTab tab) {
		List<EmiBind> binds = new ArrayList<>();
		if (tab == HubTab.INFO) {
			binds.add(InfoSettings.openInfo);
		}
		if (tab == InfoSettings.primaryView.tab()) {
			binds.add(EmiConfig.viewRecipes);
		}
		if (tab == HubTab.USES) {
			binds.add(EmiConfig.viewUses);
		}
		MutableText keys = EmiPort.literal("");
		int n = 0;
		for (EmiBind bind : binds) {
			for (EmiBind.ModifiedKey key : bind.boundKeys) {
				if (key.isUnbound()) {
					continue;
				}
				if (n++ > 0) {
					keys.append(EmiPort.literal(", "));
				}
				keys.append(key.getKeyText(Formatting.GOLD));
			}
		}
		if (n == 0) {
			return null;
		}
		return EmiPort.translatable(n == 1 ? "emi_info.hub.tooltip.key" : "emi_info.hub.tooltip.keys", keys)
			.formatted(Formatting.GRAY);
	}

	/** EMI panel behind the category tabs, from the plate top down into the recipe panel (drawn over it). */
	public static void drawPlate(DrawContext raw, int x, int plateTop, int width, int panelTop) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, x, plateTop, width, panelTop - plateTop + 4,
			0, 0, 4, 1);
	}

	/** The tooltip of the hovered tab, if any (also for disabled tabs). */
	public static boolean renderTooltip(Screen screen, DrawContext raw, List<HubTabWidget> tabs, int mouseX, int mouseY) {
		for (HubTabWidget tab : tabs) {
			if (mouseX >= tab.getX() && mouseY >= tab.getY() && mouseX < tab.getX() + tab.getWidth()
					&& mouseY < tab.getY() + tab.getHeight()) {
				List<TooltipComponent> lines = tab.tooltip.stream().map(EmiPort::ordered).map(TooltipComponent::of).toList();
				EmiRenderHelper.drawTooltip(screen, EmiDrawContext.wrap(raw), lines, mouseX, mouseY);
				return true;
			}
		}
		return false;
	}
}
