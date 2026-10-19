package dev.emi.remastered.tools.client;

import java.util.List;
import java.util.function.Function;

import dev.emi.emi.input.EmiBind;
import dev.emi.remastered.tools.TextFocus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;

/** Minecraft side of {@link TextFocus}: is a text field of the current screen being typed into? */
final class TextInput {
	private TextInput() {
	}

	static boolean isTyping() {
		Screen screen = MinecraftClient.getInstance().currentScreen;
		return screen != null && TextFocus.isTyping((Element) screen, TextInput::children, TextInput::focused, TextInput::isEditing);
	}

	/** {@code function} (EMI's "does this bind match the current event") that answers false for key binds while typing. */
	static Function<EmiBind, Boolean> guard(Function<EmiBind, Boolean> function) {
		return bind -> function.apply(bind) && !TextFocus.blocks(isTyping(), usesKeyboard(bind));
	}

	private static boolean usesKeyboard(EmiBind bind) {
		for (EmiBind.ModifiedKey key : bind.boundKeys) {
			if (!key.isUnbound() && key.key().getCategory() != InputUtil.Type.MOUSE) {
				return true;
			}
		}
		return false;
	}

	private static List<Element> children(Element element) {
		return element instanceof ParentElement parent ? List.copyOf(parent.children()) : List.of();
	}

	private static Element focused(Element element) {
		return element instanceof ParentElement parent ? parent.getFocused() : null;
	}

	private static boolean isEditing(Element element) {
		if (element instanceof TextFieldWidget field) {
			return field.isActive();
		}
		return element instanceof EditBoxWidget box && box.visible && box.isFocused();
	}
}
