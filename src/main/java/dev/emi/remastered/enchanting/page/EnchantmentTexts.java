package dev.emi.remastered.enchanting.page;

import java.util.Optional;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/** Text helpers shared by the pages. */
public final class EnchantmentTexts {
	public static final int BLACK = 0x000000;

	private EnchantmentTexts() {
	}

	/**
	 * The name of an enchantment for the black-on-grey recipe background: the plain name, in black, without the
	 * level. Obfuscation is the one style that is kept, because mods disguise enchantments the player has not
	 * unlocked yet that way.
	 */
	public static Text name(RegistryEntry<Enchantment> entry) {
		try {
			Text name = Enchantment.getName(entry, 1);
			MutableText plain = name.copyContentOnly().formatted(Formatting.BLACK);
			return isObfuscated(name) ? plain.formatted(Formatting.OBFUSCATED) : plain;
		} catch (Throwable t) {
			Identifier id = entry.getKey().map(key -> key.getValue()).orElse(null);
			return Text.literal(String.valueOf(id)).formatted(Formatting.BLACK);
		}
	}

	public static boolean isObfuscated(Text text) {
		return text.visit((style, string) -> style.isObfuscated() ? Optional.of(true) : Optional.empty(), Style.EMPTY).isPresent();
	}
}
