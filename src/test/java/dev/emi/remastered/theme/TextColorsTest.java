package dev.emi.remastered.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/** Style and colour remap of the {@link TextColors} facade. No Minecraft bootstrap: only text classes. */
class TextColorsTest {
	@AfterEach
	void reset() {
		TextColors.set(TextColorTable.EMPTY);
	}

	private static List<Integer> colors(OrderedText text) {
		List<Integer> out = new ArrayList<>();
		text.accept((index, style, codePoint) -> {
			out.add(style.getColor() == null ? null : style.getColor().getRgb());
			return true;
		});
		return out;
	}

	@Test
	void emptyTableReturnsSameInstances() {
		OrderedText ordered = Text.literal("ab").asOrderedText();
		assertSame(ordered, TextColors.map(ordered));
		assertEquals(0xFF404040, TextColors.map(0xFF404040));
	}

	@Test
	void bakedColourIsRemappedAndUnmappedKept() {
		TextColors.set(TextColorTable.builder().put(0x555555, 0xDDDDDD).build());
		Text text = Text.empty().append(Text.literal("a").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x555555))))
			.append(Text.literal("b").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AA00))))
			.append(Text.literal("c"));
		List<Integer> colors = colors(TextColors.map(text));
		assertEquals(3, colors.size());
		assertEquals(0xDDDDDD, colors.get(0));
		assertEquals(0x00AA00, colors.get(1));
		assertEquals(null, colors.get(2));
	}

	@Test
	void styleWithoutColourIsTheSameInstance() {
		TextColorTable t = TextColorTable.builder().put(0x555555, 0xDDDDDD).build();
		Style plain = Style.EMPTY.withBold(true);
		assertSame(plain, TextColors.remapStyle(t, plain));
	}

	@Test
	void intColourIsRemappedWithAlphaKept() {
		TextColors.set(TextColorTable.builder().put(0x404040, 0xFFFFFF).build());
		assertEquals(0xFFFFFFFF, TextColors.map(0xFF404040));
		assertEquals(0x00FFFFFF, TextColors.map(0x00404040));
		assertEquals(0xFF123456, TextColors.map(0xFF123456));
	}
}
