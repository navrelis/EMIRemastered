package dev.emi.remastered.info.client.hub;

import java.util.List;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.remastered.theme.TextColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * One tab of the item view bar, drawn with EMI's tab sprites like its category tabs: the selected tab is raised and
 * merges into the panel below it. The widget's area is the visible tab; the sprite reaches {@code OVERLAP} pixels into
 * the panel border.
 */
public final class HubTabWidget extends ClickableWidget {
	static final int OVERLAP = 3;
	private static final int TEXT = 0x404040;
	private static final int TEXT_DISABLED = 0x8B8B8B;
	private static final int HOVER_FILL = 0x50FFFFFF;
	/** Counts use the Item Info page's muted grey (its "x20" counts), readable on both tab sprites. */
	private static final int COUNT = 0x707070;
	final HubTab tab;
	final boolean selected;
	final List<Text> tooltip;
	private final EmiStack icon;
	private final Text label;
	private final int count;
	private final Runnable action;

	HubTabWidget(int x, int y, int width, HubTab tab, boolean selected, boolean enabled, EmiStack icon, Text label,
			int count, Text message, List<Text> tooltip, Runnable action) {
		super(x, y - (selected ? HubRules.SELECTED_RAISE : 0), width,
			HubRules.TAB_HEIGHT + (selected ? HubRules.SELECTED_RAISE : 0), message);
		this.tab = tab;
		this.selected = selected;
		this.active = enabled;
		this.icon = icon;
		this.label = label;
		this.count = count;
		this.tooltip = tooltip;
		this.action = action;
	}

	@Override
	protected void renderWidget(DrawContext raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, getX(), getY(), width, height + OVERLAP,
			selected ? 9 : 18, 0, 4, 1);
		if (!selected && active && isHovered()) {
			raw.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height, HOVER_FILL);
		}
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int x = getX() + HubBar.PAD;
		int top = getY();
		if (icon != null) {
			x = getX() + HubBar.ICON_PAD;
			icon.render(raw, x, top + 2, delta, EmiIngredient.RENDER_ICON);
			x += 16 + HubBar.ICON_GAP;
		}
		int textY = top + (HubRules.TAB_HEIGHT - 8) / 2;
		if (label != null) {
			raw.drawText(font, TextColors.map(label), x, textY, TextColors.map(active ? TEXT : TEXT_DISABLED), false);
			x += font.getWidth(label);
		}
		if (count > 0) {
			raw.drawText(font, String.valueOf(count), x + HubBar.COUNT_GAP, textY, TextColors.map(COUNT), false);
		}
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		if (!selected) {
			action.run();
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
