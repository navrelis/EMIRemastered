package dev.emi.remastered.enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(Enchantment.class)
public class EnchantmentThrowMixin {
	@Inject(method = "canBeCombined", at = @At("HEAD"))
	private static void throwTest(RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
		if (first.matchesKey(Enchantments.MENDING) && second.matchesKey(Enchantments.INFINITY)) {
			throw new IllegalStateException("test: canBeCombined throws");
		}
	}

	@Inject(method = "isAcceptableItem", at = @At("HEAD"))
	private void throwTest2(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.isOf(net.minecraft.item.Items.STICK) && ((Enchantment) (Object) this).getMaxLevel() == 2) {
			throw new IllegalStateException("test: isAcceptableItem throws");
		}
	}
}
