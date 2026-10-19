package dev.emi.remastered.distractionfree;

import java.util.List;
import java.util.function.Function;

import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.SidebarSide;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.BoMScreen;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.EmiScreenManager.ScreenSpace;
import dev.emi.emi.screen.EmiScreenManager.SidebarPanel;
import dev.emi.emi.screen.RecipeScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * Distraction-free mode (Distraction Free Recipes 1.2.2 by Toni, MIT): EMI's index sidebar stays hidden until you
 * search. Client only. EMI's {@link EmiScreenManager} calls the small hooks of this class; all state and the
 * decisions live here.
 * <p>
 * As in the original, the index panel is shown while the search box is not empty. While it is hidden, a hint and a
 * "Show Recipes" button take its place; the button pins the panel open ("Hide Recipes" unpins it again). Additions:
 * the pin also has a key bind, the panel fades and slides in and out over
 * {@link DistractionFreeSettings#fadeDuration} (it takes no clicks, scrolls or tooltips while it is not completely
 * shown), and the mode can be limited to some kinds of screens.
 */
public final class DistractionFree {
	/** Distance in pixels the panel slides while fading. */
	private static final int SLIDE = 24;
	/** Size of a sidebar cell in pixels (EMI: 16 plus the padding). */
	private static final int ENTRY_SIZE = 18;
	private static final int TEXT_COLOR = 0x80C8C8C8;
	private static final int TEXT_COLOR_HOVER = 0xFFFFFFFF;
	private static final int BUTTON_COLOR = 0x32000000;
	private static final int BUTTON_COLOR_HOVER = 0x70000000;
	private static final int BUTTON_HEIGHT = 15;

	// state of the current screen
	private static Screen lastScreen;
	private static long lastNanos;
	private static boolean active;
	private static boolean target = true;
	private static float progress = 1f;

	// cache of "does distraction-free mode apply to this screen"
	private static Screen cachedScreen;
	private static int cachedFlags = -1;
	private static String cachedExcluded;
	private static boolean cachedResult;
	private static List<String> cachedEntries = List.of();

	// click areas of the frame that was drawn last: x, y, width, height
	private static int[] showArea;
	private static int[] hideArea;

	private DistractionFree() {
	}

	/** Whether distraction-free mode applies to this screen (module on, kind of screen, excluded screens). */
	public static boolean appliesTo(Screen screen) {
		if (screen == null || !DistractionFreeModule.enabled) {
			return false;
		}
		int flags = (DistractionFreeSettings.applyToInventory ? 1 : 0) | (DistractionFreeSettings.applyToCrafting ? 2 : 0)
				| (DistractionFreeSettings.applyToContainers ? 4 : 0) | (DistractionFreeSettings.keepOnRecipeScreens ? 8 : 0);
		String excluded = DistractionFreeSettings.excludedScreens == null ? "" : DistractionFreeSettings.excludedScreens;
		if (screen == cachedScreen && flags == cachedFlags && excluded.equals(cachedExcluded)) {
			return cachedResult;
		}
		if (!excluded.equals(cachedExcluded)) {
			cachedEntries = ScreenFilter.parse(excluded);
		}
		boolean result = ScreenRules.applies(kindOf(screen), (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0)
				&& !ScreenFilter.matches(cachedEntries, screen.getClass(), DistractionFree::toRuntimeName);
		cachedScreen = screen;
		cachedFlags = flags;
		cachedExcluded = excluded;
		cachedResult = result;
		return result;
	}

	private static ScreenKind kindOf(Screen screen) {
		if (screen instanceof RecipeScreen || screen instanceof BoMScreen) {
			return ScreenKind.RECIPE;
		} else if (screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen) {
			return ScreenKind.INVENTORY;
		} else if (screen instanceof CraftingScreen) {
			return ScreenKind.CRAFTING;
		}
		return ScreenKind.CONTAINER;
	}

	/** Translates a Yarn class name to the running environment's name (identity in a development environment). */
	private static String toRuntimeName(String yarnName) {
		try {
			return FabricLoader.getInstance().getMappingResolver().mapClassName("named", yarnName);
		} catch (RuntimeException e) {
			return yarnName;
		}
	}

	/** Called every frame by EMI's screen manager: decides what the index panel should do and moves the fade. */
	public static void update() {
		MinecraftClient client = MinecraftClient.getInstance();
		Screen screen = client.currentScreen;
		long now = System.nanoTime();
		boolean newScreen = screen != lastScreen;
		active = appliesTo(screen);
		target = !active || DistractionFreeSettings.pinned || !EmiScreenManager.search.getText().isEmpty();
		if (newScreen || !active) {
			progress = target ? 1f : 0f;
		} else {
			progress = Fade.step(progress, target, now - lastNanos, DistractionFreeSettings.fadeDuration);
		}
		lastScreen = screen;
		lastNanos = now;
		// the panel render hooks set the click areas again in this frame
		showArea = null;
		hideArea = null;
	}

	private static void ensureCurrent() {
		if (MinecraftClient.getInstance().currentScreen != lastScreen) {
			update();
		}
	}

	/** Whether the panel is one distraction-free mode manages: the index sidebar while the mode is active. */
	private static boolean manages(SidebarPanel panel) {
		return panel.getType() == SidebarType.INDEX && active;
	}

	/** True when the panel must not react to the mouse (hidden, or not yet completely faded in). */
	public static boolean isHidden(SidebarPanel panel) {
		ensureCurrent();
		return manages(panel) && !(target && progress >= 1f);
	}

	/** True when the panel is drawn at all (shown, or fading). */
	public static boolean isDrawn(SidebarPanel panel) {
		ensureCurrent();
		return !manages(panel) || progress > 0f;
	}

	private static boolean isFading(SidebarPanel panel) {
		return manages(panel) && progress > 0f && progress < 1f;
	}

	// Rendering

	/**
	 * Slides the panel while it fades; call {@link #endTransform} in a finally block with the returned value.
	 * Everything the panel draws (GUI draw calls and the batched item renderer) follows the model view matrix.
	 */
	public static boolean beginTransform(EmiDrawContext context, SidebarPanel panel) {
		if (!isFading(panel)) {
			return false;
		}
		int offset = Math.round((1f - Fade.ease(progress)) * SLIDE);
		int dx = 0;
		int dy = 0;
		switch (panel.side) {
			case LEFT -> dx = -offset;
			case RIGHT -> dx = offset;
			case TOP -> dy = -offset;
			case BOTTOM -> dy = offset;
			default -> {
			}
		}
		Matrix4fStack view = RenderSystem.getModelViewStack();
		view.pushMatrix();
		view.translate(dx, dy, 0);
		EmiPort.applyModelViewMatrix();
		return true;
	}

	public static void endTransform(EmiDrawContext context, boolean applied) {
		if (applied) {
			RenderSystem.getModelViewStack().popMatrix();
			EmiPort.applyModelViewMatrix();
			context.resetColor();
		}
	}

	/** Replaces {@code context.resetColor()} in the panel background: the fade alpha while fading. */
	public static void applyColor(EmiDrawContext context, SidebarPanel panel) {
		if (isFading(panel)) {
			context.setColor(1f, 1f, 1f, Fade.ease(progress));
		} else {
			context.resetColor();
		}
	}

	/** Help text and "Show Recipes" button in the place of a hidden index panel. */
	public static void renderHint(EmiDrawContext context, SidebarPanel panel, int mouseX, int mouseY) {
		if (!manages(panel) || target || progress > 0f || panel.space == null) {
			return;
		}
		boolean text = DistractionFreeSettings.showHelpText;
		boolean button = DistractionFreeSettings.showToggleButton;
		if (!text && !button) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		TextRenderer font = client.textRenderer;
		ScreenSpace space = panel.space;
		int width = space.tw * ENTRY_SIZE;
		int cx = space.tx + width / 2;
		// positions of the original: the button 20 pixels below the middle of the screen, the text 20 pixels above it
		int buttonY = client.getWindow().getScaledHeight() / 2 + 20;
		if (text) {
			List<OrderedText> lines = font.wrapLines(EmiPort.translatable("distraction_free_recipes.hint.search"), Math.max(width, 80));
			int lineHeight = font.fontHeight + 2;
			int y = buttonY - 20 - (lines.size() - 1) * lineHeight;
			for (OrderedText line : lines) {
				context.drawTextWithShadow(line, cx - font.getWidth(line) / 2, y, TEXT_COLOR);
				y += lineHeight;
			}
		}
		if (button) {
			int buttonWidth = Math.min(100, (int) (width * 0.5f));
			int[] area = {cx - buttonWidth / 2, buttonY, buttonWidth, BUTTON_HEIGHT};
			showArea = area;
			drawButton(context, area, EmiPort.translatable("distraction_free_recipes.hint.show"), mouseX, mouseY);
		}
	}

	/** "Hide Recipes" button next to a shown index panel while it is pinned open. */
	public static void renderHideButton(EmiDrawContext context, SidebarPanel panel, int mouseX, int mouseY) {
		if (!manages(panel) || !DistractionFreeSettings.pinned || !DistractionFreeSettings.showToggleButton
				|| isFading(panel) || panel.space == null) {
			return;
		}
		ScreenSpace space = panel.space;
		int width = space.tw * ENTRY_SIZE;
		int cx = space.tx + width / 2;
		int buttonWidth = Math.min(100, (int) (width * 0.5f));
		int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
		int x;
		if (panel.side == SidebarSide.LEFT) {
			x = cx + width / 2 + 5;
		} else {
			// position of the original: left of the panel, at the top of the screen
			x = cx - width - 5 + Math.max(0, (int) (width * 0.5f) - 100);
		}
		x = Math.max(0, Math.min(screenWidth - buttonWidth, x));
		int[] area = {x, 8, buttonWidth, BUTTON_HEIGHT};
		hideArea = area;
		drawButton(context, area, EmiPort.translatable("distraction_free_recipes.hint.hide"), mouseX, mouseY);
	}

	private static void drawButton(EmiDrawContext context, int[] area, Text label, int mouseX, int mouseY) {
		boolean hover = contains(area, mouseX, mouseY);
		context.fill(area[0], area[1], area[2], area[3], hover ? BUTTON_COLOR_HOVER : BUTTON_COLOR);
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		context.drawTextWithShadow(label.asOrderedText(), area[0] + (area[2] - font.getWidth(label)) / 2, area[1] + 4,
				hover ? TEXT_COLOR_HOVER : TEXT_COLOR);
	}

	private static boolean contains(int[] area, int x, int y) {
		return x >= area[0] && y >= area[1] && x < area[0] + area[2] && y < area[1] + area[3];
	}

	// Input

	/** Left click on the "Show Recipes" or "Hide Recipes" button: pins or unpins. Returns whether the click was used. */
	public static boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 || !active) {
			return false;
		}
		int x = (int) mouseX;
		int y = (int) mouseY;
		if (showArea != null && contains(showArea, x, y) && !DistractionFreeSettings.pinned) {
			click();
			setPinned(true);
			return true;
		}
		if (hideArea != null && contains(hideArea, x, y) && DistractionFreeSettings.pinned) {
			click();
			setPinned(false);
			return true;
		}
		return false;
	}

	private static void click() {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	/** The pin bind, checked by {@code EmiScreenManager.genericInteraction}. */
	public static boolean handleBind(Function<EmiBind, Boolean> function) {
		if (DistractionFreeModule.enabled && function.apply(DistractionFreeSettings.pinBind)) {
			togglePinned();
			return true;
		}
		return false;
	}

	/** Toggles "pinned open" and saves it. Public so that the dev tour can call it. */
	public static void togglePinned() {
		setPinned(!DistractionFreeSettings.pinned);
	}

	public static void setPinned(boolean pinned) {
		if (DistractionFreeSettings.pinned != pinned) {
			DistractionFreeSettings.pinned = pinned;
			EmiConfig.writeConfig();
		}
	}

	/** Setting "lower search opacity", for {@code EmiSearchWidget}. */
	public static boolean lowerSearchOpacity() {
		return DistractionFreeModule.enabled && DistractionFreeSettings.lowerSearchOpacity;
	}

	/** One line with the current state, for the dev tour. */
	public static String status() {
		return "distraction-free: module=" + DistractionFreeModule.enabled + " active=" + active + " pinned="
				+ DistractionFreeSettings.pinned + " target=" + target + " progress=" + progress;
	}
}
