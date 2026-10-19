package dev.emi.remastered.config.screen;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import dev.emi.emi.EmiPort;
import dev.emi.emi.screen.ConfigScreen.Mutator;
import dev.emi.emi.screen.widget.config.ConfigEntryWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Config screen entry for {@code double} settings: a number field, same size and place as EMI's int fields. */
public class DoubleWidget extends ConfigEntryWidget {
	private static final Pattern NUMBER = Pattern.compile("^-?[0-9]*([.,][0-9]*)?$");
	private final TextFieldWidget text;

	public DoubleWidget(Text name, List<TooltipComponent> tooltip, Supplier<String> search, Mutator<Double> mutator) {
		super(name, tooltip, search, 20);
		MinecraftClient client = MinecraftClient.getInstance();
		text = new TextFieldWidget(client.textRenderer, 0, 0, 148, 18, EmiPort.literal(""));
		text.setText(format(mutator.get()));
		text.setTextPredicate(s -> NUMBER.matcher(s).matches());
		text.setChangedListener(s -> {
			try {
				String n = s.replace(',', '.');
				if (n.isEmpty() || n.equals("-") || n.equals(".") || n.equals("-.")) {
					mutator.set(0.0);
				} else {
					mutator.set(Double.parseDouble(n));
				}
			} catch (NumberFormatException e) {
			}
		});
		this.setChildren(List.of(text));
	}

	private static String format(double d) {
		if (d == Math.rint(d) && Math.abs(d) < 1e15) {
			return String.format(Locale.ROOT, "%.1f", d);
		}
		return Double.toString(d);
	}

	@Override
	public void update(int y, int x, int width, int height) {
		text.x = x + width - 150 + 1;
		text.y = y + 1;
	}
}
