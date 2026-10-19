package dev.emi.emi.bom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.ComponentChanges;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * KB7: amounts served by recipe remainders in a progress calculation (aMelonRind/emi 2508321 and e7fd7d9).
 * The tree is an activator rail (6 iron, 2 sticks, 1 redstone torch); sticks come 4 per batch from planks and the
 * torch (redstone + 1 stick) takes its stick from what the first stick node left over. No Minecraft bootstrap: fake
 * stacks and recipes only.
 */
class TreeCostRemainderTest {
	private static final class Fake extends EmiStack {
		private final String key;

		Fake(String key, long amount) {
			this.key = key;
			this.amount = amount;
		}

		@Override
		public EmiStack copy() {
			Fake f = new Fake(key, amount);
			f.chance = chance;
			f.comparison = comparison;
			f.setRemainder(getRemainder());
			return f;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public ComponentChanges getComponentChanges() {
			return null;
		}

		@Override
		public Object getKey() {
			return key;
		}

		@Override
		public Identifier getId() {
			return Identifier.of("kb7", key);
		}

		@Override
		public List<Text> getTooltipText() {
			return List.of();
		}

		@Override
		public List<TooltipComponent> getTooltip() {
			return List.of();
		}

		@Override
		public Text getName() {
			return null;
		}

		@Override
		public void render(DrawContext draw, int x, int y, float delta, int flags) {
		}

		@Override
		public String toString() {
			return key + " x" + amount;
		}
	}

	private record Recipe(String name, List<EmiIngredient> inputs, List<EmiStack> outputs) implements EmiRecipe {
		@Override
		public EmiRecipeCategory getCategory() {
			return null;
		}

		@Override
		public Identifier getId() {
			return Identifier.of("kb7", name);
		}

		@Override
		public List<EmiIngredient> getInputs() {
			return inputs;
		}

		@Override
		public List<EmiStack> getOutputs() {
			return outputs;
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
	}

	private static EmiStack s(String key, long amount) {
		return new Fake(key, amount);
	}

	private final Recipe sticks = new Recipe("sticks", List.of(s("plank", 2)), List.of(s("stick", 4)));
	private final Recipe torch = new Recipe("torch", List.of(s("redstone", 1), s("stick", 1)), List.of(s("torch", 1)));
	private final Recipe rail = new Recipe("rail", List.of(s("iron", 6), s("stick", 2), s("torch", 1)), List.of(s("rail", 6)));
	private final Map<Object, EmiRecipe> recipes = Map.of("stick", sticks, "torch", torch);

	private MaterialNode goal;
	private MaterialNode stickNode, torchNode, torchStickNode;
	private final TreeCost cost = new TreeCost();

	private void resolve(MaterialNode node) {
		EmiRecipe recipe = recipes.get(node.ingredient.getEmiStacks().get(0).getKey());
		if (recipe != null) {
			node.defineRecipe(recipe);
			for (MaterialNode child : node.children) {
				resolve(child);
			}
		}
	}

	@BeforeEach
	void buildTree() {
		goal = new MaterialNode(rail.outputs().get(0));
		goal.defineRecipe(rail);
		for (MaterialNode child : goal.children) {
			resolve(child);
		}
		stickNode = goal.children.get(1);
		torchNode = goal.children.get(2);
		torchStickNode = torchNode.children.get(1);
	}

	/** EmiPlayerInventory's constructor needs the client; progress only reads the map, so skip the constructor. */
	private static EmiPlayerInventory inventory(EmiStack... stacks) throws Exception {
		Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
		EmiPlayerInventory inv = (EmiPlayerInventory) unsafe.allocateInstance(EmiPlayerInventory.class);
		inv.inventory = new HashMap<>();
		for (EmiStack stack : stacks) {
			inv.inventory.put(stack, stack);
		}
		return inv;
	}

	/** What EmiFavorites.countRecipes adds up for a recipe: the old value (totalNeeded) and the fixed one (plus usedRemainder). */
	private static long[] amountOf(MaterialNode node, EmiRecipe recipe) {
		long[] sum = new long[2];
		if (node.recipe != null) {
			if (node.recipe == recipe) {
				sum[0] += node.totalNeeded;
				sum[1] += node.totalNeeded + node.usedRemainder;
			}
			for (MaterialNode child : node.children) {
				long[] c = amountOf(child, recipe);
				sum[0] += c[0];
				sum[1] += c[1];
			}
		}
		return sum;
	}

	@Test
	void plainCostUsesTheRemainderOnce() {
		cost.calculate(goal, 1);
		assertEquals(2, cost.costs.get(s("plank", 1)).amount);
		assertEquals(6, cost.costs.get(s("iron", 1)).amount);
		assertEquals(1, cost.costs.get(s("redstone", 1)).amount);
		// 4 sticks made, 2 + 1 used
		assertEquals(1, cost.remainders.get(s("stick", 1)).amount);
	}

	@Test
	void emptyInventoryCountsTheRemainderServedStick() throws Exception {
		cost.calculateProgress(goal, 1, inventory());
		assertEquals(2, stickNode.totalNeeded);
		assertEquals(0, stickNode.usedRemainder);
		assertEquals(0, torchStickNode.totalNeeded);
		assertEquals(1, torchStickNode.usedRemainder);
		assertEquals(ProgressState.COMPLETED, torchStickNode.progress);
		assertEquals(1, stickNode.neededBatches);
		long[] amount = amountOf(goal, sticks);
		assertEquals(2, amount[0], "old countRecipes value: one stick of the three is missing");
		assertEquals(3, amount[1], "fixed value: the whole tree needs three sticks");
	}

	@Test
	void inventoryIsNotCountedAsRemainder() throws Exception {
		cost.calculateProgress(goal, 1, inventory(s("stick", 1)));
		assertEquals(1, stickNode.totalNeeded);
		assertEquals(0, stickNode.usedRemainder);
		assertEquals(1, torchStickNode.usedRemainder);
		long[] amount = amountOf(goal, sticks);
		assertEquals(1, amount[0]);
		assertEquals(2, amount[1], "three sticks needed, one in the inventory");
		// the inventory stick was used up by the first stick node; the batch left 3 sticks, the torch took one
		assertFalse(cost.invs.containsKey(s("stick", 1)));
		assertEquals(2, cost.remainders.get(s("stick", 1)).amount);
	}

	@Test
	void fullInventoryNeedsNothing() throws Exception {
		cost.calculateProgress(goal, 1, inventory(s("stick", 3), s("torch", 1), s("iron", 6)));
		long[] amount = amountOf(goal, sticks);
		assertEquals(0, amount[0]);
		assertEquals(0, amount[1]);
		assertEquals(ProgressState.COMPLETED, stickNode.progress);
	}

	@Test
	void progressIsResetWhenTheTreeIsRecalculated() throws Exception {
		cost.calculateProgress(goal, 1, inventory());
		assertEquals(1, torchStickNode.usedRemainder);
		// enough sticks in the inventory: the torch's stick comes from the inventory, not from a remainder
		cost.calculateProgress(goal, 1, inventory(s("stick", 3)));
		assertEquals(0, torchStickNode.usedRemainder);
		assertEquals(0, stickNode.usedRemainder);
		assertEquals(ProgressState.COMPLETED, stickNode.progress);
		// back to nothing: the counters are not carried over
		cost.calculateProgress(goal, 1, inventory());
		assertEquals(2, stickNode.totalNeeded);
		assertEquals(1, torchStickNode.usedRemainder);
	}
}
