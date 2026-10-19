package dev.emi.remastered.effects;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The content of an effect page as a list of blocks with their heights. Block 0 is the header (icon, title,
 * description); {@link PagePlanner} splits the list into pages for the height that is available.
 */
final class EffectLayout {
	static final int WIDTH = 144;
	static final int SLOTS_PER_ROW = WIDTH / 18;
	static final int ICON_COLUMN = 31;
	static final int HEADING_HEIGHT = 12;
	static final int LINE_HEIGHT = 10;
	static final int SLOT_ROW_HEIGHT = 19;
	static final int NAV_HEIGHT = 14;
	static final int CONTINUATION_HEIGHT = 14;
	static final int HEADING_COLOR = 0x202020;
	static final int TEXT_COLOR = 0x404040;

	enum Kind {
		HEADER, HEADING, LABEL, LINE, SLOTS
	}

	/** {@code text} for HEADING, LABEL and LINE; {@code cells} for SLOTS. */
	record Block(Kind kind, int height, boolean keepWithNext, OrderedText text, List<EmiIngredient> cells) {
	}

	final List<Block> blocks = new ArrayList<>();
	final int headerHeight;
	/** Height of the description area next to the effect icon. */
	final int descriptionHeight;
	final List<OrderedText> description;
	private final int[] heights;
	private final boolean[] keep;
	private final int total;
	private int cachedCapacity = -1;
	private int[] cachedStarts;

	EffectLayout(TextRenderer font, List<OrderedText> description, List<EffectSection> sections) {
		this.description = description;
		int lineHeight = font.fontHeight;
		this.descriptionHeight = Math.max(description.size() * lineHeight, 30);
		this.headerHeight = 14 + descriptionHeight + 2;
		blocks.add(new Block(Kind.HEADER, headerHeight, false, null, List.of()));
		for (EffectSection section : sections) {
			blocks.add(new Block(Kind.HEADING, HEADING_HEIGHT, true,
				Text.empty().append(section.title()).formatted(Formatting.UNDERLINE).asOrderedText(), List.of()));
			for (EffectSection.Group group : section.groups()) {
				if (group.label() != null) {
					for (OrderedText line : font.wrapLines(group.label(), WIDTH)) {
						blocks.add(new Block(Kind.LABEL, LINE_HEIGHT, true, line, List.of()));
					}
				}
				List<EmiIngredient> cells = group.cells();
				for (int i = 0; i < cells.size(); i += SLOTS_PER_ROW) {
					blocks.add(new Block(Kind.SLOTS, SLOT_ROW_HEIGHT, false, null,
						cells.subList(i, Math.min(cells.size(), i + SLOTS_PER_ROW))));
				}
			}
			for (Text line : section.lines()) {
				for (OrderedText wrapped : font.wrapLines(line, WIDTH)) {
					blocks.add(new Block(Kind.LINE, LINE_HEIGHT, false, wrapped, List.of()));
				}
			}
		}
		heights = new int[blocks.size()];
		keep = new boolean[blocks.size()];
		int sum = 0;
		for (int i = 0; i < heights.length; i++) {
			heights[i] = blocks.get(i).height();
			keep[i] = blocks.get(i).keepWithNext();
			sum += heights[i];
		}
		total = sum;
	}

	/** Height of the page when everything fits on one page. */
	int totalHeight() {
		return total;
	}

	/** Index of the first block of every page for the given available height. */
	int[] pageStarts(int capacity) {
		if (capacity != cachedCapacity) {
			cachedStarts = PagePlanner.plan(heights, keep, capacity, NAV_HEIGHT, CONTINUATION_HEIGHT);
			cachedCapacity = capacity;
		}
		return cachedStarts;
	}
}
