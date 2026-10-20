package dev.emi.remastered.info.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.emi.emi.runtime.EmiHistory;
import net.minecraft.client.gui.screen.Screen;

/** EMI's back history, so switching item view tabs can replace the current entry instead of adding one. */
@Mixin(value = EmiHistory.class, remap = false)
public interface EmiHistoryAccessor {

	@Accessor("HISTORIES")
	static List<Screen> getHistories() {
		throw new AssertionError();
	}
}
