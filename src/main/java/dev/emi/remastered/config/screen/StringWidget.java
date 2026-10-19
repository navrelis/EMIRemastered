package dev.emi.remastered.config.screen;

import java.util.List;
import java.util.function.Supplier;

import dev.emi.emi.EmiPort;
import dev.emi.emi.screen.ConfigScreen.Mutator;
import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Config screen entry for {@code String} settings: a text field, same size and place as EMI's int fields.
 * Double quotes are not allowed (emi.css stores strings in double quotes without escaping).
 */
public class StringWidget extends ConfigEntryWidget {
	private final TextFieldWidget text;

	public StringWidget(Text name, List<TooltipComponent> tooltip, Supplier<String> search, Mutator<String> mutator) {
		super(name, tooltip, search, 20);
		MinecraftClient client = MinecraftClient.getInstance();
		text = new TextFieldWidget(client.textRenderer, 0, 0, 148, 18, EmiPort.literal(""));
		text.setMaxLength(256);
		String value = mutator.get();
		text.setText(value == null ? "" : value);
		text.setTextPredicate(s -> s.indexOf('"') < 0 && s.indexOf(';') < 0);
		text.setChangedListener(mutator::set);
		this.setChildren(List.of(text));
	}

	@Override
	public void update(int y, int x, int width, int height) {
		text.x = x + width - 150 + 1;
		text.y = y + 1;
	}
}
