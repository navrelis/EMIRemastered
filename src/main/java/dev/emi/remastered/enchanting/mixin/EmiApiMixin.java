package dev.emi.remastered.enchanting.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.enchanting.EnchantingContent;

@Mixin(value = EmiApi.class, remap = false)
public class EmiApiMixin {

	/**
	 * "Show recipes" on an enchanted book only finds recipes that have exactly that book as an output. Add the info
	 * recipes of the book's enchantments, so books of any level or with several enchantments open them too.
	 */
	@WrapOperation(method = "displayRecipes", at = @At(value = "INVOKE",
		target = "Ldev/emi/emi/api/EmiApi;pruneSources(Ljava/util/List;Ldev/emi/emi/api/stack/EmiStack;)Ljava/util/List;"))
	private static List<EmiRecipe> emiEnchanting$addInfoRecipes(List<EmiRecipe> recipes, EmiStack context,
			Operation<List<EmiRecipe>> original) {
		return EnchantingContent.withInfoRecipes(original.call(recipes, context), context);
	}
}
