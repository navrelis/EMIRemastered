package dev.emi.remastered.devtour.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.remastered.devtour.BenchMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

/**
 * Benchmark only ({@code mergeEnchantments=true} in {@code config/emi_bench.properties}): every enchantment accepts
 * every item, like Merge Enchantments with {@code combineEnchantments: true} in the owner's pack. Same injection as
 * that mod (HEAD, cancellable), so the reload does the same work.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentBenchMixin {
	@Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
	private void emiDevtour$acceptAll(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (BenchMod.mergeEnchantments) {
			cir.setReturnValue(true);
		}
	}
}
