package dev.emi.emi.recipe;

import java.util.List;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;

public class EmiShapedRecipe extends EmiCraftingRecipe {

	public EmiShapedRecipe(ShapedRecipe recipe) {
		super(padIngredients(recipe), EmiStack.of(EmiPort.getOutput(recipe)), EmiPort.getId(recipe), false);
		setRemainders(input, recipe);
	}

	public static void setRemainders(List<EmiIngredient> input, CraftingRecipe recipe) {
		try {
			CraftingInventory inv = EmiUtil.getCraftingInventory();
			for (int i = 0; i < input.size(); i++) {
				if (input.get(i).isEmpty()) {
					continue;
				}
				for (int j = 0; j < input.size(); j++) {
					if (j == i) {
						continue;
					}
					if (!input.get(j).isEmpty()) {
						inv.setStack(j, input.get(j).getEmiStacks().get(0).getItemStack().copy());
					}
				}
				List<EmiStack> stacks = input.get(i).getEmiStacks();
				for (EmiStack stack : stacks) {
					inv.setStack(i, stack.getItemStack().copy());
					CraftingRecipeInput cri = CraftingRecipeInput.create(inv.getWidth(), inv.getHeight(), inv.getHeldStacks());
					if (cri.getWidth() <= 3 && cri.getHeight() <= 3) {
						// EMI Remastered: index into the cropped input, not the padded grid (KB8)
						boolean[] occupied = new boolean[inv.size()];
						for (int s = 0; s < occupied.length; s++) {
							occupied[s] = !inv.getStack(s).isEmpty();
						}
						int idx = remainderIndex(i, inv.getWidth(), occupied);
						List<ItemStack> remainders = recipe.getRemainder(cri);
						if (idx >= 0 && idx < remainders.size()) {
							ItemStack remainder = remainders.get(idx);
							if (!remainder.isEmpty()) {
								stack.setRemainder(EmiStack.of(remainder));
							}
						}
					}
				}
				inv.clear();
			}
		} catch (Exception e) {
			EmiLog.error("Exception thrown setting remainders for " + EmiPort.getId(recipe), e);
		}
	}

	/**
	 * EMI Remastered: index of a grid slot inside the cropped (bounding box of the occupied slots) crafting input.
	 * Returns -1 if the slot is out of range or not occupied.
	 */
	public static int remainderIndex(int slot, int gridWidth, boolean[] occupied) {
		if (gridWidth <= 0 || slot < 0 || slot >= occupied.length || !occupied[slot]) {
			return -1;
		}
		int minRow = Integer.MAX_VALUE, minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
		for (int s = 0; s < occupied.length; s++) {
			if (occupied[s]) {
				minRow = Math.min(minRow, s / gridWidth);
				minCol = Math.min(minCol, s % gridWidth);
				maxCol = Math.max(maxCol, s % gridWidth);
			}
		}
		return (slot / gridWidth - minRow) * (maxCol - minCol + 1) + (slot % gridWidth - minCol);
	}

	private static List<EmiIngredient> padIngredients(ShapedRecipe recipe) {
		List<EmiIngredient> list = Lists.newArrayList();
		int i = 0;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if (x >= recipe.getWidth() || y >= recipe.getHeight() || i >= recipe.getIngredients().size()) {
					list.add(EmiStack.EMPTY);
				} else {
					list.add(EmiIngredient.of(recipe.getIngredients().get(i++)));
				}
			}
		}
		return list;
	}
}
