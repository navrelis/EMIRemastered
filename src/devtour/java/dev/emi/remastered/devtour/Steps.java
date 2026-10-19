package dev.emi.remastered.devtour;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import dev.emi.remastered.effects.EffectEmiStack;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffect;
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
		// {"type":"emi_recipes","effect":"minecraft:speed"}  page of a status effect (effects module)
		register("emi_recipes", (args, ctx) -> {
			requireWorld(ctx);
			Screen before = ctx.client.currentScreen;
			String id;
			if (args.has("effect")) {
				id = args.str("effect");
				Identifier ident = Identifier.tryParse(id);
				StatusEffect effect = ident == null ? null : Registries.STATUS_EFFECT.get(ident);
				if (effect == null) {
					throw new IllegalArgumentException("unknown effect '" + id + "'");
				}
				EmiApi.displayRecipes(new EffectEmiStack(effect));
			} else {
				id = args.str("item");
				EmiApi.displayRecipes(EmiStack.of(resolveItem(id)));
			}
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

		// {"type":"click","x":383,"y":270}  left click (optional "button") at GUI-scaled coordinates in the open screen
		register("click", (args, ctx) -> {
			Screen screen = ctx.client.currentScreen;
			if (screen == null) {
				throw new IllegalStateException("no screen is open");
			}
			int button = args.intVal("button", 0);
			screen.mouseClicked(args.intVal("x"), args.intVal("y"), button);
			screen.mouseReleased(args.intVal("x"), args.intVal("y"), button);
			return waitTicks(2);
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
