package dev.emi.remastered.world.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.remastered.world.WorldSettings;
import dev.emi.remastered.world.data.IconLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Layout pieces shared by the biome and the structure page. Client only. */
final class PageParts {
	static final int WIDTH = 176;
	static final int COLUMNS = 9;
	static final int LINE = 11;
	static final int CELL = 18;
	static final int TEXT = 0x404040;
	static final int TEXT_DARK = 0x202020;
	static final int TEXT_LIGHT = 0x6A6A6A;
	private static final int MAX_TOOLTIP_LINES = 20;
	private static final int TOOLTIP_WIDTH = 220;

	private PageParts() {
	}

	/** One icon of a section: the stack and extra tooltip lines (clicking the stack opens its pages). */
	record Icon(EmiStack stack, List<Text> tooltip) {
		Icon(EmiStack stack) {
			this(stack, List.of());
		}
	}

	/** Height of a section: the label line, then the icon rows. */
	static int sectionHeight(int icons) {
		return LINE + IconLayout.of(icons, WorldSettings.iconRows(), COLUMNS).rows() * CELL + 2;
	}

	/**
	 * Adds the label at {@code y} and the icons in rows below it; when they do not all fit the last cell says "+N" and
	 * its tooltip lists the names of the hidden ones.
	 *
	 * @return the y below the section
	 */
	static int addIcons(WidgetHolder widgets, Text label, List<Icon> icons, int y) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		addTrimmed(widgets, font, label, 0, y, WIDTH, TEXT);
		IconLayout layout = IconLayout.of(icons.size(), WorldSettings.iconRows(), COLUMNS);
		int top = y + LINE;
		for (int i = 0; i < layout.shown(); i++) {
			Icon icon = icons.get(i);
			var slot = widgets.addSlot(icon.stack(), (i % COLUMNS) * CELL, top + (i / COLUMNS) * CELL);
			for (Text line : icon.tooltip()) {
				slot.appendTooltip(line);
			}
		}
		if (layout.hidden() > 0) {
			int cell = layout.shown();
			List<Icon> hidden = icons.subList(layout.shown(), icons.size());
			Text more = Text.translatable("emi_world.more_icons", layout.hidden());
			String moreLabel = "+" + layout.hidden();
			widgets.addDrawable((cell % COLUMNS) * CELL, top + (cell / COLUMNS) * CELL, CELL, CELL,
				(gui, mouseX, mouseY, delta) -> gui.drawText(font, moreLabel, (CELL - font.getWidth(moreLabel)) / 2, 5, TEXT, false))
				.tooltip((mouseX, mouseY) -> hiddenTooltip(font, more, hidden));
		}
		return y + sectionHeight(icons.size());
	}

	private static List<TooltipComponent> hiddenTooltip(TextRenderer font, Text title, List<Icon> hidden) {
		List<TooltipComponent> lines = new ArrayList<>();
		lines.add(TooltipComponent.of(title.asOrderedText()));
		StringBuilder names = new StringBuilder();
		for (Icon icon : hidden) {
			if (!names.isEmpty()) {
				names.append(", ");
			}
			names.append(icon.stack().getName().getString());
		}
		List<OrderedText> wrapped = font.wrapLines(StringVisitable.plain(names.toString()), TOOLTIP_WIDTH);
		for (int i = 0; i < wrapped.size() && i < MAX_TOOLTIP_LINES; i++) {
			lines.add(TooltipComponent.of(wrapped.get(i)));
		}
		if (wrapped.size() > MAX_TOOLTIP_LINES) {
			lines.add(TooltipComponent.of(Text.literal("...").formatted(Formatting.GRAY).asOrderedText()));
		}
		return lines;
	}

	/** Adds the text, cut to the width with "..." and the full text as tooltip when it does not fit. */
	static void addTrimmed(WidgetHolder widgets, TextRenderer font, Text text, int x, int y, int width, int color) {
		if (font.getWidth(text) <= width) {
			widgets.addText(text, x, y, color, false);
			return;
		}
		StringVisitable trimmed = font.trimToWidth(text, width - font.getWidth("..."));
		widgets.addText(Text.literal(trimmed.getString() + "..."), x, y, color, false);
		widgets.addTooltipText(List.of(text), x, y, width, 9);
	}

	/** A line "label: value" with a tooltip, the value trimmed to the page width. */
	static void addLine(WidgetHolder widgets, TextRenderer font, Text label, Text value, int y, List<Text> tooltip) {
		Text line = Text.empty().append(label.copy().formatted(Formatting.DARK_GRAY)).append(" ").append(value);
		addTrimmed(widgets, font, line, 0, y, WIDTH, TEXT_DARK);
		if (!tooltip.isEmpty()) {
			widgets.addTooltipText(tooltip, 0, y, Math.min(WIDTH, font.getWidth(line)), LINE - 2);
		}
	}
}
