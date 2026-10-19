package dev.emi.remastered.loot.util;

import dev.emi.emi.api.render.EmiTooltipComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.ArrayList;
import java.util.List;

/**
 * Text helpers that need client classes. Never load this on a dedicated server.
 */
public class ClientText {

    public static List<TooltipComponent> components(Text title, String namespace) {
        ArrayList<TooltipComponent> components = new ArrayList<>();
        components.add(EmiTooltipComponents.of(title));
        EmiTooltipComponents.appendModName(components, namespace);
        return components;
    }

    public record Wrapped(List<OrderedText> lines, boolean truncated) {
    }

    /** Wraps the text to the width; if it needs more than maxLines the last line is cut and ends with an ellipsis. */
    public static Wrapped wrap(Text text, int width, int maxLines) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<StringVisitable> parts = textRenderer.getTextHandler().wrapLines(text, width, Style.EMPTY);
        boolean truncated = parts.size() > maxLines;
        List<OrderedText> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(parts.size(), maxLines); i++) {
            StringVisitable part = parts.get(i);
            if (truncated && i == maxLines - 1) {
                part = StringVisitable.concat(textRenderer.trimToWidth(part, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS)), ScreenTexts.ELLIPSIS);
            }
            lines.add(Language.getInstance().reorder(part));
        }
        return new Wrapped(lines, truncated);
    }

    public static OrderedText trim(Text text, int width) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        StringVisitable stringVisitable = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS));
        return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS));
    }
}
