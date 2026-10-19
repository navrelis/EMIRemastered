package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.SidebarSide;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The info page of one status effect (EMIffect's {@code MobEffectInfo}): icon, name, description and the sources of
 * the effect. Long pages are split into pages with arrows. Recipe id {@code emiffect:/effects/<ns>/<path>} as in
 * EMIffect 2.1.5.
 */
public class EffectInfoRecipe implements EmiRecipe {
	private final EffectEmiStack stack;
	private final StatusEffect effect;
	private final Identifier id;
	private final List<EffectSection> sections;
	private final List<EmiIngredient> inputs;
	private EffectLayout layout;

	EffectInfoRecipe(EffectEmiStack stack, EffectSources sources) {
		this.stack = stack;
		this.effect = stack.getEffect();
		this.id = recipeId(stack.getId());
		this.sections = EffectSectionBuilder.build(effect, sources, EffectsSettings.showAttributeModifiers,
			EffectsSettings.showPotionVariants);
		List<EmiIngredient> list = new ArrayList<>();
		for (EffectSection section : sections) {
			for (EffectSection.Group group : section.groups()) {
				for (EmiIngredient cell : group.cells()) {
					if (!list.contains(cell)) {
						list.add(cell);
					}
				}
			}
		}
		this.inputs = List.copyOf(list);
	}

	/** Recipe id of the page of the effect with this registry id. */
	static Identifier recipeId(Identifier effectId) {
		return Identifier.of("emiffect", "/effects/" + effectId.getNamespace() + "/" + effectId.getPath());
	}

	/** The blocks of the page (attributes, sources, cures), shared with the Item Info provider. */
	List<EffectSection> sections() {
		return sections;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return EffectsEmi.CATEGORY;
	}

	@Override
	public @Nullable Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(stack);
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return EffectLayout.WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		EffectLayout layout = layout();
		int capacity = maxHeight();
		return layout.totalHeight() <= capacity ? layout.totalHeight() : capacity;
	}

	/** The height EMI's recipe screen leaves for one recipe (same formula as {@code RecipeScreen} and {@code RecipeTab}). */
	static int maxHeight() {
		int screen = MinecraftClient.getInstance().getWindow().getScaledHeight();
		int height = Math.min(EmiConfig.maximumRecipeScreenHeight, screen - 52 - EmiConfig.verticalMargin) - 46;
		if (EmiConfig.workstationLocation == SidebarSide.BOTTOM
				&& (RecipeScreen.resolve != null || !EmiApi.getRecipeManager().getWorkstations(EffectsEmi.CATEGORY).isEmpty())) {
			height -= 23;
		}
		return Math.max(height, 80);
	}

	private EffectLayout layout() {
		if (layout == null) {
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			Text description = Text.translatable(descriptionKey(stack.getId()));
			layout = new EffectLayout(font, font.wrapLines(description, EffectLayout.WIDTH - EffectLayout.ICON_COLUMN), sections);
		}
		return layout;
	}

	/** JEED / EMIffect lang keys, {@code effect.<ns>.<id>.description} first, then the older {@code .desc}. */
	static String descriptionKey(Identifier id) {
		String key = "effect." + id.getNamespace() + "." + id.getPath() + ".description";
		if (I18n.hasTranslation(key)) {
			return key;
		}
		String old = "effect." + id.getNamespace() + "." + id.getPath() + ".desc";
		if (I18n.hasTranslation(old)) {
			return old;
		}
		return "info.emiffect.desc_not_found";
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		EffectLayout layout = layout();
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int[] starts = layout.pageStarts(widgets.getHeight());
		int pages = starts.length;
		int[] current = {0};
		IntSupplier page = () -> current[0];
		PageText text = new PageText(page, pages, font, widgets.getHeight());

		int titleColor = switch (effect.getCategory()) {
			case BENEFICIAL -> Formatting.GREEN.getColorValue();
			case NEUTRAL -> Formatting.GOLD.getColorValue();
			case HARMFUL -> Formatting.RED.getColorValue();
		};
		OrderedText title = stack.getName().asOrderedText();
		int titleWidth = font.getWidth(title);
		for (int p = 0; p < pages; p++) {
			int y;
			if (p == 0) {
				// header: large icon, title and description beside it
				int descHeight = layout.descriptionHeight + 12;
				SlotWidget icon = new PagedSlot(stack, 3, (descHeight - 26) / 2, 0, page).large(true).recipeContext(this);
				widgets.add(icon);
				int titleX = EffectLayout.ICON_COLUMN + (EffectLayout.WIDTH - EffectLayout.ICON_COLUMN - titleWidth) / 2;
				text.add(0, title, titleX, 2, titleColor, true);
				for (int i = 0; i < layout.description.size(); i++) {
					text.add(0, layout.description.get(i), EffectLayout.ICON_COLUMN, 14 + font.fontHeight * i, EffectLayout.TEXT_COLOR, false);
				}
				y = layout.headerHeight;
			} else {
				text.add(p, title, (EffectLayout.WIDTH - titleWidth) / 2, 2, titleColor, true);
				y = EffectLayout.CONTINUATION_HEIGHT;
			}
			int end = p + 1 < pages ? starts[p + 1] : layout.blocks.size();
			for (int i = Math.max(starts[p], 1); i < end; i++) {
				EffectLayout.Block block = layout.blocks.get(i);
				switch (block.kind()) {
					case HEADING -> text.add(p, block.text(), 0, y + 1, EffectLayout.HEADING_COLOR, false);
					case LABEL, LINE -> text.add(p, block.text(), 0, y, EffectLayout.TEXT_COLOR, false);
					case SLOTS -> {
						for (int c = 0; c < block.cells().size(); c++) {
							widgets.add(new PagedSlot(block.cells().get(c), c * 18, y, p, page));
						}
					}
					default -> {
					}
				}
				y += block.height();
			}
		}
		widgets.add(text);
		if (pages > 1) {
			int y = widgets.getHeight() - 12;
			widgets.addButton(2, y, 12, 12, 0, 0, () -> true, (mouseX, mouseY, button) -> current[0] = (current[0] + pages - 1) % pages);
			widgets.addButton(widgets.getWidth() - 14, y, 12, 12, 12, 0, () -> true, (mouseX, mouseY, button) -> current[0] = (current[0] + 1) % pages);
		}
	}

	/** A slot that only exists while its page is shown. */
	private static final class PagedSlot extends SlotWidget {
		private final int pageIndex;
		private final IntSupplier current;

		PagedSlot(EmiIngredient stack, int x, int y, int pageIndex, IntSupplier current) {
			super(stack, x, y);
			this.pageIndex = pageIndex;
			this.current = current;
		}

		private boolean visible() {
			return current.getAsInt() == pageIndex;
		}

		@Override
		public Bounds getBounds() {
			return visible() ? super.getBounds() : Bounds.EMPTY;
		}

		@Override
		public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
			if (visible()) {
				super.render(draw, mouseX, mouseY, delta);
			}
		}

		@Override
		public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
			return visible() ? super.getTooltip(mouseX, mouseY) : List.of();
		}

		@Override
		public boolean mouseClicked(int mouseX, int mouseY, int button) {
			return visible() && super.mouseClicked(mouseX, mouseY, button);
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return visible() && super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	/** All text lines of all pages; draws the ones of the current page and the page counter. */
	private static final class PageText extends Widget {
		private record Line(int page, OrderedText text, int x, int y, int color, boolean shadow) {
		}

		private final List<Line> lines = new ArrayList<>();
		private final IntSupplier current;
		private final int pages;
		private final TextRenderer font;
		private final int height;

		PageText(IntSupplier current, int pages, TextRenderer font, int height) {
			this.current = current;
			this.pages = pages;
			this.font = font;
			this.height = height;
		}

		void add(int page, OrderedText text, int x, int y, int color, boolean shadow) {
			lines.add(new Line(page, text, x, y, color, shadow));
		}

		@Override
		public Bounds getBounds() {
			return Bounds.EMPTY;
		}

		@Override
		public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
			EmiDrawContext context = EmiDrawContext.wrap(draw);
			context.push();
			context.matrices().translate(0, 0, 300);
			int page = current.getAsInt();
			for (Line line : lines) {
				if (line.page() == page) {
					if (line.shadow()) {
						context.drawTextWithShadow(line.text(), line.x(), line.y(), line.color());
					} else {
						context.drawText(line.text(), line.x(), line.y(), line.color());
					}
				}
			}
			if (pages > 1) {
				OrderedText counter = EmiPort.literal((page + 1) + "/" + pages).asOrderedText();
				context.drawText(counter, (EffectLayout.WIDTH - font.getWidth(counter)) / 2, height - 10, EffectLayout.TEXT_COLOR);
			}
			context.pop();
		}
	}
}
