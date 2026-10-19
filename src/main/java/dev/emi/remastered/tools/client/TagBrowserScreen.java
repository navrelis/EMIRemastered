package dev.emi.remastered.tools.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiHistory;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.tools.tags.NumberText;
import dev.emi.remastered.tools.tags.RowLayout;
import dev.emi.remastered.tools.tags.TagCatalog;
import dev.emi.remastered.tools.tags.TagEntry;
import dev.emi.remastered.tools.tags.TagFilter;
import dev.emi.remastered.tools.tags.TagFilter.MemberRef;
import dev.emi.remastered.tools.tags.TagNames;
import dev.emi.remastered.tools.tags.TagSearch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * The tag browser: every tag of every registry of the client world with its members.
 * <p>
 * Left: the tags (filtered by the search field, the registry button and an optional "contains this item" filter).
 * Right: the members of the selected tag as EMI stacks (items, blocks and fluids as themselves, entity types as spawn
 * eggs, everything else as ids). Clicking a member opens EMI's recipes (right click: uses); hovering a member shows the
 * tags that contain it. Escape returns to the previous screen.
 */
public class TagBrowserScreen extends Screen {
	private static final String KEY = "emi_remastered_tools.tags.";
	private static final int ROW = 12;
	private static final int SLOT = 18;
	private static final int TEXT_ROW = 10;
	private static final int PAD = 6;
	private static final int BAR = 6;
	private static final int PANEL_TEXT = 0x404040;
	private static final int PANEL_SUBTEXT = 0x6C6C6C;

	/** What the browser shows when it opens. */
	public record Target(@Nullable String registry, @Nullable String tag, @Nullable MemberRef member) {
		public static final Target NONE = new Target(null, null, null);

		public static Target tag(String registry, String tag) {
			return new Target(registry, tag, null);
		}

		public static Target member(MemberRef member) {
			return new Target(null, null, member);
		}
	}

	private final @Nullable Screen previous;
	private final TagCatalog catalog;
	private final Locale locale = NumberText.locale(MinecraftClient.getInstance().getLanguageManager().getLanguage());
	private final Map<TagEntry, String> titles = new IdentityHashMap<>();
	private TagFilter filter = TagFilter.ALL;
	private List<TagEntry> results = List.of();
	private @Nullable TagEntry selected;
	private List<TagMember> stackMembers = List.of();
	private List<TagMember> textMembers = List.of();
	private final Scroll tagScroll = new Scroll();
	private final Scroll memberScroll = new Scroll();
	private @Nullable Text status;
	private long statusUntil;
	private boolean searchFocused = true;

	private TextFieldWidget search;
	private ButtonWidget registryButton;
	private int panelX, panelY, panelW, panelH;
	private int listX, listY, listW, listH;
	private int detailX, detailW, gridY, gridH, footerY;
	private int columns = 1;

	private int hoveredTag = -1;
	private @Nullable TagMember hoveredMember;
	private boolean overChipClear;

	public TagBrowserScreen(@Nullable Screen previous, TagCatalog catalog, Target target) {
		super(EmiPort.translatable(KEY + "title"));
		this.previous = previous;
		this.catalog = catalog;
		if (target.registry() != null) {
			filter = filter.withRegistry(target.registry());
		}
		if (target.member() != null) {
			filter = filter.withMember(target.member());
		}
		refilter();
		if (target.tag() != null) {
			select(catalog.find(target.registry(), target.tag()));
			int index = selected == null ? -1 : results.indexOf(selected);
			if (index >= 0) {
				tagScroll.scroll = Math.max(0, index * ROW - 3 * ROW);
			}
		} else if (target.member() != null && !results.isEmpty()) {
			select(results.get(0));
		}
	}

	// ------------------------------------------------------------------ state

	private void refilter() {
		results = TagSearch.filter(catalog.entries(), filter);
		tagScroll.scroll = 0;
		hoveredTag = -1;
		hoveredMember = null;
		// the right pane must not keep showing a tag the left list no longer has (the selection is cleared, not moved)
		if (selected != null && !results.contains(selected)) {
			select(null);
		}
	}

	private String num(long number) {
		return NumberText.format(number, locale);
	}

	/** The friendly name of a tag: its translation if there is one, else a name made of the last path segment. */
	private String title(TagEntry tag) {
		return titles.computeIfAbsent(tag, t -> {
			String key = TagNames.tagKey(t.registry(), t.id());
			if (I18n.hasTranslation(key)) {
				return I18n.translate(key);
			}
			Text name = tagName(t);
			if (!name.getString().equals("#" + t.id())) {
				return name.getString();
			}
			return TagNames.readable(t.id());
		});
	}

	private void select(@Nullable TagEntry tag) {
		selected = tag;
		memberScroll.scroll = 0;
		if (tag == null) {
			stackMembers = List.of();
			textMembers = List.of();
			return;
		}
		List<TagMember> stacks = new ArrayList<>();
		List<TagMember> texts = new ArrayList<>();
		for (TagMember member : TagMember.of(tag)) {
			(member.stack() == null ? texts : stacks).add(member);
		}
		stackMembers = stacks;
		textMembers = texts;
	}

	private void cycleRegistry(boolean backwards) {
		List<String> registries = catalog.registries();
		if (registries.isEmpty()) {
			return;
		}
		int index = filter.registry() == null ? -1 : registries.indexOf(filter.registry());
		int count = registries.size() + 1;
		int next = Math.floorMod(index + 1 + (backwards ? -1 : 1), count) - 1;
		filter = filter.withRegistry(next < 0 ? null : registries.get(next));
		refilter();
		registryButton.setMessage(registryButtonText());
	}

	private void setStatus(Text text) {
		status = text;
		statusUntil = System.currentTimeMillis() + 3000;
	}

	// ------------------------------------------------------------------ layout

	@Override
	protected void init() {
		super.init();
		panelW = Math.min(width - 8, 520);
		panelH = Math.min(height - 8, 340);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;
		int buttonW = Math.min(110, panelW / 3);
		int searchY = panelY + 18;
		String text = search == null ? filter.query() : search.getText();
		search = new TextFieldWidget(textRenderer, panelX + PAD, searchY, panelW - PAD * 2 - buttonW - 4, 16,
			EmiPort.translatable(KEY + "search"));
		search.setMaxLength(128);
		search.setPlaceholder(EmiPort.translatable(KEY + "search.hint").formatted(Formatting.DARK_GRAY));
		search.setText(text);
		search.setChangedListener(value -> {
			filter = filter.withQuery(value);
			refilter();
		});
		addDrawableChild(search);
		registryButton = EmiPort.newButton(panelX + panelW - PAD - buttonW, searchY, buttonW, 16, registryButtonText(),
			button -> cycleRegistry(Screen.hasShiftDown()));
		addDrawableChild(registryButton);
		if (searchFocused) {
			setFocused(search);
			search.setFocused(true);
		}

		int bodyTop = searchY + 16 + 20;
		int bodyBottom = panelY + panelH - 28;
		listX = panelX + PAD;
		listY = bodyTop;
		listW = Math.max(100, (panelW - PAD * 3) * 2 / 5);
		listH = Math.max(ROW * 2, bodyBottom - bodyTop);
		detailX = listX + listW + PAD;
		detailW = panelX + panelW - PAD - detailX;
		gridY = bodyTop + 35;
		gridH = Math.max(SLOT, bodyBottom - gridY);
		footerY = bodyBottom + 4;
		columns = Math.max(1, (detailW - BAR - 2) / SLOT);
	}

	/** The text cut to the width, with "..." when something was cut off. */
	private String fit(String text, int width) {
		if (textRenderer.getWidth(text) <= width) {
			return text;
		}
		return textRenderer.trimToWidth(text, Math.max(0, width - textRenderer.getWidth("..."))) + "...";
	}

	private Text registryButtonText() {
		return EmiPort.literal(registryLabel(filter.registry()).getString() + " (" + num(catalog.count(filter.registry())) + ")");
	}

	private static Text registryLabel(@Nullable String registry) {
		if (registry == null) {
			return EmiPort.translatable(KEY + "registry.all");
		}
		String key = KEY + "registry." + registry.replace(':', '.').replace('/', '.');
		if (I18n.hasTranslation(key)) {
			return EmiPort.translatable(key);
		}
		return EmiPort.literal(registry.startsWith("minecraft:") ? registry.substring("minecraft:".length()) : registry);
	}

	private int contentHeightOfMembers() {
		int rows = (stackMembers.size() + columns - 1) / columns;
		int height = rows * SLOT;
		if (!textMembers.isEmpty()) {
			height += (rows > 0 ? 4 : 0) + textMembers.size() * TEXT_ROW;
		}
		return height + 2;
	}

	// ------------------------------------------------------------------ render

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// drawn in render() so the panel is drawn on top of a single background
	}

	@Override
	public void render(DrawContext raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		renderInGameBackground(raw);
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, EmiRenderHelper.BACKGROUND, panelX, panelY, panelW, panelH, 0, 0, 4, 1);
		context.drawText(title, panelX + PAD, panelY + 7, PANEL_TEXT);
		Text count = EmiPort.translatable(KEY + "count", num(results.size()), num(catalog.count(filter.registry())));
		context.drawText(count, panelX + panelW - PAD - textRenderer.getWidth(count), panelY + 7, PANEL_SUBTEXT);

		super.render(raw, mouseX, mouseY, delta);

		renderChip(context, mouseX, mouseY);
		hoveredTag = -1;
		hoveredMember = null;
		renderTagList(context, mouseX, mouseY);
		renderDetail(context, mouseX, mouseY, delta);
		renderFooter(context);
		renderTooltips(context, mouseX, mouseY);
	}

	/** The line above the lists: the "contains this item" filter, or what the search does. */
	private void renderChip(EmiDrawContext context, int mouseX, int mouseY) {
		overChipClear = false;
		int y = listY - 20;
		MemberRef member = filter.member();
		if (member == null) {
			return;
		}
		Text clear = EmiPort.literal("[x]");
		int clearW = textRenderer.getWidth(clear);
		int clearX = panelX + panelW - PAD - clearW;
		overChipClear = mouseX >= clearX && mouseX < clearX + clearW && mouseY >= y && mouseY < y + 10;
		context.drawText(clear, clearX, y + 1, overChipClear ? 0xFFFFFF : 0x8B0000);
		String name = member.id();
		EmiStack stack = member.registry() == null || member.registry().equals("minecraft:item") ? TagMember.stackOf("minecraft:item", member.id()) : null;
		int x = panelX + PAD;
		if (stack != null) {
			context.drawStack(stack, x - 1, y - 3, 0, EmiIngredient.RENDER_ICON);
			x += 15;
			name = stack.getName().getString() + " (" + member.id() + ")";
		}
		Text label = EmiPort.translatable(KEY + "containing", name);
		context.drawText(EmiPort.literal(fit(label.getString(), clearX - x - 6)), x, y + 1, PANEL_TEXT);
	}

	private void renderTagList(EmiDrawContext context, int mouseX, int mouseY) {
		DrawContext raw = context.raw();
		drawInset(context, listX, listY, listW, listH);
		int viewW = listW - 2 - BAR;
		tagScroll.update(results.size() * ROW + 2, listH - 2);
		raw.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
		boolean showRegistry = filter.registry() == null && listW >= 150;
		int first = Math.max(0, tagScroll.scroll / ROW);
		int last = Math.min(results.size(), (tagScroll.scroll + listH) / ROW + 1);
		for (int i = first; i < last; i++) {
			TagEntry tag = results.get(i);
			int y = listY + 1 + i * ROW - tagScroll.scroll;
			boolean hovered = mouseX >= listX + 1 && mouseX < listX + 1 + viewW && mouseY >= Math.max(y, listY + 1)
				&& mouseY < Math.min(y + ROW, listY + listH - 1);
			if (hovered) {
				hoveredTag = i;
			}
			if (tag == selected) {
				context.fill(listX + 1, y, viewW, ROW, 0xFF4A6F8F);
			} else if (hovered) {
				context.fill(listX + 1, y, viewW, ROW, 0xFF34495C);
			}
			// the id keeps its width; the registry label goes first, then the count (see RowLayout)
			boolean empty = tag.size() == 0;
			String size = num(tag.size());
			String registry = showRegistry ? registryLabel(tag.registry()).getString() : "";
			int idX = listX + 4;
			int rowRight = listX + 1 + viewW - 3;
			RowLayout layout = RowLayout.of(rowRight - idX, textRenderer.getWidth(tag.id()), textRenderer.getWidth(size),
				showRegistry ? textRenderer.getWidth(registry) : 0, 6);
			int right = rowRight;
			if (layout.showSize()) {
				int sizeW = textRenderer.getWidth(size);
				context.drawText(EmiPort.literal(size), right - sizeW, y + 2, empty ? 0xFFA05050 : 0xFF909090);
				right -= sizeW + 6;
			}
			if (layout.showRegistry()) {
				context.drawText(EmiPort.literal(registry), right - textRenderer.getWidth(registry), y + 2, 0xFF6F7F8F);
			}
			int idColor = tag == selected ? 0xFFFFFFFF : empty ? 0xFF7C8288 : 0xFFD8D8D8;
			context.drawText(EmiPort.literal(fit(tag.id(), layout.idWidth())), idX, y + 2, idColor);
		}
		raw.disableScissor();
		if (results.isEmpty()) {
			Text none = EmiPort.translatable(KEY + "none");
			context.drawText(none, listX + (listW - textRenderer.getWidth(none)) / 2, listY + listH / 2 - 4, 0xFF909090);
		}
		tagScroll.render(context, listX + listW - 1 - BAR, listY + 1, listH - 2, mouseX, mouseY);
	}

	private void renderDetail(EmiDrawContext context, int mouseX, int mouseY, float delta) {
		DrawContext raw = context.raw();
		if (selected == null) {
			Text hint = EmiPort.translatable(KEY + "select");
			int y = listY + 4;
			for (var line : textRenderer.wrapLines(hint, detailW)) {
				context.drawText(line, detailX, y, PANEL_SUBTEXT);
				y += 10;
			}
			return;
		}
		TagEntry tag = selected;
		context.drawText(EmiPort.literal(fit(title(tag), detailW)), detailX, listY + 1, PANEL_TEXT);
		context.drawText(EmiPort.literal(fit("#" + tag.id(), detailW)), detailX, listY + 12, PANEL_SUBTEXT);
		Text info = EmiPort.translatable(KEY + "info", registryLabel(tag.registry()), num(tag.size()));
		context.drawText(EmiPort.literal(fit(info.getString(), detailW)), detailX, listY + 23, PANEL_SUBTEXT);

		int gridX = detailX;
		memberScroll.update(contentHeightOfMembers(), gridH);
		raw.enableScissor(gridX, gridY, gridX + detailW - BAR, gridY + gridH);
		int rows = (stackMembers.size() + columns - 1) / columns;
		for (int i = 0; i < stackMembers.size(); i++) {
			int x = gridX + 1 + (i % columns) * SLOT;
			int y = gridY + 1 + (i / columns) * SLOT - memberScroll.scroll;
			if (y + SLOT < gridY || y > gridY + gridH) {
				continue;
			}
			TagMember member = stackMembers.get(i);
			EmiTexture.SLOT.render(raw, x, y, delta);
			context.drawStack(member.stack(), x + 1, y + 1, delta, EmiIngredient.RENDER_ICON);
			if (mouseX >= x && mouseX < x + SLOT && mouseY >= Math.max(y, gridY) && mouseY < Math.min(y + SLOT, gridY + gridH)
					&& mouseX < gridX + detailW - BAR) {
				hoveredMember = member;
				EmiRenderHelper.drawSlotHightlight(context, x + 1, y + 1, 16, 16, 0);
			}
		}
		int textTop = gridY + 1 + rows * SLOT + (rows > 0 ? 4 : 0) - memberScroll.scroll;
		for (int i = 0; i < textMembers.size(); i++) {
			int y = textTop + i * TEXT_ROW;
			if (y + TEXT_ROW < gridY || y > gridY + gridH) {
				continue;
			}
			TagMember member = textMembers.get(i);
			boolean hovered = mouseX >= gridX && mouseX < gridX + detailW - BAR && mouseY >= Math.max(y, gridY)
				&& mouseY < Math.min(y + TEXT_ROW, gridY + gridH);
			if (hovered) {
				hoveredMember = member;
				context.fill(gridX, y, detailW - BAR, TEXT_ROW, 0x55FFFFFF);
			}
			int textW = detailW - BAR - 4;
			String label = fit(member.label(), textW);
			context.drawText(EmiPort.literal(label), gridX + 2, y + 1, PANEL_TEXT);
			int idX = textRenderer.getWidth(label) + 8;
			if (!member.label().equals(member.id()) && idX + textRenderer.getWidth(member.id()) <= textW) {
				context.drawText(EmiPort.literal(member.id()), gridX + 2 + idX, y + 1, PANEL_SUBTEXT);
			}
		}
		raw.disableScissor();
		if (stackMembers.isEmpty() && textMembers.isEmpty()) {
			Text empty = EmiPort.translatable(KEY + "empty");
			context.drawText(empty, detailX, gridY + 4, PANEL_SUBTEXT);
		}
		memberScroll.render(context, detailX + detailW - BAR, gridY, gridH, mouseX, mouseY);
	}

	private void renderFooter(EmiDrawContext context) {
		int width = panelW - PAD * 2;
		if (status != null && System.currentTimeMillis() < statusUntil) {
			context.drawText(EmiPort.literal(fit(status.getString(), width)), panelX + PAD, footerY + 1, 0x8B0000);
			return;
		}
		if (hoveredMember != null) {
			List<TagEntry> tags = memberTags(hoveredMember);
			if (tags.isEmpty()) {
				context.drawText(EmiPort.translatable(KEY + "member.none", hoveredMember.id()), panelX + PAD, footerY + 1, PANEL_TEXT);
				return;
			}
			context.drawText(EmiPort.literal(fit(
				EmiPort.translatable(KEY + "member.in", hoveredMember.id(), tags.size()).getString(), width)),
				panelX + PAD, footerY + 1, PANEL_TEXT);
			StringBuilder ids = new StringBuilder();
			for (TagEntry tag : tags) {
				if (ids.length() > 0) {
					ids.append(", ");
				}
				ids.append('#').append(tag.id());
			}
			context.drawText(EmiPort.literal(fit(ids.toString(), width)), panelX + PAD, footerY + 11, PANEL_SUBTEXT);
			return;
		}
		context.drawText(EmiPort.literal(fit(EmiPort.translatable(KEY + "hint").getString(), width)),
			panelX + PAD, footerY + 1, PANEL_SUBTEXT);
		context.drawText(EmiPort.literal(fit(EmiPort.translatable(KEY + "hint.2").getString(), width)),
			panelX + PAD, footerY + 11, PANEL_SUBTEXT);
	}

	private List<TagEntry> memberTags(TagMember member) {
		return catalog.tagsContaining(selected == null ? null : selected.registry(), member.id());
	}

	private void renderTooltips(EmiDrawContext context, int mouseX, int mouseY) {
		if (hoveredMember != null) {
			List<TooltipComponent> tooltip = new ArrayList<>();
			if (hoveredMember.stack() != null) {
				tooltip.addAll(hoveredMember.stack().getTooltip());
			} else {
				tooltip.add(EmiTooltipComponents.of(EmiPort.literal(hoveredMember.label())));
				if (!hoveredMember.label().equals(hoveredMember.id())) {
					tooltip.add(EmiTooltipComponents.of(EmiPort.literal(hoveredMember.id(), Formatting.GRAY)));
				}
			}
			List<TagEntry> tags = memberTags(hoveredMember);
			tooltip.add(EmiTooltipComponents.of(tags.isEmpty()
				? EmiPort.translatable(KEY + "member.none.short").formatted(Formatting.GRAY)
				: EmiPort.translatable(KEY + "member.in.short", tags.size()).formatted(Formatting.GRAY)));
			for (int i = 0; i < Math.min(tags.size(), 6); i++) {
				tooltip.add(EmiTooltipComponents.of(EmiPort.literal("#" + tags.get(i).id(), Formatting.DARK_AQUA)));
			}
			if (tags.size() > 6) {
				tooltip.add(EmiTooltipComponents.of(EmiPort.translatable(KEY + "more", tags.size() - 6).formatted(Formatting.GRAY)));
			}
			EmiRenderHelper.drawTooltip(this, context, tooltip, mouseX, mouseY, Math.min(width / 2 - 16, 260));
		} else if (hoveredTag >= 0 && hoveredTag < results.size()) {
			TagEntry tag = results.get(hoveredTag);
			List<TooltipComponent> tooltip = new ArrayList<>();
			tooltip.add(EmiTooltipComponents.of(EmiPort.literal(title(tag))));
			tooltip.add(EmiTooltipComponents.of(EmiPort.literal("#" + tag.id(), Formatting.AQUA)));
			tooltip.add(EmiTooltipComponents.of(EmiPort.translatable(KEY + "info", registryLabel(tag.registry()),
				num(tag.size())).formatted(Formatting.GRAY)));
			if (tag.size() == 0) {
				tooltip.add(EmiTooltipComponents.of(EmiPort.translatable(KEY + "empty.tag").formatted(Formatting.RED)));
			}
			tooltip.add(EmiTooltipComponents.of(EmiPort.translatable(KEY + "tag.copy").formatted(Formatting.DARK_GRAY)));
			EmiRenderHelper.drawTooltip(this, context, tooltip, mouseX, mouseY, Math.min(width / 2 - 16, 260));
		} else if (overChipClear) {
			EmiRenderHelper.drawTooltip(this, context,
				List.of(EmiTooltipComponents.of(EmiPort.translatable(KEY + "containing.clear"))), mouseX, mouseY);
		}
	}

	private static Text tagName(TagEntry tag) {
		try {
			Identifier registry = Identifier.tryParse(tag.registry());
			Identifier id = Identifier.tryParse(tag.id());
			if (registry != null && id != null) {
				RegistryKey<? extends net.minecraft.registry.Registry<Object>> key = RegistryKey.ofRegistry(registry);
				return EmiTagKey.of(TagKey.of(key, id)).getTagName();
			}
		} catch (RuntimeException e) {
			// fall through to the plain id
		}
		return EmiPort.literal("#" + tag.id());
	}

	/** A recessed dark box, like the lists of EMI's config screen. */
	private static void drawInset(EmiDrawContext context, int x, int y, int w, int h) {
		context.fill(x, y, w, h, 0xFF373737);
		context.fill(x + w - 1, y, 1, h, 0xFFFFFFFF);
		context.fill(x, y + h - 1, w, 1, 0xFFFFFFFF);
		context.fill(x + 1, y + 1, w - 2, h - 2, 0xFF1E242B);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			searchFocused = search.isFocused();
			return true;
		}
		EmiPort.focus(search, false);
		searchFocused = false;
		if (overChipClear && button == 0) {
			filter = filter.withMember(null);
			refilter();
			return true;
		}
		if (tagScroll.mouseClicked(mouseX, mouseY, button) || memberScroll.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (hoveredTag >= 0 && hoveredTag < results.size() && button == 0) {
			TagEntry tag = results.get(hoveredTag);
			if (Screen.hasControlDown()) {
				copy("#" + tag.id());
			} else {
				select(tag);
			}
			return true;
		}
		TagMember member = hoveredMember;
		if (member != null) {
			memberClicked(member, button);
			return true;
		}
		return false;
	}

	private void memberClicked(TagMember member, int button) {
		if (Screen.hasControlDown()) {
			copy(member.id());
		} else if (button == 2 || (button == 0 && Screen.hasShiftDown())) {
			filterByMember(member);
		} else if (member.stack() != null && EmiConfig.viewRecipes.matchesMouse(button)) {
			openRecipes(member, false);
		} else if (member.stack() != null && EmiConfig.viewUses.matchesMouse(button)) {
			openRecipes(member, true);
		}
	}

	private void filterByMember(TagMember member) {
		String registry = selected == null ? null : selected.registry();
		filter = filter.withMember(new MemberRef(registry, member.id()));
		refilter();
		select(results.isEmpty() ? selected : results.get(0));
	}

	private void copy(String text) {
		client.keyboard.setClipboard(text);
		setStatus(EmiPort.translatable(KEY + "copied", text));
	}

	private void openRecipes(TagMember member, boolean uses) {
		EmiStack stack = member.stack();
		if (stack == null) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (previous != null) {
			client.setScreen(previous);
		}
		if (uses) {
			EmiApi.displayUses(stack);
		} else {
			EmiApi.displayRecipes(stack);
		}
		if (client.currentScreen instanceof RecipeScreen) {
			EmiHistory.push(this);
		} else {
			client.setScreen(this);
			setStatus(EmiPort.translatable(KEY + (uses ? "no_uses" : "no_recipes"), stack.getName()));
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (tagScroll.mouseDragged(mouseY) || memberScroll.mouseDragged(mouseY)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		tagScroll.dragging = false;
		memberScroll.dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
		if (registryButton.isMouseOver(mouseX, mouseY)) {
			cycleRegistry(amount > 0);
			return true;
		}
		if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
			tagScroll.scroll -= (int) (amount * ROW * 2);
			tagScroll.clamp();
			return true;
		}
		if (mouseX >= detailX && mouseX < detailX + detailW && mouseY >= gridY && mouseY < gridY + gridH) {
			memberScroll.scroll -= (int) (amount * SLOT * 2);
			memberScroll.clamp();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, amount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
			moveSelection(keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
			moveSelection(keyCode == GLFW.GLFW_KEY_PAGE_DOWN ? Math.max(1, listH / ROW - 1) : -Math.max(1, listH / ROW - 1));
			return true;
		}
		if (EmiConfig.focusSearch.matchesKey(keyCode, scanCode)) {
			EmiPort.focus(search, true);
			setFocused(search);
			searchFocused = true;
			return true;
		}
		if (!search.isFocused()) {
			if (client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
				close();
				return true;
			}
			TagMember member = hoveredMember;
			if (member != null && member.stack() != null) {
				if (EmiConfig.viewRecipes.matchesKey(keyCode, scanCode)) {
					openRecipes(member, false);
					return true;
				} else if (EmiConfig.viewUses.matchesKey(keyCode, scanCode)) {
					openRecipes(member, true);
					return true;
				}
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void moveSelection(int delta) {
		if (results.isEmpty()) {
			return;
		}
		int index = selected == null ? -1 : results.indexOf(selected);
		int next = MathHelper.clamp(index + delta, 0, results.size() - 1);
		select(results.get(next));
		if (next * ROW < tagScroll.scroll) {
			tagScroll.scroll = next * ROW;
		} else if ((next + 1) * ROW + 2 > tagScroll.scroll + listH - 2) {
			tagScroll.scroll = (next + 1) * ROW + 2 - (listH - 2);
		}
		tagScroll.clamp();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		client.setScreen(previous);
	}

	// ------------------------------------------------------------------ scrolling

	/** Vertical scroll position of one area with a draggable bar. */
	private static final class Scroll {
		int scroll;
		boolean dragging;
		private int content;
		private int view;
		private int barX, barY, barH;
		private int grab;

		void update(int content, int view) {
			this.content = content;
			this.view = view;
			clamp();
		}

		int max() {
			return Math.max(0, content - view);
		}

		void clamp() {
			scroll = MathHelper.clamp(scroll, 0, max());
		}

		private int thumbHeight() {
			return MathHelper.clamp(barH * view / Math.max(1, content), 12, barH);
		}

		private int thumbY() {
			int max = max();
			return max == 0 ? barY : barY + (barH - thumbHeight()) * scroll / max;
		}

		void render(EmiDrawContext context, int x, int y, int h, int mouseX, int mouseY) {
			barX = x;
			barY = y;
			barH = h;
			if (max() == 0) {
				return;
			}
			context.fill(x, y, BAR, h, 0x66000000);
			boolean hot = dragging || (mouseX >= x && mouseX < x + BAR && mouseY >= y && mouseY < y + h);
			context.fill(x + 1, thumbY(), BAR - 2, thumbHeight(), hot ? 0xFFE0E0E0 : 0xFFA0A0A0);
		}

		boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button != 0 || max() == 0 || mouseX < barX || mouseX >= barX + BAR || mouseY < barY || mouseY >= barY + barH) {
				return false;
			}
			int top = thumbY();
			grab = mouseY >= top && mouseY < top + thumbHeight() ? (int) mouseY - top : thumbHeight() / 2;
			dragging = true;
			mouseDragged(mouseY);
			return true;
		}

		boolean mouseDragged(double mouseY) {
			if (!dragging) {
				return false;
			}
			int travel = Math.max(1, barH - thumbHeight());
			scroll = (int) Math.round((mouseY - grab - barY) * max() / travel);
			clamp();
			return true;
		}
	}
}
