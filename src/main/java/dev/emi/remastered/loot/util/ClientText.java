package dev.emi.remastered.loot.util;

import dev.emi.emi.api.render.EmiTooltipComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
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

    public static OrderedText trim(Text text, int width) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        StringVisitable stringVisitable = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS));
        return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS));
    }
}
