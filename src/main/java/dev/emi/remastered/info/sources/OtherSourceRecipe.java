package dev.emi.remastered.info.sources;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

/**
 * One entry of an {@code emi_sources} file as an EMI recipe of the category {@link OtherSources#CATEGORY}: the {@code from}
 * stacks (display only), an arrow, the output and the wrapped text (three lines at most, the whole text as tooltip).
 * The recipe has an output, so EMI's recipe index, the Item Info page and the exports count it as a source.
 */
final class OtherSourceRecipe implements EmiRecipe {
	static final int WIDTH = 150;
	private static final int SLOT = 18;
	private static final int OUTPUT = 26;
	private static final int PER_ROW = 3;
	private static final int MAX_INPUTS = 6;
	private static final int ARROW_X = 77;
	private static final int LINE = 10;
	private static final int MAX_LINES = 3;
	private static final int TOOLTIP_WIDTH = 220;
	private static final int GAP = 4;
	private static final int GOLD = 0xFFFFD27F;

	private final Identifier id;
	private final List<EmiIngredient> inputs;
	private final EmiStack output;
	private final String text;
	private final Double chance;
	private Layout layout;

	OtherSourceRecipe(Identifier id, List<EmiIngredient> inputs, EmiStack output, String text, Double chance) {
		this.id = id;
		this.inputs = inputs;
		this.output = output;
		this.text = text;
		this.chance = chance;
	}

	/** The wrapped text lines, the tooltip lines (null when nothing was cut) and the pixel height of the page. */
	private record Layout(int width, List<OrderedText> lines, List<TooltipComponent> tooltip, int height) {
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return OtherSources.CATEGORY;
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
		return List.of(output);
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return layout().height();
	}

	private boolean hasInputs() {
		return !inputs.isEmpty();
	}

	private int headerHeight() {
		int rows = (Math.min(inputs.size(), MAX_INPUTS) + PER_ROW - 1) / PER_ROW;
		return Math.max(OUTPUT, rows * SLOT);
	}

	/** Text, wrapped for the current font and language (the page is rebuilt when the language changes). */
	private Layout layout() {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		Layout current = layout;
		if (current != null) {
			return current;
		}
		int width = hasInputs() ? WIDTH : WIDTH - OUTPUT - GAP - 2;
		StringVisitable full = textComponent();
		List<StringVisitable> wrapped = font.getTextHandler().wrapLines(full, width, Style.EMPTY);
		List<OrderedText> lines = new ArrayList<>();
		List<TooltipComponent> tooltip = null;
		int shown = Math.min(wrapped.size(), MAX_LINES);
		for (int i = 0; i < shown; i++) {
			StringVisitable line = wrapped.get(i);
			if (i == shown - 1 && wrapped.size() > MAX_LINES) {
				int dots = font.getWidth("...");
				line = StringVisitable.concat(font.trimToWidth(line, Math.max(0, width - dots)), StringVisitable.plain("..."));
			}
			lines.add(Language.getInstance().reorder(line));
		}
		if (wrapped.size() > MAX_LINES) {
			tooltip = new ArrayList<>();
			for (StringVisitable line : font.getTextHandler().wrapLines(full, TOOLTIP_WIDTH, Style.EMPTY)) {
				tooltip.add(TooltipComponent.of(Language.getInstance().reorder(line)));
			}
		}
		int textHeight = lines.size() * LINE + (chance != null ? LINE + 2 : 0);
		int height = hasInputs() ? headerHeight() + GAP + textHeight : Math.max(OUTPUT, textHeight);
		current = new Layout(width, lines, tooltip, height);
		layout = current;
		return current;
	}

	private Text textComponent() {
		return Language.getInstance().hasTranslation(text) ? EmiPort.translatable(text) : EmiPort.literal(text);
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		layout = null;
		Layout layout = layout();
		int textX;
		int textY;
		if (hasInputs()) {
			int header = headerHeight();
			int shown = Math.min(inputs.size(), MAX_INPUTS);
			int rows = (shown + PER_ROW - 1) / PER_ROW;
			int top = (header - rows * SLOT) / 2;
			for (int i = 0; i < shown; i++) {
				widgets.addSlot(inputs.get(i), (i % PER_ROW) * SLOT, top + (i / PER_ROW) * SLOT).recipeContext(this);
			}
			if (inputs.size() > MAX_INPUTS) {
				widgets.addText(EmiPort.translatable("emi_info.sources.more", inputs.size() - MAX_INPUTS), PER_ROW * SLOT + 2,
					top, 0xFFAAAAAA, true);
			}
			widgets.addTexture(EmiTexture.EMPTY_ARROW, ARROW_X, (header - 17) / 2);
			widgets.addSlot(output, WIDTH - OUTPUT, (header - OUTPUT) / 2).large(true).recipeContext(this);
			textX = 0;
			textY = header + GAP;
		} else {
			widgets.addSlot(output, 0, 0).large(true).recipeContext(this);
			textX = OUTPUT + GAP;
			textY = Math.max(0, (OUTPUT - layout.lines().size() * LINE - (chance != null ? LINE + 2 : 0)) / 2);
		}
		int y = textY;
		for (OrderedText line : layout.lines()) {
			widgets.addText(line, textX, y, -1, true);
			y += LINE;
		}
		if (layout.tooltip() != null) {
			widgets.addTooltip(layout.tooltip(), textX, textY, layout.width(), layout.lines().size() * LINE);
		}
		if (chance != null) {
			widgets.addText(EmiPort.translatable("emi_info.sources.chance", SourceText.percent(chance)), textX, y + 2, GOLD, true);
		}
	}
}
