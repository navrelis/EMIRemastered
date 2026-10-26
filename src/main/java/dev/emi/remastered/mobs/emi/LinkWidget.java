package dev.emi.remastered.mobs.emi;

import java.util.List;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.remastered.theme.TextColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Underlined clickable text inside a recipe page. A disabled link is drawn grey and does nothing. */
public class LinkWidget extends Widget {
	private static final int COLOR = 0x1F3FBF;
	private static final int COLOR_HOVER = 0x0A1678;
	private static final int COLOR_DISABLED = 0x6E6E6E;

	private final Text text;
	private final int x;
	private final int y;
	private final boolean enabled;
	private final Runnable action;
	private final List<Text> tooltip;

	public LinkWidget(Text text, int x, int y, boolean enabled, Runnable action, List<Text> tooltip) {
		this.text = enabled ? text.copy().formatted(Formatting.UNDERLINE) : text;
		this.x = x;
		this.y = y;
		this.enabled = enabled;
		this.action = action;
		this.tooltip = tooltip;
	}

	@Override
	public Bounds getBounds() {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		return new Bounds(x, y, font.getWidth(text), font.fontHeight);
	}

	@Override
	public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int color = !enabled ? COLOR_DISABLED : getBounds().contains(mouseX, mouseY) ? COLOR_HOVER : COLOR;
		draw.drawText(font, TextColors.map(text), x, y, TextColors.map(color), false);
	}

	@Override
	public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
		if (tooltip.isEmpty() || !getBounds().contains(mouseX, mouseY)) {
			return List.of();
		}
		return tooltip.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (enabled && button == 0 && getBounds().contains(mouseX, mouseY)) {
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			action.run();
			return true;
		}
		return false;
	}
}
