package dev.emi.remastered.devtour;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import dev.emi.remastered.tools.ToolsModule;
import dev.emi.remastered.tools.ToolsSettings;
import dev.emi.remastered.tools.RecipeFormat;
import dev.emi.remastered.tools.client.RecipeClipboard;
import dev.emi.remastered.tools.client.TagBrowserScreen;
import dev.emi.remastered.tools.client.TagIndex;
import dev.emi.remastered.tools.client.ToolsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Items;

/**
 * Dev tour helpers of the {@code tools} module, called by the tour step {@code call} (public static, no arguments).
 * They drive the tag browser and "copy recipe", and write what ended up on the clipboard to
 * {@code run/tour-out/tools/}. Dev only, never packaged.
 */
public final class ToolsTour {
	private ToolsTour() {
	}

	private static MinecraftClient client() {
		return MinecraftClient.getInstance();
	}

	private static String screenName() {
		Screen screen = client().currentScreen;
		return screen == null ? "none" : screen.getClass().getSimpleName();
	}

	private static TagBrowserScreen browser() {
		if (client().currentScreen instanceof TagBrowserScreen screen) {
			return screen;
		}
		throw new IllegalStateException("current screen is " + screenName() + ", not the tag browser");
	}

	private static Object field(Object target, String name) throws ReflectiveOperationException {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	// ---------------------------------------------------------------- tag browser

	public static String openBrowser() {
		ToolsClient.openTagBrowser(null);
		return screenName();
	}

	public static String openLogs() {
		client().setScreen(new TagBrowserScreen(client().currentScreen, TagIndex.get(),
			TagBrowserScreen.Target.tag("minecraft:item", "minecraft:logs")));
		return screenName();
	}

	/** Opens the browser as if the bind was pressed over an oak log: the reverse lookup. */
	public static String openForOakLog() {
		ToolsClient.openTagBrowser(EmiStack.of(Items.OAK_LOG));
		return screenName();
	}

	private static String setSearch(String text) throws ReflectiveOperationException {
		TextFieldWidget search = (TextFieldWidget) field(browser(), "search");
		search.setText(text);
		return "search '" + text + "'";
	}

	public static String searchAxe() throws ReflectiveOperationException {
		return setSearch("axe");
	}

	public static String searchHusk() throws ReflectiveOperationException {
		return setSearch("@husk");
	}

	public static String searchClear() throws ReflectiveOperationException {
		return setSearch("");
	}

	private static String currentRegistry(TagBrowserScreen screen) throws ReflectiveOperationException {
		return ((dev.emi.remastered.tools.tags.TagFilter) field(screen, "filter")).registry();
	}

	private static String setRegistry(String registry) throws ReflectiveOperationException {
		Method cycle = TagBrowserScreen.class.getDeclaredMethod("cycleRegistry", boolean.class);
		cycle.setAccessible(true);
		for (int i = 0; i < 40 && !java.util.Objects.equals(currentRegistry(browser()), registry); i++) {
			cycle.invoke(browser(), false);
		}
		return "registry filter " + currentRegistry(browser());
	}

	public static String registryAll() throws ReflectiveOperationException {
		return setRegistry(null);
	}

	/** Entity types, with the tag minecraft:undead selected (spawn eggs as members). */
	public static String registryEntities() throws ReflectiveOperationException {
		String result = setRegistry("minecraft:entity_type");
		TagBrowserScreen screen = browser();
		Method select = TagBrowserScreen.class.getDeclaredMethod("select", dev.emi.remastered.tools.tags.TagEntry.class);
		select.setAccessible(true);
		select.invoke(screen, TagIndex.get().find("minecraft:entity_type", "minecraft:undead"));
		return result;
	}

	/** A data-driven registry: biome tags with plain ids as members. */
	public static String registryBiomes() throws ReflectiveOperationException {
		String result = setRegistry("minecraft:worldgen/biome");
		TagBrowserScreen screen = browser();
		Method select = TagBrowserScreen.class.getDeclaredMethod("select", dev.emi.remastered.tools.tags.TagEntry.class);
		select.setAccessible(true);
		select.invoke(screen, TagIndex.get().find("minecraft:worldgen/biome", "minecraft:is_forest"));
		return result;
	}

	/** Selects the item tag minecraft:logs, so a later filter that drops it shows whether the selection goes stale. */
	public static String selectLogs() throws ReflectiveOperationException {
		TagBrowserScreen screen = browser();
		Method select = TagBrowserScreen.class.getDeclaredMethod("select", dev.emi.remastered.tools.tags.TagEntry.class);
		select.setAccessible(true);
		select.invoke(screen, TagIndex.get().find("minecraft:item", "minecraft:logs"));
		return "selected " + field(screen, "selected");
	}

	/** What the right pane shows now (the selected tag), for the tour log. */
	public static String selection() throws ReflectiveOperationException {
		return "selected=" + field(browser(), "selected") + ", results=" + ((List<?>) field(browser(), "results")).size();
	}

	/** Scrolls the longest tag id of the list into view and hovers its row (the row tooltip shows the full id). */
	public static String hoverLongestTag() throws ReflectiveOperationException {
		TagBrowserScreen screen = browser();
		List<?> results = (List<?>) field(screen, "results");
		int longest = 0;
		for (int i = 0; i < results.size(); i++) {
			String id = ((dev.emi.remastered.tools.tags.TagEntry) results.get(i)).id();
			if (id.length() > ((dev.emi.remastered.tools.tags.TagEntry) results.get(longest)).id().length()) {
				longest = i;
			}
		}
		Object scroll = field(screen, "tagScroll");
		Field scrollY = scroll.getClass().getDeclaredField("scroll");
		scrollY.setAccessible(true);
		int row = 12;
		int listX = (int) field(screen, "listX");
		int listY = (int) field(screen, "listY");
		scrollY.setInt(scroll, Math.max(0, longest * row - 2 * row));
		int y = listY + 1 + longest * row - scrollY.getInt(scroll) + row / 2;
		MinecraftClient client = client();
		double scale = client.getWindow().getScaleFactor();
		Method move = client.mouse.getClass().getDeclaredMethod("onCursorPos", long.class, double.class, double.class);
		move.setAccessible(true);
		move.invoke(client.mouse, client.getWindow().getHandle(), (listX + 20) * scale, y * scale);
		return "hover row " + longest + " " + ((dev.emi.remastered.tools.tags.TagEntry) results.get(longest)).id();
	}

	public static String closeScreen() {
		browser().close();
		return screenName();
	}

	/** Moves the game's mouse over the n-th stack member of the open browser (no real cursor movement needed). */
	private static String hoverMember(int index) throws ReflectiveOperationException {
		TagBrowserScreen screen = browser();
		int detailX = (int) field(screen, "detailX");
		int gridY = (int) field(screen, "gridY");
		int columns = (int) field(screen, "columns");
		MinecraftClient client = client();
		double scale = client.getWindow().getScaleFactor();
		double x = (detailX + 1 + (index % columns) * 18 + 9) * scale;
		double y = (gridY + 1 + (index / columns) * 18 + 9) * scale;
		Method move = client.mouse.getClass().getDeclaredMethod("onCursorPos", long.class, double.class, double.class);
		move.setAccessible(true);
		move.invoke(client.mouse, client.getWindow().getHandle(), x, y);
		return "mouse at " + x + "," + y;
	}

	public static String hoverFirstMember() throws ReflectiveOperationException {
		return hoverMember(0);
	}

	public static String hoverFifthMember() throws ReflectiveOperationException {
		return hoverMember(4);
	}

	private static String click(int button) throws ReflectiveOperationException {
		TagBrowserScreen screen = browser();
		Object member = ((List<?>) field(screen, "stackMembers")).get(0);
		Method click = TagBrowserScreen.class.getDeclaredMethod("memberClicked", member.getClass(), int.class);
		click.setAccessible(true);
		click.invoke(screen, member, button);
		return screenName();
	}

	/** Left click on the first stack member: EMI recipes (stays in the browser with a message when there are none). */
	public static String clickFirstMember() throws ReflectiveOperationException {
		return click(0);
	}

	/** Right click on the first stack member: EMI uses, with the browser pushed on EMI's history. */
	public static String clickFirstMemberUses() throws ReflectiveOperationException {
		return click(1);
	}

	// ---------------------------------------------------------------- copy recipe

	private static EmiRecipe hoveredRecipe(boolean preferVanilla) throws ReflectiveOperationException {
		if (!(client().currentScreen instanceof RecipeScreen screen)) {
			throw new IllegalStateException("current screen is " + screenName() + ", not the recipe screen");
		}
		Field f = RecipeScreen.class.getDeclaredField("currentPage");
		f.setAccessible(true);
		EmiRecipe first = null;
		for (Object o : (List<?>) f.get(screen)) {
			WidgetGroup group = (WidgetGroup) o;
			if (group.recipe == null) {
				continue;
			}
			if (!preferVanilla || group.recipe.getBackingRecipe() != null) {
				return group.recipe;
			}
			if (first == null) {
				first = group.recipe;
			}
		}
		if (first != null) {
			return first;
		}
		throw new IllegalStateException("no recipe on the page");
	}

	private static String record(EmiRecipe recipe, RecipeFormat format, String suffix) throws Exception {
		String actual = client().keyboard.getClipboard();
		String clipboard = RecipeClipboard.render(recipe, format).content();
		String name = (recipe.getId() == null ? "no-id" : recipe.getId().toString().replaceAll("[^A-Za-z0-9._-]", "_")) + "-" + suffix + ".txt";
		Path dir = client().runDirectory.toPath().resolve("tour-out").resolve("tools");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(name), clipboard, StandardCharsets.UTF_8);
		return name + " (" + clipboard.length() + " chars, system clipboard " + (actual.equals(clipboard) ? "matches" : "unreadable or different: " + actual.length() + " chars") + ")";
	}

	/** The hook with the plain bind pressed: the default format (text). */
	public static String copyDefault() throws Exception {
		return copyDefault(hoveredRecipe(true));
	}

	public static String copyDefaultFirst() throws Exception {
		return copyDefault(hoveredRecipe(false));
	}

	private static String copyDefault(EmiRecipe recipe) throws Exception {
		boolean handled = EmiScreenManager.recipeInteraction(recipe, bind -> bind == ToolsSettings.copyRecipe);
		return "handled=" + handled + ", " + record(recipe, ToolsSettings.copyRecipeFormat, "default");
	}

	/** The hook with shift + the bind pressed: the other format (JSON). */
	public static String copyOther() throws Exception {
		return copyOther(hoveredRecipe(true));
	}

	public static String copyOtherFirst() throws Exception {
		return copyOther(hoveredRecipe(false));
	}

	private static String copyOther(EmiRecipe recipe) throws Exception {
		boolean handled = EmiScreenManager.recipeInteraction(recipe,
			bind -> bind != ToolsSettings.copyRecipe && bind.translationKey.equals(ToolsSettings.copyRecipe.translationKey));
		return "handled=" + handled + ", " + record(recipe, ToolsSettings.copyRecipeFormat.other(), "other");
	}

	/** With the module switched off the hook must neither handle the key nor touch the clipboard. */
	public static String copyModuleOff() throws Exception {
		EmiRecipe recipe = hoveredRecipe(true);
		client().keyboard.setClipboard("UNCHANGED");
		ToolsModule.enabled = false;
		try {
			boolean handled = EmiScreenManager.recipeInteraction(recipe, bind -> bind == ToolsSettings.copyRecipe);
			boolean opened = dev.emi.emi.screen.EmiScreenManager.genericInteraction(bind -> bind == ToolsSettings.openTagBrowser);
			return "moduleOff: copyHandled=" + handled + ", tagBrowserHandled=" + opened + ", screen=" + screenName()
				+ ", clipboard=" + client().keyboard.getClipboard();
		} finally {
			ToolsModule.enabled = true;
		}
	}
}
