package dev.emi.emi.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.runtime.EmiReloadLog;
import net.minecraft.util.Identifier;

/**
 * KB2 (a recipe without a category is dropped, not fatal) and the list sharing of KB12. The EmiStack identity itself
 * ({@code TagEmiIngredient.identicalStacks}) needs real stacks and therefore a bootstrapped Minecraft; the interner
 * is tested here with a stand-in element type.
 */
class RecipeBakeSupportTest {

	/** A stack stand-in: equal hash for the same key, identical only if key and amount match. */
	private record Stack(String key, long amount) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Stack s && s.key.equals(key);
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}
	}

	private static RecipeBakeSupport.ListInterner<Stack> interner() {
		return new RecipeBakeSupport.ListInterner<>((a, b) -> a.key.equals(b.key) && a.amount == b.amount);
	}

	@Test
	void identicalListsShareOneInstance() {
		RecipeBakeSupport.ListInterner<Stack> lists = interner();
		List<Stack> first = List.of(new Stack("a", 1), new Stack("b", 1));
		List<Stack> second = new ArrayList<>(List.of(new Stack("a", 1), new Stack("b", 1)));
		assertSame(first, lists.intern(first));
		assertSame(first, lists.intern(second));
		assertSame(first, lists.intern(first));
		assertEquals(1, lists.size());
	}

	@Test
	void listsDifferingInAnyStrictPropertyStaySeparate() {
		RecipeBakeSupport.ListInterner<Stack> lists = interner();
		List<Stack> base = List.of(new Stack("a", 1), new Stack("b", 1));
		List<Stack> amount = List.of(new Stack("a", 1), new Stack("b", 2));
		List<Stack> order = List.of(new Stack("b", 1), new Stack("a", 1));
		List<Stack> shorter = List.of(new Stack("a", 1));
		List<Stack> empty = List.of();
		assertSame(base, lists.intern(base));
		// equal by Stack.equals (key only), but not identical: must not be merged
		assertSame(amount, lists.intern(amount));
		assertSame(order, lists.intern(order));
		assertSame(shorter, lists.intern(shorter));
		assertSame(empty, lists.intern(empty));
		assertEquals(5, lists.size());
		assertNotSame(base, lists.intern(new ArrayList<>(amount)));
		assertSame(amount, lists.intern(new ArrayList<>(amount)));
	}

	@Test
	void nullListsAndElements() {
		RecipeBakeSupport.ListInterner<Stack> lists = interner();
		assertNull(lists.intern(null));
		List<Stack> withNull = new ArrayList<>();
		withNull.add(null);
		withNull.add(new Stack("a", 1));
		List<Stack> withNull2 = new ArrayList<>(withNull);
		List<Stack> noNull = List.of(new Stack("x", 1), new Stack("a", 1));
		assertSame(withNull, lists.intern(withNull));
		assertSame(withNull, lists.intern(withNull2));
		assertSame(noNull, lists.intern(noNull));
	}

	private static EmiRecipe recipe(@Nullable EmiRecipeCategory category) {
		return new EmiRecipe() {
			@Override
			public EmiRecipeCategory getCategory() {
				return category;
			}

			@Override
			public @Nullable Identifier getId() {
				return Identifier.of("test", "broken");
			}

			@Override
			public List<EmiIngredient> getInputs() {
				return List.of();
			}

			@Override
			public List<EmiStack> getOutputs() {
				return List.of();
			}

			@Override
			public int getDisplayWidth() {
				return 0;
			}

			@Override
			public int getDisplayHeight() {
				return 0;
			}

			@Override
			public void addWidgets(WidgetHolder widgets) {
			}
		};
	}

	@Test
	void recipeWithoutCategoryIsDroppedAndLogged() {
		EmiReloadLog.bake();
		EmiRecipe broken = recipe(null);
		assertTrue(RecipeBakeSupport.missingCategory(broken));
		EmiReloadLog.bake();
		assertEquals(1, EmiReloadLog.warningCount);
		String warning = EmiReloadLog.warnings.get(0);
		assertTrue(warning.contains("test:broken"), warning);
		assertTrue(warning.contains(broken.getClass().getName()), warning);
	}

	@Test
	void recipeWithCategoryIsKept() {
		EmiReloadLog.bake();
		EmiRecipeCategory category = new EmiRecipeCategory(Identifier.of("test", "category"),
			(draw, x, y, delta) -> {}, (draw, x, y, delta) -> {}, (a, b) -> 0);
		assertFalse(RecipeBakeSupport.missingCategory(recipe(category)));
		EmiReloadLog.bake();
		assertEquals(0, EmiReloadLog.warningCount);
	}
}
