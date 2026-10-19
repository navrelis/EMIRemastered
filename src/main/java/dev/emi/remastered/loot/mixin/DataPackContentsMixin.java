package dev.emi.remastered.loot.mixin;

import dev.emi.remastered.loot.parser.LootTableParser;
import net.minecraft.server.DataPackContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataPackContents.class)
public class DataPackContentsMixin {

    @Inject(method = "refresh", at = @At("TAIL"))
    private void emi_loot_postProcessBuildersAfterTagReload(CallbackInfo ci) {
        LootTableParser.postProcess(LootTableParser.PostProcessor.TAG);
    }

}