package dev.emi.remastered.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TextFocusTest {
	private static final class Node {
		final String name;
		final List<Node> children = new ArrayList<>();
		Node focused;
		boolean textField;
		boolean focusedField;

		Node(String name) {
			this.name = name;
		}

		Node add(Node child) {
			children.add(child);
			return this;
		}

		static Node field(String name, boolean focused) {
			Node node = new Node(name);
			node.textField = true;
			node.focusedField = focused;
			return node;
		}
	}

	private static boolean typing(Node root) {
		return TextFocus.isTyping(root, n -> n.children, n -> n.focused, n -> n.textField && n.focusedField);
	}

	@Test
	void nullScreenIsNotTyping() {
		assertFalse(TextFocus.isTyping(null, n -> List.<Object>of(), n -> null, n -> true));
	}

	@Test
	void screenWithoutTextFieldIsNotTyping() {
		Node screen = new Node("screen").add(new Node("button")).add(new Node("slot"));
		assertFalse(typing(screen));
	}

	@Test
	void unfocusedTextFieldIsNotTyping() {
		Node screen = new Node("screen").add(Node.field("name", false));
		assertFalse(typing(screen));
	}

	@Test
	void focusedTextFieldIsTyping() {
		Node screen = new Node("screen").add(new Node("button")).add(Node.field("name", true));
		assertTrue(typing(screen));
	}

	@Test
	void containerBeforeTheFieldDoesNotHideIt() {
		Node container = new Node("list").add(new Node("entry"));
		Node screen = new Node("screen").add(container).add(Node.field("name", true));
		assertTrue(typing(screen));
	}

	@Test
	void nestedFocusedFieldIsTyping() {
		Node inner = new Node("panel").add(new Node("a")).add(Node.field("search", true));
		Node screen = new Node("screen").add(new Node("b")).add(inner);
		assertTrue(typing(screen));
	}

	@Test
	void focusChainFindsFieldThatIsNotListed() {
		Node field = Node.field("hidden", true);
		Node screen = new Node("screen");
		screen.focused = field;
		assertTrue(typing(screen));
	}

	@Test
	void cyclicFocusChainTerminates() {
		Node a = new Node("a");
		Node b = new Node("b");
		a.focused = b;
		b.focused = a;
		assertFalse(typing(a));
	}

	@Test
	void guardOnlyBlocksKeyboardBinds() {
		assertTrue(TextFocus.blocks(true, true));
		assertFalse(TextFocus.blocks(true, false));
		assertFalse(TextFocus.blocks(false, true));
		assertFalse(TextFocus.blocks(false, false));
	}
}
