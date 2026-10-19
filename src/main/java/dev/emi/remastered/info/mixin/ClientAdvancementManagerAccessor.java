package dev.emi.remastered.info.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.network.ClientAdvancementManager;

/** Read access to the client's advancement progress, so the Item Info screen can show done/open without a listener. */
@Mixin(ClientAdvancementManager.class)
public interface ClientAdvancementManagerAccessor {

	@Accessor("advancementProgresses")
	Map<AdvancementEntry, AdvancementProgress> emiInfo$getProgresses();
}
