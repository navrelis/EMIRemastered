package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.remastered.info.api.InfoElement;
import dev.emi.remastered.info.api.InfoLink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

/** Built-in elements of the Item Info screen. Client render thread only. */
public final class Elements {
	public static final int TEXT = 0x404040;
	public static final int MUTED = 0x707070;
	public static final int LINK = 0x1E4BB0;
	public static final int LINK_HOVER = 0x2F6BFF;
	public static final int HOVER_FILL = 0x50FFFFFF;
	public static final int LINE_HEIGHT = 10;
	/** Gap between right-aligned texts and the edge of the scroll area. */
	static final int RIGHT_PAD = 3;

	private Elements() {
	}

	static TextRenderer font() {
		return MinecraftClient.getInstance().textRenderer;
	}

	static void click() {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	static boolean open(@Nullable InfoLink link) {
		if (link == null) {
			return false;
		}
		click();
		link.open();
		return true;
	}

	static List<TooltipComponent> tooltip(List<Text> lines) {
		List<TooltipComponent> list = new ArrayList<>(lines.size());
		for (Text line : lines) {
			list.add(TooltipComponent.of(EmiPort.ordered(line)));
		}
		return list;
	}

	/** {@code text} cut to {@code width} with "..." when too long. */
	static OrderedText trim(Text text, int width) {
		TextRenderer font = font();
		if (font.getWidth(text) <= width) {
			return text.asOrderedText();
		}
		int dots = font.getWidth("...");
		StringVisitable cut = font.trimToWidth(text, Math.max(0, width - dots));
		return Language.getInstance().reorder(StringVisitable.concat(cut, StringVisitable.plain("...")));
	}

	static boolean inside(int mouseX, int mouseY, int width, int height) {
		return mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height;
	}

	/** Slots laid out in rows from a given origin. */
	static final class SlotGrid {
		final List<SlotWidget> slots = new ArrayList<>();

		void clear() {
			slots.clear();
		}

		void add(EmiIngredient stack, int x, int y) {
			slots.add(new SlotWidget(stack, x, y));
		}

		@Nullable SlotWidget at(int mouseX, int mouseY) {
			for (SlotWidget slot : slots) {
				if (slot.getBounds().contains(mouseX, mouseY)) {
					return slot;
				}
			}
			return null;
		}

		void render(DrawContext draw, int mouseX, int mouseY, float delta) {
			for (SlotWidget slot : slots) {
				slot.render(draw, mouseX, mouseY, delta);
			}
		}

		List<TooltipComponent> tooltip(int mouseX, int mouseY) {
			SlotWidget slot = at(mouseX, mouseY);
			return slot == null ? List.of() : slot.getTooltip(mouseX, mouseY);
		}

		boolean mouseClicked(int mouseX, int mouseY, int button) {
			SlotWidget slot = at(mouseX, mouseY);
			return slot != null && slot.mouseClicked(mouseX, mouseY, button);
		}

		boolean keyPressed(int mouseX, int mouseY, int keyCode, int scanCode, int modifiers) {
			SlotWidget slot = at(mouseX, mouseY);
			return slot != null && slot.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	/** A wrapped paragraph, optionally a link. */
	public static final class TextElement implements InfoElement {
		private final Text text;
		private final List<Text> tooltip;
		private final @Nullable InfoLink link;
		private int lastWidth = -1;
		private List<OrderedText> lines = List.of();
		private List<OrderedText> hoverLines = List.of();

		public TextElement(Text text, List<Text> tooltip, @Nullable InfoLink link) {
			this.text = text;
			this.tooltip = tooltip;
			this.link = link;
		}

		private void layout(int width) {
			if (width != lastWidth) {
				lastWidth = width;
				lines = font().wrapLines(text, Math.max(16, width));
				if (link != null) {
					hoverLines = font().wrapLines(text.copy().styled(s -> s.withUnderline(true)), Math.max(16, width));
				}
			}
		}

		@Override
		public int getHeight(int width) {
			layout(width);
			return Math.max(1, lines.size()) * LINE_HEIGHT;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			layout(width);
			boolean hovered = link != null && inside(mouseX, mouseY, width, getHeight(width));
			List<OrderedText> list = hovered ? hoverLines : lines;
			int color = link == null ? TEXT : hovered ? LINK_HOVER : LINK;
			for (int i = 0; i < list.size(); i++) {
				draw.drawText(font(), list.get(i), 0, i * LINE_HEIGHT + 1, color, false);
			}
		}

		@Override
		public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
			return tooltip(tooltip);
		}

		@Override
		public boolean mouseClicked(int width, int mouseX, int mouseY, int button) {
			return button == 0 && open(link);
		}
	}

	/** Rows of slots with an optional label line. */
	public static final class StacksElement implements InfoElement {
		private final @Nullable Text label;
		private final List<EmiIngredient> stacks;
		private final SlotGrid grid = new SlotGrid();
		private int lastWidth = -1;
		private int height;

		public StacksElement(@Nullable Text label, List<EmiIngredient> stacks) {
			this.label = label;
			this.stacks = stacks;
		}

		private void layout(int width) {
			if (width == lastWidth) {
				return;
			}
			lastWidth = width;
			grid.clear();
			int top = label == null ? 0 : LINE_HEIGHT + 1;
			int perRow = Math.max(1, width / 18);
			for (int i = 0; i < stacks.size(); i++) {
				grid.add(stacks.get(i), (i % perRow) * 18, top + (i / perRow) * 18);
			}
			height = top + (stacks.size() + perRow - 1) / perRow * 18;
		}

		@Override
		public int getHeight(int width) {
			layout(width);
			return height;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			layout(width);
			if (label != null) {
				draw.drawText(font(), trim(label, width), 0, 1, TEXT, false);
			}
			grid.render(draw, mouseX, mouseY, delta);
		}

		@Override
		public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
			return grid.tooltip(mouseX, mouseY);
		}

		@Override
		public boolean mouseClicked(int width, int mouseX, int mouseY, int button) {
			return grid.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		public boolean keyPressed(int width, int mouseX, int mouseY, int keyCode, int scanCode, int modifiers) {
			return grid.keyPressed(mouseX, mouseY, keyCode, scanCode, modifiers);
		}
	}

	/** Label, bar, value. */
	public static final class BarElement implements InfoElement {
		private final Text label;
		private final float progress;
		private final int color;
		private final @Nullable Text value;

		public BarElement(Text label, float progress, int color, @Nullable Text value) {
			this.label = label;
			this.progress = Float.isNaN(progress) ? 0 : MathHelper.clamp(progress, 0, 1);
			this.color = color;
			this.value = value;
		}

		@Override
		public int getHeight(int width) {
			return LINE_HEIGHT + 1;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			TextRenderer font = font();
			int valueWidth = value == null ? 0 : font.getWidth(value) + 4 + RIGHT_PAD;
			int labelWidth = Math.min(font.getWidth(label), width * 2 / 5);
			draw.drawText(font, trim(label, labelWidth), 0, 2, TEXT, false);
			int barX = labelWidth + 4;
			int barW = width - barX - valueWidth;
			if (barW > 4) {
				draw.fill(barX, 2, barX + barW, 9, 0xFF373737);
				draw.fill(barX + 1, 3, barX + barW - 1, 8, 0xFF8B8B8B);
				int fill = Math.round((barW - 2) * progress);
				if (fill > 0) {
					draw.fill(barX + 1, 3, barX + 1 + fill, 8, 0xFF000000 | color);
				}
			}
			if (value != null) {
				draw.drawText(font, value, width - valueWidth + 4, 2, TEXT, false);
			}
		}
	}

	/** Heading of a section kind: EMI's inset bar with centred white text. */
	static final class HeadingElement implements InfoElement {
		private final Text text;

		HeadingElement(Text text) {
			this.text = text;
		}

		@Override
		public int getHeight(int width) {
			return 14;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			EmiDrawContext context = EmiDrawContext.wrap(draw);
			context.resetColor();
			EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, 0, 1, width, 12, 0, 16, 3, 6);
			OrderedText line = trim(text, width - 6);
			draw.drawTextWithShadow(font(), line, (width - font().getWidth(line)) / 2, 3, 0xFFFFFF);
		}
	}

	/** Title line of a section: icon and text. */
	static final class TitleElement implements InfoElement {
		private final @Nullable EmiRenderable icon;
		private final Text title;

		TitleElement(@Nullable EmiRenderable icon, Text title) {
			this.icon = icon;
			this.title = title;
		}

		@Override
		public int getHeight(int width) {
			return 18;
		}

		@Override
		public void render(DrawContext draw, int width, int mouseX, int mouseY, float delta) {
			int x = 0;
			if (icon != null) {
				icon.render(draw, 0, 1, delta);
				x = 20;
			}
			draw.drawText(font(), trim(title, width - x), x, 5, TEXT, false);
		}

		@Override
		public List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
			int x = icon == null ? 0 : 20;
			if (font().getWidth(title) > width - x) {
				return tooltip(List.of(title));
			}
			return List.of();
		}
	}
}
