package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.api.InfoElement;
import dev.emi.remastered.info.api.InfoEntry;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.theme.TextColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Entry row: icon, label, example slots, detail; clicking the row opens its link. */
final class EntryImpl implements InfoEntry, InfoElement {
	private static final int HEIGHT = 18;
	/** Gap between the right-aligned detail / example slots and the edge of the scroll area. */
	private static final int RIGHT_PAD = 3;
	private final EmiRenderable icon;
	private final Text label;
	private @Nullable Text detail;
	private List<EmiIngredient> stacks = List.of();
	private @Nullable InfoLink link;
	private List<Text> tooltip = List.of();
	private final Elements.SlotGrid grid = new Elements.SlotGrid();
	private int lastWidth = -1, lastMax = -1;
	private int labelWidth;

	EntryImpl(EmiRenderable icon, Text label) {
		this.icon = icon;
		this.label = label;
	}

	@Override
	public InfoEntry detail(@Nullable Text detail) {
		this.detail = detail;
		lastWidth = -1;
		return this;
	}

	@Override
	public InfoEntry count(long count) {
		return detail(EmiPort.translatable("emi_info.count", count));
	}

	@Override
	public InfoEntry stacks(List<? extends EmiIngredient> stacks) {
		this.stacks = List.copyOf(stacks);
		lastWidth = -1;
		return this;
	}

	@Override
	public InfoEntry link(@Nullable InfoLink link) {
		this.link = link;
		return this;
	}

	@Override
	public InfoEntry tooltip(List<Text> tooltip) {
		this.tooltip = List.copyOf(tooltip);
		return this;
	}

	private void layout(int width) {
		int max = Math.max(0, InfoSettings.exampleStacks);
		if (width == lastWidth && max == lastMax) {
			return;
		}
		lastWidth = width;
		lastMax = max;
		TextRenderer font = Elements.font();
		int right = width - RIGHT_PAD - (detail == null ? 0 : font.getWidth(detail) + 4);
		int labelMin = Math.min(font.getWidth(label), 64);
		int fit = Math.max(0, (right - 20 - labelMin - 4) / 18);
		int n = Math.min(Math.min(max, fit), stacks.size());
		int slotsX = right - n * 18;
		grid.clear();
		for (int i = 0; i < n; i++) {
			grid.add(stacks.get(i), slotsX + i * 18, 0);
		}
		labelWidth = (n > 0 ? slotsX - 4 : right) - 20;
	}

	@Override
	public int getHeight(int width) {
		return HEIGHT;
	}

	@Override
	public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
		layout(width);
		if (link != null && Elements.inside(mouseX, mouseY, width, HEIGHT) && grid.at(mouseX, mouseY) == null) {
			draw.fill(0, 0, width, HEIGHT, Elements.HOVER_FILL);
		}
		icon.render(draw, 1, 1, delta);
		draw.drawText(Elements.font(), TextColors.map(Elements.trim(label, labelWidth)), 20, 5, TextColors.map(Elements.TEXT), false);
		if (detail != null) {
			draw.drawText(Elements.font(), TextColors.map(detail), width - RIGHT_PAD - Elements.font().getWidth(detail), 5, TextColors.map(Elements.MUTED), false);
		}
		grid.render(draw, mouseX, mouseY, delta);
	}

	@Override
	public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
		layout(width);
		if (grid.at(mouseX, mouseY) != null) {
			return grid.tooltip(mouseX, mouseY);
		}
		List<Text> lines = new ArrayList<>();
		lines.add(label);
		lines.addAll(tooltip);
		if (link != null) {
			lines.add(EmiPort.translatable("emi_info.tooltip.open", Formatting.DARK_GRAY));
		}
		return Elements.tooltip(lines);
	}

	@Override
	public boolean mouseClicked(int width, int mouseX, int mouseY, int button) {
		layout(width);
		if (grid.at(mouseX, mouseY) != null) {
			return grid.mouseClicked(mouseX, mouseY, button);
		}
		return button == 0 && Elements.open(link);
	}

	@Override
	public boolean keyPressed(int width, int mouseX, int mouseY, int keyCode, int scanCode, int modifiers) {
		layout(width);
		return grid.keyPressed(mouseX, mouseY, keyCode, scanCode, modifiers);
	}
}
