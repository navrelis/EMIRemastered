package dev.emi.remastered.info.api;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

/**
 * Advanced hook: a self-rendering element of an {@link InfoSection}. Coordinates are relative to the element's top
 * left corner, {@code width} is the width available to it. Client render thread only.
 */
public interface InfoElement {

	/** Height for the given width; asked again whenever the screen is resized. */
	int getHeight(int width);

	/** Renders the element; the origin is translated to its top left corner. */
	void render(DrawContext draw, int width, int mouseX, int mouseY, float delta);

	/** Tooltip at the given position inside the element, empty for none. */
	default List<TooltipComponent> getTooltip(int width, int mouseX, int mouseY) {
		return List.of();
	}

	/** A mouse click inside the element; return true when handled. */
	default boolean mouseClicked(int width, int mouseX, int mouseY, int button) {
		return false;
	}

	/** A key press while the mouse is inside the element; return true when handled. */
	default boolean keyPressed(int width, int mouseX, int mouseY, int keyCode, int scanCode, int modifiers) {
		return false;
	}
}
