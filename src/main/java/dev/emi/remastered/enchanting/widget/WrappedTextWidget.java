package dev.emi.remastered.enchanting.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Text wrapped to a width and cut off after a number of lines. Wrapping needs the font, so it happens on the first
 * frame that shows the text (never while EMI is reloading, which can overlap a resource reload) and again if the text
 * changes.
 */
public class WrappedTextWidget extends Widget {
	private final Supplier<Text> text;
	private final int x, y, width, maxLines, lineHeight, color;
	private String cachedKey;
	private List<OrderedText> lines = List.of();
	private boolean failed;

	public WrappedTextWidget(Supplier<Text> text, int x, int y, int width, int maxLines, int lineHeight, int color) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.width = width;
		this.maxLines = maxLines;
		this.lineHeight = lineHeight;
		this.color = color;
	}

	private List<OrderedText> lines() {
		if (failed) {
			return List.of();
		}
		try {
			Text current = text.get();
			boolean obfuscated = current.visit((style, string) -> style.isObfuscated() ? Optional.of(true) : Optional.empty(),
				Style.EMPTY).isPresent();
			String key = obfuscated + current.getString();
			if (!key.equals(cachedKey)) {
				List<OrderedText> wrapped = MinecraftClient.getInstance().textRenderer.wrapLines(current, width);
				if (wrapped.size() > maxLines) {
					wrapped = new ArrayList<>(wrapped.subList(0, maxLines));
					wrapped.set(maxLines - 1, OrderedText.concat(wrapped.get(maxLines - 1), Text.literal("...").asOrderedText()));
				}
				lines = wrapped;
				cachedKey = key;
			}
			return lines;
		} catch (Throwable t) {
			failed = true;
			EmiLog.error("EMI Enchanting: could not wrap a block of text", t);
			return List.of();
		}
	}

	@Override
	public Bounds getBounds() {
		return new Bounds(x, y, width, Math.max(1, lines().size()) * lineHeight);
	}

	@Override
	public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(draw);
		context.push();
		context.matrices().translate(0, 0, 300);
		int lineY = y;
		for (OrderedText line : lines()) {
			context.drawText(line, x, lineY, color);
			lineY += lineHeight;
		}
		context.pop();
	}
}
