package dev.emi.remastered.tools;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Decides whether the user is typing into a text field, independent of Minecraft's widget classes (so it can be unit
 * tested). The tools key binds (tag browser, copy recipe) must never fire, and never swallow Ctrl+C, while this is true.
 */
public final class TextFocus {
	private static final int MAX_DEPTH = 12;

	private TextFocus() {
	}

	/**
	 * Whether a text field with the keyboard focus is reachable from {@code root}: along the chain of focused children,
	 * or anywhere in the tree (every branch is searched, a container listed before the field does not hide it).
	 *
	 * @param children     all children of a node, empty for leaves
	 * @param focusedChild the focused child of a node or null
	 * @param typing       whether a node is a text field that is visible, editable and focused
	 */
	public static <T> boolean isTyping(T root, Function<T, List<T>> children, Function<T, T> focusedChild, Predicate<T> typing) {
		if (root == null) {
			return false;
		}
		T focused = root;
		for (int i = 0; i < MAX_DEPTH && focused != null; i++) {
			if (typing.test(focused)) {
				return true;
			}
			focused = focusedChild.apply(focused);
		}
		return anywhere(root, children, typing, MAX_DEPTH);
	}

	private static <T> boolean anywhere(T node, Function<T, List<T>> children, Predicate<T> typing, int depth) {
		if (typing.test(node)) {
			return true;
		}
		if (depth <= 0) {
			return false;
		}
		for (T child : children.apply(node)) {
			if (child != null && anywhere(child, children, typing, depth - 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the guard applies to a bind. Key presses are blocked while typing; a bind that only uses mouse buttons is
	 * not. A bind with both kinds is treated as a key bind (EMI does not tell which event it asks about).
	 */
	public static boolean blocks(boolean typing, boolean bindUsesKeyboard) {
		return typing && bindUsesKeyboard;
	}
}
