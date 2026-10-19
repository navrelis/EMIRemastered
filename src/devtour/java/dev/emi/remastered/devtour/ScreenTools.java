package dev.emi.remastered.devtour;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import dev.emi.emi.screen.widget.SizedButtonWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

/**
 * Helpers for the screen interaction steps (click, scroll, hover, screen_info): finding widgets, converting GUI
 * (scaled) coordinates and moving the virtual mouse. Coordinates in tour files are always GUI coordinates, the
 * same ones the screen sees in {@code render(mouseX, mouseY)}.
 */
final class ScreenTools {
	private static Field mouseX;
	private static Field mouseY;
	private static Field tooltipSupplier;

	private ScreenTools() {
	}

	/** A widget together with the EMI list it lives in (null for widgets directly on the screen). */
	record Found(ClickableWidget widget, ListWidget list) {
		/** Visible, inside the screen and, for list rows, inside the visible part of the list. */
		boolean onScreen(Screen screen) {
			ClickableWidget w = widget;
			if (!w.visible || w.getWidth() <= 0 || w.getHeight() <= 0) {
				return false;
			}
			int cx = w.getX() + w.getWidth() / 2;
			int cy = w.getY() + w.getHeight() / 2;
			if (cx < 0 || cy < 0 || cx >= screen.width || cy >= screen.height) {
				return false;
			}
			return list == null || list.isMouseOver(cx, cy);
		}
	}

	/** All clickable widgets reachable from the screen's children, recursively (lists, nested containers). */
	static List<Found> found(Screen screen) {
		List<Found> out = new ArrayList<>();
		Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Element e : screen.children()) {
			collect(e, null, out, seen);
		}
		return out;
	}

	static List<ClickableWidget> widgets(Screen screen) {
		return found(screen).stream().map(Found::widget).toList();
	}

	private static void collect(Element e, ListWidget list, List<Found> out, Set<Object> seen) {
		if (e == null || !seen.add(e)) {
			return;
		}
		ListWidget inner = e instanceof ListWidget l ? l : list;
		if (e instanceof ParentElement p) {
			for (Element child : p.children()) {
				collect(child, inner, out, seen);
			}
		}
		if (e instanceof ClickableWidget w) {
			out.add(new Found(w, list));
		}
	}

	static String textOf(ClickableWidget w) {
		try {
			return w.getMessage() == null ? "" : w.getMessage().getString();
		} catch (RuntimeException e) {
			return "";
		}
	}

	/** Translation key of the widget's message, or an empty string. */
	static String keyOf(ClickableWidget w) {
		Text message = w.getMessage();
		if (message != null) {
			TextContent content = message.getContent();
			if (content instanceof TranslatableTextContent t) {
				return t.getKey();
			}
		}
		return "";
	}

	/** Tooltip lines of an EMI sized button (the icon-only jump buttons have no message, only a tooltip). */
	@SuppressWarnings("unchecked")
	static List<String> tooltipOf(ClickableWidget w) {
		if (!(w instanceof SizedButtonWidget)) {
			return List.of();
		}
		try {
			if (tooltipSupplier == null) {
				Field f = SizedButtonWidget.class.getDeclaredField("text");
				f.setAccessible(true);
				tooltipSupplier = f;
			}
			Supplier<List<Text>> supplier = (Supplier<List<Text>>) tooltipSupplier.get(w);
			if (supplier == null) {
				return List.of();
			}
			List<String> lines = new ArrayList<>();
			for (Text t : supplier.get()) {
				lines.add(t.getString());
			}
			return lines;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return List.of();
		}
	}

	static String describe(ClickableWidget w) {
		String text = textOf(w).replace('\n', ' ');
		String key = keyOf(w);
		List<String> tip = tooltipOf(w);
		return w.getClass().getSimpleName() + (text.isEmpty() ? "" : " '" + text + "'")
			+ (key.isEmpty() ? "" : " key=" + key) + (tip.isEmpty() ? "" : " tooltip='" + String.join(" / ", tip) + "'")
			+ " @" + w.getX() + "," + w.getY() + " " + w.getWidth() + "x" + w.getHeight() + (w.visible ? "" : " hidden")
			+ (w.active ? "" : " inactive");
	}

	/** Labels shown on screen that identify the visible buttons (for error messages). */
	static String availableTexts(Screen screen) {
		List<String> texts = new ArrayList<>();
		for (Found f : found(screen)) {
			if (!f.onScreen(screen)) {
				continue;
			}
			String t = textOf(f.widget());
			if (t.isEmpty()) {
				t = String.join(" / ", tooltipOf(f.widget()));
			}
			if (!t.isEmpty()) {
				texts.add("'" + t.replace('\n', ' ') + "'");
			}
		}
		return texts.isEmpty() ? "(none)" : String.join(", ", texts);
	}

	/**
	 * Finds the nth (0-based) widget that is on screen and matches: {@code text} equals (else contains, ignoring
	 * case) the widget's message or one of its tooltip lines; {@code key} equals the translation key of its message.
	 * Exact matches come before partial ones.
	 */
	static ClickableWidget find(Screen screen, String text, String key, int index) {
		String needle = text == null ? null : text.toLowerCase(Locale.ROOT).strip();
		List<ClickableWidget> exact = new ArrayList<>();
		List<ClickableWidget> partial = new ArrayList<>();
		for (Found f : found(screen)) {
			if (!f.onScreen(screen)) {
				continue;
			}
			ClickableWidget w = f.widget();
			if (key != null) {
				if (keyOf(w).equals(key)) {
					exact.add(w);
				}
				continue;
			}
			List<String> labels = new ArrayList<>();
			labels.add(textOf(w));
			labels.addAll(tooltipOf(w));
			boolean isExact = false;
			boolean isPartial = false;
			for (String label : labels) {
				String t = label.toLowerCase(Locale.ROOT).strip();
				if (t.isEmpty()) {
					continue;
				}
				if (t.equals(needle)) {
					isExact = true;
				} else if (t.contains(needle)) {
					isPartial = true;
				}
			}
			if (isExact) {
				exact.add(w);
			} else if (isPartial) {
				partial.add(w);
			}
		}
		exact.addAll(partial);
		if (index < 0 || index >= exact.size()) {
			String what = key != null ? "translation key '" + key + "'" : "text '" + text + "'";
			throw new IllegalStateException("no clickable widget with " + what
				+ (exact.isEmpty() ? "" : " at index " + index + " (" + exact.size() + " matches)") + " on "
				+ screen.getClass().getSimpleName() + "; available: " + availableTexts(screen));
		}
		return exact.get(index);
	}

	/** The (first) EMI list widget of the screen. */
	static ListWidget findList(Screen screen) {
		for (Element e : screen.children()) {
			if (e instanceof ListWidget l) {
				return l;
			}
		}
		throw new IllegalStateException("no list on " + screen.getClass().getSimpleName());
	}

	/** Screen centre in GUI coordinates. */
	static int centreX(MinecraftClient c) {
		return c.getWindow().getScaledWidth() / 2;
	}

	static int centreY(MinecraftClient c) {
		return c.getWindow().getScaledHeight() / 2;
	}

	/**
	 * Moves the virtual mouse to the GUI coordinates: sets the position {@code GameRenderer} reads for the next
	 * frame (in window pixels) and notifies the screen. A real mouse movement over the window overrides it.
	 */
	static void moveMouse(MinecraftClient c, double guiX, double guiY) throws ReflectiveOperationException {
		Window w = c.getWindow();
		double px = guiX * w.getWidth() / w.getScaledWidth();
		double py = guiY * w.getHeight() / w.getScaledHeight();
		if (mouseX == null) {
			Field fx = Mouse.class.getDeclaredField("x");
			Field fy = Mouse.class.getDeclaredField("y");
			fx.setAccessible(true);
			fy.setAccessible(true);
			mouseX = fx;
			mouseY = fy;
		}
		mouseX.setDouble(c.mouse, px);
		mouseY.setDouble(c.mouse, py);
		if (c.currentScreen != null) {
			c.currentScreen.mouseMoved(guiX, guiY);
		}
	}

	/** Current virtual mouse position in GUI coordinates. */
	static String mouseString(MinecraftClient c) {
		Window w = c.getWindow();
		return (int) (c.mouse.getX() * w.getScaledWidth() / w.getWidth()) + ","
			+ (int) (c.mouse.getY() * w.getScaledHeight() / w.getHeight());
	}
}
