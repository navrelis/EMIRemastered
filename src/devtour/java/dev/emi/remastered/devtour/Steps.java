package dev.emi.remastered.devtour;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonElement;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.ConfigScreen;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.RecipeTab;
import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import dev.emi.emi.search.EmiSearch;
import dev.emi.remastered.config.screen.PluginManagerScreen;
import dev.emi.remastered.plugin.PluginManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;

/**
 * Registry of all step types.
 *
 * <p>To add a step type, add one {@code register("my_step", (args, ctx) -> ...)} line in the static block. Return
 * {@link StepRun#DONE} when the work was instant, {@link #waitTicks(int)} to let the game run a few ticks
 * afterwards, or your own {@link StepRun} for anything that needs several ticks. Throw an exception (any
 * message) to fail the step. {@code ctx.info("...")} adds a note to the step in summary.json. Document the
 * step in docs/DEVTOUR.md.
 */
public final class Steps {
	private static final String PLUGINS_BUTTON_KEY = "screen.emi_remastered.plugins.button";
	private static final Map<String, StepHandler> HANDLERS = new LinkedHashMap<>();

	private Steps() {
	}

	public static StepHandler get(String type) {
		return HANDLERS.get(type);
	}

	public static java.util.Set<String> types() {
		return HANDLERS.keySet();
	}

	static void register(String type, StepHandler handler) {
		HANDLERS.put(type, handler);
	}

	// ---------------------------------------------------------------- helpers

	/** A step run that finishes after the given number of ticks. */
	public static StepRun waitTicks(int ticks) {
		int[] left = {ticks};
		return () -> --left[0] <= 0;
	}

	private static void requireWorld(TourContext ctx) {
		if (ctx.client.world == null || ctx.client.player == null) {
			throw new IllegalStateException("not in a world");
		}
	}

	private static String screenName(Screen s) {
		return s == null ? "none" : s.getClass().getSimpleName();
	}

	private static Item resolveItem(String id) {
		Identifier ident = Identifier.tryParse(id);
		if (ident == null) {
			throw new IllegalArgumentException("invalid item id '" + id + "'");
		}
		return Registries.ITEM.getOrEmpty(ident)
			.orElseThrow(() -> new IllegalArgumentException("unknown item '" + id + "'"));
	}

	/** Lists the tabs of a recipe screen (category id and page count). Reflection: the field is private. */
	private static String describeTabs(RecipeScreen rs) {
		try {
			Field f = RecipeScreen.class.getDeclaredField("tabs");
			f.setAccessible(true);
			List<?> tabs = (List<?>) f.get(rs);
			StringBuilder sb = new StringBuilder("tabs:");
			for (Object o : tabs) {
				RecipeTab t = (RecipeTab) o;
				sb.append(' ').append(t.category.getId()).append('(').append(t.getPageCount()).append("p)");
			}
			return sb.toString();
		} catch (ReflectiveOperationException e) {
			return "tabs: unavailable (" + e + ")";
		}
	}

	private static List<?> tabsOf(RecipeScreen rs) throws ReflectiveOperationException {
		Field f = RecipeScreen.class.getDeclaredField("tabs");
		f.setAccessible(true);
		return (List<?>) f.get(rs);
	}

	/** GLFW key by name: "R", "F3", "LEFT_CONTROL", "KEY_R", "GLFW_KEY_R" or a raw integer key code. */
	static int resolveKey(String name) {
		String n = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
		try {
			return Integer.parseInt(n);
		} catch (NumberFormatException ignored) {
			// not a number
		}
		if (n.startsWith("GLFW_KEY_")) {
			n = n.substring("GLFW_KEY_".length());
		} else if (n.startsWith("KEY_")) {
			n = n.substring("KEY_".length());
		}
		try {
			return GLFW.class.getField("GLFW_KEY_" + n).getInt(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("unknown GLFW key '" + name + "'");
		}
	}

	private static int resolveModifiers(Args args) {
		int mods = 0;
		if (args.json().has("modifiers")) {
			for (JsonElement e : args.json().getAsJsonArray("modifiers")) {
				switch (e.getAsString().toLowerCase(Locale.ROOT)) {
					case "shift" -> mods |= GLFW.GLFW_MOD_SHIFT;
					case "ctrl", "control" -> mods |= GLFW.GLFW_MOD_CONTROL;
					case "alt" -> mods |= GLFW.GLFW_MOD_ALT;
					case "super" -> mods |= GLFW.GLFW_MOD_SUPER;
					default -> throw new IllegalArgumentException("unknown modifier '" + e.getAsString() + "'");
				}
			}
		}
		return mods;
	}

	// ------------------------------------------------------------------ steps

	static {
		// {"type":"wait","ticks":20}
		register("wait", (args, ctx) -> waitTicks(args.intVal("ticks", 20)));

		// {"type":"screenshot","name":"inventory"}  optional "settleTicks" overrides the plan default.
		// Waits for the settle ticks and at least 3 rendered frames, then saves <NN>-<name>.png.
		register("screenshot", (args, ctx) -> {
			String name = args.str("name", "shot");
			int settle = args.intVal("settleTicks", ctx.plan.settleTicks);
			long startFrames = ctx.frames();
			int[] ticks = {0};
			return () -> {
				ticks[0]++;
				if (ticks[0] < settle || ctx.frames() - startFrames < 3) {
					return false;
				}
				ctx.screenshot(name);
				return true;
			};
		});

		// {"type":"open_inventory"}
		register("open_inventory", (args, ctx) -> {
			requireWorld(ctx);
			ctx.client.setScreen(new InventoryScreen(ctx.client.player));
			return waitTicks(2);
		});

		// {"type":"close_screen"}
		register("close_screen", (args, ctx) -> {
			MinecraftClient c = ctx.client;
			if (c.player != null && c.currentScreen != null) {
				c.player.closeHandledScreen();
			}
			c.setScreen(null);
			return waitTicks(2);
		});

		// {"type":"emi_search","text":"iron"}   (needs an open screen that shows EMI, e.g. open_inventory first)
		register("emi_search", (args, ctx) -> {
			if (ctx.client.currentScreen == null) {
				throw new IllegalStateException("no screen open; use open_inventory first");
			}
			EmiApi.setSearchText(args.str("text", ""));
			int[] ticks = {0};
			return () -> {
				// the search runs on a worker thread; wait for it to finish (max ~2 s)
				Thread t = EmiSearch.searchThread;
				boolean running = t != null && t.isAlive();
				if (running && ++ticks[0] < 40) {
					return false;
				}
				ctx.info("search '" + EmiApi.getSearchText() + "': " + EmiSearch.stacks.size() + " results");
				return true;
			};
		});

		// {"type":"emi_recipes","item":"minecraft:iron_pickaxe"}  (EmiApi.displayRecipes)
		register("emi_recipes", (args, ctx) -> {
			requireWorld(ctx);
			String id = args.str("item");
			Screen before = ctx.client.currentScreen;
			EmiApi.displayRecipes(EmiStack.of(resolveItem(id)));
			return afterRecipeScreen(ctx, before, "recipes", id);
		});

		// {"type":"emi_uses","item":"minecraft:iron_ingot"}  (EmiApi.displayUses)
		register("emi_uses", (args, ctx) -> {
			requireWorld(ctx);
			String id = args.str("item");
			Screen before = ctx.client.currentScreen;
			EmiApi.displayUses(EmiStack.of(resolveItem(id)));
			return afterRecipeScreen(ctx, before, "uses", id);
		});

		// {"type":"emi_category","id":"emi:fuel"}  switches the open recipe screen to that category's tab
		register("emi_category", (args, ctx) -> {
			RecipeScreen rs = requireRecipeScreen(ctx);
			String id = args.str("id");
			EmiRecipeCategory found = null;
			for (Object o : tabsOf(rs)) {
				RecipeTab t = (RecipeTab) o;
				if (t.category.getId().toString().equals(id)) {
					found = t.category;
				}
			}
			if (found == null) {
				throw new IllegalStateException("category '" + id + "' is not on this recipe screen; " + describeTabs(rs));
			}
			rs.focusCategory(found);
			ctx.info("focused " + id);
			return waitTicks(2);
		});

		// {"type":"emi_recipe_tab","index":1,"page":0}  tab/page of the open recipe screen, 0-based
		register("emi_recipe_tab", (args, ctx) -> {
			RecipeScreen rs = requireRecipeScreen(ctx);
			List<?> tabs = tabsOf(rs);
			int index = args.intVal("index");
			if (index < 0 || index >= tabs.size()) {
				throw new IllegalStateException("tab index " + index + " out of range; " + describeTabs(rs));
			}
			Field tp = RecipeScreen.class.getDeclaredField("tabPage");
			tp.setAccessible(true);
			rs.setPage(tp.getInt(rs), index, args.intVal("page", 0));
			ctx.info("tab " + index + ": " + ((RecipeTab) tabs.get(index)).category.getId());
			return waitTicks(2);
		});

		// {"type":"emi_bom"}  opens the recipe tree (bill of materials) screen
		register("emi_bom", (args, ctx) -> {
			requireWorld(ctx);
			EmiApi.viewRecipeTree();
			return waitTicks(2);
		});

		// {"type":"open_emi_config"}
		register("open_emi_config", (args, ctx) -> {
			ctx.client.setScreen(new ConfigScreen(ctx.client.currentScreen));
			return waitTicks(2);
		});

		// {"type":"key","key":"R","modifiers":["shift"]}  presses and releases a key through the game's keyboard
		// handler, so key bindings and open screens react as to a real key press.
		register("key", (args, ctx) -> {
			int key = resolveKey(args.str("key"));
			int mods = resolveModifiers(args);
			long window = ctx.client.getWindow().getHandle();
			int scancode = GLFW.glfwGetKeyScancode(key);
			ctx.client.keyboard.onKey(window, key, scancode, GLFW.GLFW_PRESS, mods);
			boolean[] released = {false};
			return () -> {
				if (!released[0]) {
					released[0] = true;
					ctx.client.keyboard.onKey(window, key, scancode, GLFW.GLFW_RELEASE, mods);
					return false;
				}
				return true;
			};
		});

		// {"type":"command","text":"/time set day"}  runs a command on the integrated server as the server (op 4)
		register("command", (args, ctx) -> {
			IntegratedServer server = ctx.client.getServer();
			if (server == null) {
				throw new IllegalStateException("no integrated server");
			}
			String text = args.str("text").strip();
			String cmd = text.startsWith("/") ? text.substring(1) : text;
			server.execute(() -> server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd));
			return waitTicks(args.intVal("waitTicks", 5));
		});

		// {"type":"set_gui_scale","n":3}  0 = auto
		register("set_gui_scale", (args, ctx) -> {
			int n = args.intVal("n");
			ctx.client.options.getGuiScale().setValue(n);
			ctx.client.onResolutionChanged();
			ctx.info("gui scale " + n + " -> scaled " + ctx.client.getWindow().getScaledWidth() + "x"
				+ ctx.client.getWindow().getScaledHeight());
			return waitTicks(3);
		});

		// {"type":"resize","w":1280,"h":720}  resizes the game window (windowed mode)
		register("resize", (args, ctx) -> {
			int w = args.intVal("w");
			int h = args.intVal("h");
			GLFW.glfwSetWindowSize(ctx.client.getWindow().getHandle(), w, h);
			int[] ticks = {0};
			return () -> {
				if (++ticks[0] < 6) {
					return false;
				}
				ctx.info("window now " + ctx.client.getWindow().getWidth() + "x" + ctx.client.getWindow().getHeight()
					+ " (scaled " + ctx.client.getWindow().getScaledWidth() + "x" + ctx.client.getWindow().getScaledHeight() + ")");
				return true;
			};
		});

		// {"type":"note","text":"..."}  puts a note into summary.json
		register("note", (args, ctx) -> {
			ctx.info(args.str("text", ""));
			return StepRun.DONE;
		});

		// {"type":"emi_wait_loaded"}  waits until EMI is loaded again (e.g. after a command that triggers a reload)
		register("emi_wait_loaded", (args, ctx) -> () -> EmiReloadManager.isLoaded());

		// {"type":"click","text":"Plugins"}  presses the widget on screen whose message (or tooltip line, for the
		// icon-only jump buttons) equals, else contains, the text, ignoring case.
		// {"type":"click","key":"screen.emi_remastered.plugins.button"}  same by translation key of the message.
		// {"type":"click","x":400,"y":120}  clicks at GUI coordinates.
		// Optional "index" picks the nth match; "button" is the mouse button (0 left, 1 right); "optional": true skips
		// the step instead of failing when nothing matches. The click goes through
		// Screen.mouseClicked at the widget centre (overlapping widgets behave as for a real click); a miss fails the
		// step with the list of button texts on screen.
		register("click", (args, ctx) -> {
			Screen screen = requireScreen(ctx);
			int button = args.intVal("button", 0);
			if (args.has("text") || args.has("key")) {
				ClickableWidget w;
				try {
					w = ScreenTools.find(screen, args.str("text", null), args.str("key", null), args.intVal("index", 0));
				} catch (IllegalStateException e) {
					if (args.bool("optional", false)) {
						ctx.info("skipped, no match (optional)");
						return StepRun.DONE;
					}
					throw e;
				}
				return beginClick(ctx, w, w.getX() + w.getWidth() / 2, w.getY() + w.getHeight() / 2, button);
			}
			return beginClick(ctx, null, args.intVal("x"), args.intVal("y"), button);
		});

		// {"type":"scroll","amount":-5,"x":640,"y":300}  mouse wheel on the current screen at GUI coordinates
		// (default: screen centre). Negative amount = wheel down (content moves up), positive = wheel up.
		register("scroll", (args, ctx) -> {
			Screen screen = requireScreen(ctx);
			double amount = args.json().get("amount").getAsDouble();
			int x = args.intVal("x", ScreenTools.centreX(ctx.client));
			int y = args.intVal("y", ScreenTools.centreY(ctx.client));
			ScreenTools.moveMouse(ctx.client, x, y);
			boolean handled = screen.mouseScrolled(x, y, 0, amount);
			ctx.info("scrolled " + amount + " at " + x + "," + y + (handled ? "" : " (not handled)"));
			return waitTicks(2);
		});

		// {"type":"emi_config_jump","group":"modules"}  ConfigScreen.jump(group): scrolls the list to that group
		register("emi_config_jump", (args, ctx) -> {
			if (!(ctx.client.currentScreen instanceof ConfigScreen cs)) {
				throw new IllegalStateException("current screen is " + screenName(ctx.client.currentScreen)
					+ ", not EMI's ConfigScreen (use open_emi_config first)");
			}
			String group = args.str("group");
			double before = cs.list.getScrollAmount();
			cs.jump(group);
			double after = cs.list.getScrollAmount();
			ctx.info("jump " + group + ": scroll " + (int) before + " -> " + (int) after);
			return waitTicks(2);
		});

		// {"type":"open_plugin_manager"}  opens EMI's config screen (unless it is open) and presses "Plugins"
		register("open_plugin_manager", (args, ctx) -> {
			if (!(ctx.client.currentScreen instanceof ConfigScreen)) {
				ctx.client.setScreen(new ConfigScreen(ctx.client.currentScreen));
			}
			int[] ticks = {0};
			StepRun[] click = {null};
			return () -> {
				if (click[0] == null) {
					if (++ticks[0] < 3) {
						return false;
					}
					ClickableWidget w = ScreenTools.find(requireScreen(ctx), null, PLUGINS_BUTTON_KEY, 0);
					click[0] = beginClick(ctx, w, w.getX() + w.getWidth() / 2, w.getY() + w.getHeight() / 2, 0);
				}
				if (!click[0].tick()) {
					return false;
				}
				if (!(ctx.client.currentScreen instanceof PluginManagerScreen)) {
					throw new IllegalStateException("the Plugins button did not open the Plugin Manager, screen is "
						+ screenName(ctx.client.currentScreen));
				}
				return true;
			};
		});

		// {"type":"plugin_toggle","id":"emi_loot","state":"off"}  in the Plugin Manager: scrolls the row of that
		// plugin id into view and presses its On/Off button. "state" (on|off) makes the step idempotent (clicks only
		// when the plugin is not in that state yet); without it the button is pressed once. Notes the new state.
		register("plugin_toggle", (args, ctx) -> {
			if (!(ctx.client.currentScreen instanceof PluginManagerScreen pm)) {
				throw new IllegalStateException("current screen is " + screenName(ctx.client.currentScreen)
					+ ", not the Plugin Manager (use open_plugin_manager first)");
			}
			String id = args.str("id");
			String state = args.lower("state", "toggle");
			List<PluginManager.PluginEntry> plugins = PluginManager.getPlugins();
			int row = -1;
			for (int i = 0; i < plugins.size(); i++) {
				if (plugins.get(i).id().equals(id)) {
					row = i;
				}
			}
			if (row < 0) {
				throw new IllegalStateException("no plugin '" + id + "' in the Plugin Manager; known: "
					+ plugins.stream().map(PluginManager.PluginEntry::id).toList());
			}
			boolean disabled = PluginManager.isDisabled(id);
			if (state.equals("on") && !disabled || state.equals("off") && disabled) {
				ctx.info(id + " already " + state);
				return StepRun.DONE;
			}
			ListWidget list = ScreenTools.findList(pm);
			list.setScrollAmount(row * (20 + list.padding) - (list.getLogicalHeight() - 20) / 2.0);
			int[] ticks = {0};
			StepRun[] click = {null};
			int targetRow = row;
			return () -> {
				if (click[0] == null) {
					if (++ticks[0] < 3) {
						return false;
					}
					ClickableWidget button = (ClickableWidget) list.children().get(targetRow).children().get(0);
					if (!button.active) {
						throw new IllegalStateException("the toggle of '" + id + "' is locked");
					}
					click[0] = beginClick(ctx, button, button.getX() + button.getWidth() / 2,
						button.getY() + button.getHeight() / 2, 0);
					ctx.info(id + " is now " + (PluginManager.isDisabled(id) ? "off" : "on"));
				}
				return click[0].tick();
			};
		});

		// {"type":"screen_close"}  closes the current screen the way its Done button / Escape does (Screen.close()),
		// so close hooks run: the config screen saves, the Plugin Manager saves and starts an EMI reload after a
		// change. (close_screen just drops all screens.) Follow it with emi_wait_loaded when a reload is expected.
		register("screen_close", (args, ctx) -> {
			Screen screen = requireScreen(ctx);
			String name = screenName(screen);
			screen.close();
			ctx.info("closed " + name + " -> " + screenName(ctx.client.currentScreen));
			return waitTicks(3);
		});

		// {"type":"click_row","text":"Distraction-Free Recipes"}  on EMI's config screen: scrolls the setting row whose
		// name equals, else contains, the text into view and presses its widget (a boolean switch, an enum button).
		// Notes the row; fails with the list of row names when nothing matches. Optional "index" (nth match).
		register("click_row", (args, ctx) -> {
			if (!(ctx.client.currentScreen instanceof ConfigScreen cs)) {
				throw new IllegalStateException("current screen is " + screenName(ctx.client.currentScreen)
					+ ", not EMI's ConfigScreen");
			}
			String needle = args.str("text").toLowerCase(Locale.ROOT).strip();
			List<ConfigEntryWidget> exact = new ArrayList<>();
			List<ConfigEntryWidget> partial = new ArrayList<>();
			List<String> names = new ArrayList<>();
			for (ListWidget.Entry e : cs.list.children()) {
				if (e instanceof ConfigEntryWidget c) {
					String name = c.getSearchableText();
					names.add(name);
					String lower = name.toLowerCase(Locale.ROOT).strip();
					if (lower.equals(needle)) {
						exact.add(c);
					} else if (lower.contains(needle)) {
						partial.add(c);
					}
				}
			}
			exact.addAll(partial);
			int index = args.intVal("index", 0);
			if (index >= exact.size()) {
				throw new IllegalStateException("no config row '" + args.str("text") + "'; rows: " + names);
			}
			ConfigEntryWidget row = exact.get(index);
			cs.list.centerScrollOn(row);
			int[] ticks = {0};
			StepRun[] click = {null};
			return () -> {
				if (click[0] == null) {
					if (++ticks[0] < 3) {
						return false;
					}
					ClickableWidget target = null;
					for (var child : row.children()) {
						if (child instanceof ClickableWidget w && w.visible) {
							target = w;
							break;
						}
					}
					if (target == null) {
						throw new IllegalStateException("row '" + row.getSearchableText() + "' has no clickable widget");
					}
					ctx.info("row '" + row.getSearchableText() + "' was '" + ScreenTools.textOf(target) + "'");
					click[0] = beginClick(ctx, target, target.getX() + target.getWidth() / 2,
						target.getY() + target.getHeight() / 2, 0);
				}
				return click[0].tick();
			};
		});

		// {"type":"window_info"}  records whether the game window has the OS focus (GLFW_FOCUSED), is visible, and
		// whether the cursor is locked. Dev clients must report focused=false, cursorLocked=false (emi_devtour mixins).
		register("window_info", (args, ctx) -> {
			long handle = ctx.client.getWindow().getHandle();
			ctx.info("focused=" + (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE)
				+ " visible=" + (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE)
				+ " cursorLocked=" + ctx.client.mouse.isCursorLocked()
				+ " cursorMode=" + GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR)
				+ " windowFocused(mc)=" + ctx.client.isWindowFocused());
			return StepRun.DONE;
		});

		// {"type":"hover","x":400,"y":120}  moves the virtual mouse to GUI coordinates so hover states and tooltips
		// render in the next screenshot. The position stays until another step moves it (or the real mouse does).
		register("hover", (args, ctx) -> {
			int x = args.intVal("x");
			int y = args.intVal("y");
			ScreenTools.moveMouse(ctx.client, x, y);
			ctx.info("mouse at " + x + "," + y);
			return waitTicks(2);
		});

		// {"type":"screen_info"}  records the screen class, its size and every widget (class, text, bounds) in info
		register("screen_info", (args, ctx) -> {
			Screen s = ctx.client.currentScreen;
			if (s == null) {
				ctx.info("no screen");
				return StepRun.DONE;
			}
			StringBuilder sb = new StringBuilder("screen " + s.getClass().getName() + " title '" + s.getTitle().getString()
				+ "' gui " + s.width + "x" + s.height + " mouse " + ScreenTools.mouseString(ctx.client) + "; widgets:");
			int n = 0;
			for (ClickableWidget w : ScreenTools.widgets(s)) {
				if (++n > 400) {
					sb.append(" ...");
					break;
				}
				sb.append("\n  ").append(ScreenTools.describe(w));
			}
			ctx.info(sb.toString());
			return StepRun.DONE;
		});

		// {"type":"call","class":"a.b.C","method":"run"}  invokes a public static no-arg method (escape hatch)
		register("call", (args, ctx) -> {
			Method m = Class.forName(args.str("class")).getMethod(args.str("method"));
			Object r = m.invoke(null);
			if (r != null) {
				ctx.info(String.valueOf(r));
			}
			return waitTicks(2);
		});
	}

	private static Screen requireScreen(TourContext ctx) {
		if (ctx.client.currentScreen == null) {
			throw new IllegalStateException("no screen open");
		}
		return ctx.client.currentScreen;
	}

	/** Mouse down now, mouse up on the next tick, then a short settle. {@code w} is only used for the note. */
	private static StepRun beginClick(TourContext ctx, ClickableWidget w, int cx, int cy, int button)
			throws ReflectiveOperationException {
		Screen screen = requireScreen(ctx);
		ScreenTools.moveMouse(ctx.client, cx, cy);
		boolean handled = screen.mouseClicked(cx, cy, button);
		ctx.info("clicked " + (w == null ? "" : ScreenTools.describe(w) + " ") + "at " + cx + "," + cy
			+ (handled ? "" : " (click not handled)"));
		int[] ticks = {0};
		return () -> {
			if (ticks[0]++ == 0) {
				if (ctx.client.currentScreen == screen) {
					screen.mouseReleased(cx, cy, button);
				}
				return false;
			}
			return ticks[0] >= 3;
		};
	}

	private static RecipeScreen requireRecipeScreen(TourContext ctx) {
		if (ctx.client.currentScreen instanceof RecipeScreen rs) {
			return rs;
		}
		throw new IllegalStateException("current screen is " + screenName(ctx.client.currentScreen) + ", not an EMI recipe screen");
	}

	private static StepRun afterRecipeScreen(TourContext ctx, Screen before, String what, String id) {
		Screen now = ctx.client.currentScreen;
		if (!(now instanceof RecipeScreen rs) || now == before) {
			throw new IllegalStateException("EMI has no " + what + " page for " + id + " (screen: " + screenName(now) + ")");
		}
		ctx.info(describeTabs(rs));
		return waitTicks(2);
	}
}
