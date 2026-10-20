package dev.emi.remastered.info.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.screen.widget.SizedButtonWidget;
import dev.emi.remastered.info.api.InfoElement;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.client.hub.Hub;
import dev.emi.remastered.info.client.hub.HubBar;
import dev.emi.remastered.info.client.hub.HubTabWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

/**
 * The Item Info screen: a header with the stack, then the page's sections grouped under "How to get", "Used for" and
 * "Details", in a scrollable panel drawn with EMI's textures. Escape, EMI's back key and the arrow button return to
 * the previous screen (EMI's history).
 */
public class InfoScreen extends Screen {
	private static final int HEADER = 38;
	private static final int SCROLLBAR = 6;
	private static final int INDENT = 8;
	private static final int OUTSIDE = -10000;
	public final EmiStack stack;
	public final HandledScreen<?> old;
	private InfoPage page;
	private final List<Row> rows = new ArrayList<>();
	private int panelX, panelY, panelW, panelH;
	private int viewX, viewY, viewW, viewH;
	private int contentHeight;
	private double scroll;
	private boolean dragging;
	private SlotWidget headerSlot;
	private int lastMouseX, lastMouseY;
	private List<HubTabWidget> tabs = List.of();

	private record Row(InfoElement element, int x, int y, int width, int height) {
	}

	public InfoScreen(EmiStack stack, HandledScreen<?> old) {
		super(EmiPort.translatable("screen.emi_info.title", stack.getName()));
		this.stack = stack;
		this.old = old;
	}

	@Override
	protected void init() {
		super.init();
		panelW = Math.min(width - 8, MathHelper.clamp(width - 16, 176, 360));
		panelH = Math.min(height - 4, MathHelper.clamp(height - 16, 80, 480));
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;
		Hub.Context hub = Hub.of(this);
		tabs = List.of();
		if (hub != null && Hub.fits(height)) {
			// the tab bar stands where it stands on the recipe screen, the panel starts below it
			panelY = Hub.plateTop(height);
			panelH = Math.max(HEADER + 24, Math.min(480, height - 8 - panelY));
			tabs = HubBar.create(hub, width / 2, panelY);
			tabs.forEach(this::addDrawableChild);
		}
		viewX = panelX + 6;
		viewY = panelY + HEADER;
		viewW = panelW - 12;
		viewH = panelH - HEADER - 6;
		headerSlot = new SlotWidget(stack, panelX + 6, panelY + 6).large(true);
		addDrawableChild(new SizedButtonWidget(panelX + panelW - 17, panelY + 5, 12, 12, 0, 0, () -> true,
			w -> close(), List.of(EmiPort.translatable("emi_info.back"))));
		page = InfoService.getPage(stack);
		layout();
	}

	private void layout() {
		rows.clear();
		int width = viewW - SCROLLBAR - 2;
		int y = 0;
		if (page.loading()) {
			y = add(new Elements.TextElement(EmiPort.translatable("emi_info.loading"), List.of(), null), 0, y, width);
		} else if (page.sections().isEmpty()) {
			y = add(new Elements.TextElement(EmiPort.translatable("emi_info.empty"), List.of(), null), 0, y, width);
		}
		InfoSection.Kind kind = null;
		for (SectionImpl section : page.sections()) {
			if (section.kind != kind) {
				kind = section.kind;
				if (y > 0) {
					y += 4;
				}
				y = add(new Elements.HeadingElement(EmiPort.translatable("emi_info.kind." + kind.name().toLowerCase())), 0, y, width) + 2;
			}
			y = add(new Elements.TitleElement(section.icon, section.title), 0, y, width);
			for (InfoElement element : section.elements) {
				y = add(element, INDENT, y, width - INDENT) + 2;
			}
			y += 2;
		}
		contentHeight = y;
		scroll = MathHelper.clamp(scroll, 0, maxScroll());
	}

	private int add(InfoElement element, int x, int y, int width) {
		int height;
		try {
			height = Math.max(0, element.getHeight(width));
		} catch (Throwable t) {
			EmiLog.error("Error laying out item info element", t);
			return y;
		}
		rows.add(new Row(element, x, y, width, height));
		return y + height;
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - viewH);
	}

	@Override
	public void tick() {
		InfoPage current = InfoService.getPage(stack);
		if (current != page && !(current.loading() && page.loading())) {
			page = current;
			layout();
		}
	}

	private boolean inView(int mouseX, int mouseY) {
		return mouseX >= viewX && mouseY >= viewY && mouseX < viewX + viewW - SCROLLBAR - 2 && mouseY < viewY + viewH;
	}

	private Row rowAt(int mouseX, int mouseY) {
		if (!inView(mouseX, mouseY)) {
			return null;
		}
		int y = mouseY - viewY + (int) scroll;
		int x = mouseX - viewX;
		for (Row row : rows) {
			if (y >= row.y && y < row.y + row.height && x >= row.x && x < row.x + row.width) {
				return row;
			}
		}
		return null;
	}

	private int rowTop(Row row) {
		return viewY + row.y - (int) scroll;
	}

	@Override
	public void render(DrawContext raw, int mouseX, int mouseY, float delta) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		renderInGameBackground(raw);
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, panelX, panelY, panelW, panelH, 0, 0, 4, 1);

		headerSlot.render(raw, mouseX, mouseY, delta);
		int textX = panelX + 38;
		int textW = panelX + panelW - 22 - textX;
		raw.drawText(textRenderer, Elements.trim(stack.getName(), textW), textX, panelY + 9, Elements.TEXT, false);
		Text sub = EmiPort.literal(EmiUtil.getModName(stack.getId().getNamespace()), Formatting.BLUE)
			.append(EmiPort.literal("  " + stack.getId(), Formatting.DARK_GRAY));
		raw.drawText(textRenderer, Elements.trim(sub, textW), textX, panelY + 21, Elements.MUTED, false);
		raw.fill(viewX, viewY - 3, viewX + viewW, viewY - 2, 0xFF8B8B8B);

		boolean mouseInView = inView(mouseX, mouseY);
		raw.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);
		for (Row row : rows) {
			int top = rowTop(row);
			if (top + row.height < viewY || top > viewY + viewH) {
				continue;
			}
			int left = viewX + row.x;
			int mx = mouseInView ? mouseX - left : OUTSIDE;
			int my = mouseInView ? mouseY - top : OUTSIDE;
			context.push();
			context.matrices().translate(left, top, 0);
			EmiPort.applyModelViewMatrix();
			try {
				row.element.render(raw, row.width, mx, my, delta);
			} catch (Throwable t) {
				EmiLog.error("Error rendering item info element", t);
			}
			context.pop();
			EmiPort.applyModelViewMatrix();
		}
		raw.disableScissor();
		renderScrollbar(raw, mouseX, mouseY);

		super.render(raw, mouseX, mouseY, delta);
		if (HubBar.renderTooltip(this, raw, tabs, mouseX, mouseY)) {
			return;
		}

		List<TooltipComponent> tooltip = List.of();
		if (headerSlot.getBounds().contains(mouseX, mouseY)) {
			tooltip = headerSlot.getTooltip(mouseX, mouseY);
		} else {
			Row row = rowAt(mouseX, mouseY);
			if (row != null) {
				try {
					tooltip = row.element.getTooltip(row.width, mouseX - viewX - row.x, mouseY - rowTop(row));
				} catch (Throwable t) {
					EmiLog.error("Error getting item info tooltip", t);
				}
			}
		}
		if (!tooltip.isEmpty()) {
			EmiRenderHelper.drawTooltip(this, context, tooltip, mouseX, mouseY);
		}
	}

	private void renderScrollbar(DrawContext raw, int mouseX, int mouseY) {
		if (maxScroll() <= 0) {
			return;
		}
		int x = viewX + viewW - SCROLLBAR;
		raw.fill(x, viewY, x + SCROLLBAR, viewY + viewH, 0xFF8B8B8B);
		int thumbH = Math.max(12, viewH * viewH / Math.max(1, contentHeight));
		int thumbY = viewY + (int) ((viewH - thumbH) * (scroll / maxScroll()));
		boolean hovered = dragging || (mouseX >= x && mouseX < x + SCROLLBAR && mouseY >= thumbY && mouseY < thumbY + thumbH);
		raw.fill(x, thumbY, x + SCROLLBAR, thumbY + thumbH, 0xFF373737);
		raw.fill(x, thumbY, x + SCROLLBAR - 1, thumbY + thumbH - 1, hovered ? 0xFFFFFFFF : 0xFFDBDBDB);
		raw.fill(x + 1, thumbY + 1, x + SCROLLBAR - 1, thumbY + thumbH - 1, hovered ? 0xFFDBDBDB : 0xFFC6C6C6);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// drawn in render
	}

	private void scrollTo(double value) {
		scroll = MathHelper.clamp(value, 0, maxScroll());
	}

	private void dragScroll(double mouseY) {
		int thumbH = Math.max(12, viewH * viewH / Math.max(1, contentHeight));
		double f = (mouseY - viewY - thumbH / 2.0) / Math.max(1, viewH - thumbH);
		scrollTo(f * maxScroll());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scrollTo(scroll - vertical * 18);
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		int mx = (int) mouseX, my = (int) mouseY;
		if (EmiConfig.back.matchesMouse(button)) {
			close();
			return true;
		}
		if (headerSlot.getBounds().contains(mx, my)) {
			return headerSlot.mouseClicked(mx, my, button);
		}
		int barX = viewX + viewW - SCROLLBAR;
		if (button == 0 && maxScroll() > 0 && mx >= barX && mx < barX + SCROLLBAR && my >= viewY && my < viewY + viewH) {
			dragging = true;
			dragScroll(mouseY);
			return true;
		}
		Row row = rowAt(mx, my);
		if (row != null) {
			try {
				return row.element.mouseClicked(row.width, mx - viewX - row.x, my - rowTop(row), button);
			} catch (Throwable t) {
				EmiLog.error("Error handling item info click", t);
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			dragScroll(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || EmiConfig.back.matchesKey(keyCode, scanCode)
				|| client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		if (headerSlot.getBounds().contains(lastMouseX, lastMouseY)) {
			if (headerSlot.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		} else {
			Row row = rowAt(lastMouseX, lastMouseY);
			if (row != null) {
				try {
					if (row.element.keyPressed(row.width, lastMouseX - viewX - row.x, lastMouseY - rowTop(row), keyCode, scanCode, modifiers)) {
						return true;
					}
				} catch (Throwable t) {
					EmiLog.error("Error handling item info key", t);
				}
			}
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_UP -> scrollTo(scroll - 18);
			case GLFW.GLFW_KEY_DOWN -> scrollTo(scroll + 18);
			case GLFW.GLFW_KEY_PAGE_UP -> scrollTo(scroll - viewH);
			case GLFW.GLFW_KEY_PAGE_DOWN -> scrollTo(scroll + viewH);
			case GLFW.GLFW_KEY_HOME -> scrollTo(0);
			case GLFW.GLFW_KEY_END -> scrollTo(maxScroll());
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	public void close() {
		InfoScreens.close(this);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** The page shown (for tests and tooling). */
	public InfoPage getPage() {
		return page;
	}
}
