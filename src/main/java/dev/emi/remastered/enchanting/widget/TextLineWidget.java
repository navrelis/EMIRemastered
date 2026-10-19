package dev.emi.remastered.enchanting.widget;

import java.util.function.Supplier;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * One line of text that is asked for every frame, so it follows state that changes while the screen is open
 * (language, mods that disguise enchantment names). A supplier that throws draws nothing and is reported once.
 */
public class TextLineWidget extends Widget {
	private final Supplier<Text> text;
	private final int x, y, color;
	private boolean failed;

	public TextLineWidget(Supplier<Text> text, int x, int y, int color) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.color = color;
	}

	private Text current() {
		if (failed) {
			return null;
		}
		try {
			return text.get();
		} catch (Throwable t) {
			failed = true;
			EmiLog.error("EMI Enchanting: could not build a line of text", t);
			return null;
		}
	}

	@Override
	public Bounds getBounds() {
		Text current = current();
		int width = current == null ? 0 : MinecraftClient.getInstance().textRenderer.getWidth(current);
		return new Bounds(x, y, width, MinecraftClient.getInstance().textRenderer.fontHeight);
	}

	@Override
	public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
		Text current = current();
		if (current == null) {
			return;
		}
		EmiDrawContext context = EmiDrawContext.wrap(draw);
		context.push();
		context.matrices().translate(0, 0, 300);
		context.drawText(EmiPort.ordered(current), x, y, color);
		context.pop();
	}
}
