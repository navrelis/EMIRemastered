package dev.emi.remastered.enchanting.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

/**
 * Several pages of widgets that share the space of one recipe, one page visible at a time, with previous/next
 * buttons when there is more than one page.
 * <p>
 * Pages are collected first and only added to the recipe by {@link #finish()}, so a page that fails halfway can be
 * dropped again with {@link #rollback(int)}. The widgets are added straight to the recipe, not nested in a container,
 * because EMI only finds hovered slots (recipe lookups, favorites, tooltips) among the recipe's own widgets. A widget
 * of a hidden page reports empty bounds, so it is neither drawn, hovered nor clicked. Add slots with
 * {@link WidgetHolder#addSlot}, not with {@code add(new SlotWidget(...))}, so they are page aware.
 */
public final class PagedWidgets {
	private static final int BUTTON_SIZE = 12;

	private final WidgetHolder target;
	private final List<Page> pages = new ArrayList<>();
	private int current;

	/**
	 * @param target the recipe's widget holder
	 * @param current page to show, kept by the recipe so it survives the screen being rebuilt
	 */
	public PagedWidgets(WidgetHolder target, int current) {
		this.target = target;
		this.current = current;
	}

	/** Starts the next page; add its widgets to the returned holder. */
	public WidgetHolder newPage() {
		Page page = new Page(pages.size());
		pages.add(page);
		return page;
	}

	/** @return a value for {@link #rollback(int)}: the number of pages started so far */
	public int mark() {
		return pages.size();
	}

	/** Drops every page started after {@code mark}. */
	public void rollback(int mark) {
		while (pages.size() > mark) {
			pages.remove(pages.size() - 1);
		}
	}

	public int getPageCount() {
		return pages.size();
	}

	/** @return the page that is shown, always a valid index while there are pages */
	public int getCurrent() {
		return pages.isEmpty() ? 0 : Math.floorMod(current, pages.size());
	}

	/** Turns by the given number of pages, wrapping around. */
	public void turn(int delta) {
		if (!pages.isEmpty()) {
			current = Math.floorMod(getCurrent() + delta, pages.size());
		}
	}

	/** Adds all collected pages, and the buttons to turn them, to the recipe. */
	public void finish() {
		for (Page page : pages) {
			if (pages.size() > 1) {
				int y = target.getHeight() - BUTTON_SIZE;
				page.addButton(0, y, BUTTON_SIZE, BUTTON_SIZE, 0, 0, () -> true, (mouseX, mouseY, button) -> turn(-1));
				page.addButton(target.getWidth() - BUTTON_SIZE, y, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, 0, () -> true,
					(mouseX, mouseY, button) -> turn(1));
			}
			page.widgets.forEach(target::add);
		}
	}

	private class Page implements WidgetHolder {
		private final List<Widget> widgets = new ArrayList<>();
		private final BooleanSupplier visible;

		private Page(int index) {
			this.visible = () -> getCurrent() == index;
		}

		@Override
		public int getWidth() {
			return target.getWidth();
		}

		@Override
		public int getHeight() {
			return target.getHeight();
		}

		@Override
		public <T extends Widget> T add(T widget) {
			widgets.add(new GatedWidget(widget, visible));
			return widget;
		}

		@Override
		public SlotWidget addSlot(EmiIngredient ingredient, int x, int y) {
			GatedSlotWidget slot = new GatedSlotWidget(ingredient, x, y, visible);
			widgets.add(slot);
			return slot;
		}
	}

	private static final class GatedWidget extends Widget {
		private final Widget widget;
		private final BooleanSupplier visible;

		private GatedWidget(Widget widget, BooleanSupplier visible) {
			this.widget = widget;
			this.visible = visible;
		}

		@Override
		public Bounds getBounds() {
			return visible.getAsBoolean() ? widget.getBounds() : Bounds.EMPTY;
		}

		@Override
		public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
			if (visible.getAsBoolean()) {
				widget.render(draw, mouseX, mouseY, delta);
			}
		}

		@Override
		public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
			return visible.getAsBoolean() ? widget.getTooltip(mouseX, mouseY) : List.of();
		}

		@Override
		public boolean mouseClicked(int mouseX, int mouseY, int button) {
			return visible.getAsBoolean() && widget.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return visible.getAsBoolean() && widget.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	private static final class GatedSlotWidget extends SlotWidget {
		private final BooleanSupplier visible;

		private GatedSlotWidget(EmiIngredient stack, int x, int y, BooleanSupplier visible) {
			super(stack, x, y);
			this.visible = visible;
		}

		@Override
		public Bounds getBounds() {
			return visible.getAsBoolean() ? super.getBounds() : Bounds.EMPTY;
		}

		@Override
		public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
			if (visible.getAsBoolean()) {
				super.render(draw, mouseX, mouseY, delta);
			}
		}
	}
}
