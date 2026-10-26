package dev.emi.remastered.info.mixin;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.info.client.hub.Hub;
import dev.emi.remastered.info.client.hub.HubBar;
import dev.emi.remastered.info.client.hub.HubRules;
import dev.emi.remastered.info.client.hub.HubTabWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

/**
 * The item view tab bar on EMI's recipe screen (see {@link Hub}): remembers the stack of the display call that made
 * the screen, makes room above the category tabs (the panel gives up height at its top only when the screen is too
 * small), draws the plate behind the category tabs and adds the tabs as widgets. EMI's own code paths are unchanged.
 */
@Mixin(RecipeScreen.class)
public abstract class RecipeScreenHubMixin extends Screen {
	@Shadow(remap = false)
	int backgroundWidth;
	@Shadow(remap = false)
	int backgroundHeight;
	@Shadow(remap = false)
	int x;
	@Shadow(remap = false)
	int y;
	@Unique
	private boolean emiInfo$bar;
	@Unique
	private List<HubTabWidget> emiInfo$tabs = List.of();

	protected RecipeScreenHubMixin(Text title) {
		super(title);
	}

	@Inject(method = "<init>", at = @At("TAIL"), remap = false)
	private void emiInfo$attach(HandledScreen<?> old, Map<?, ?> recipes, CallbackInfo ci) {
		Hub.attach((RecipeScreen) (Object) this);
	}

	@Inject(method = "init", at = @At(value = "FIELD", target = "Ldev/emi/emi/screen/RecipeScreen;tabPageSize:I",
		opcode = Opcodes.PUTFIELD, remap = false))
	private void emiInfo$makeRoom(CallbackInfo ci) {
		emiInfo$bar = Hub.of(this) != null && Hub.fits(height);
		emiInfo$tabs = List.of();
		if (emiInfo$bar) {
			int extra = HubRules.extraTop(y);
			backgroundHeight -= extra;
			y += extra;
		}
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void emiInfo$addTabs(CallbackInfo ci) {
		Hub.Context context = emiInfo$bar ? Hub.of(this) : null;
		if (context != null) {
			emiInfo$tabs = HubBar.create(context, width / 2, y - HubRules.PLATE_HEIGHT);
			emiInfo$tabs.forEach(this::addDrawableChild);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", ordinal = 0,
		target = "Ldev/emi/emi/EmiRenderHelper;drawNinePatch(Ldev/emi/emi/runtime/EmiDrawContext;Lnet/minecraft/util/Identifier;IIIIIIII)V"))
	private void emiInfo$drawPlate(DrawContext raw, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!emiInfo$tabs.isEmpty()) {
			HubBar.drawPlate(raw, x, y - HubRules.PLATE_HEIGHT, backgroundWidth, y);
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void emiInfo$drawTooltip(DrawContext raw, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!emiInfo$tabs.isEmpty()) {
			HubBar.renderTooltip(this, raw, emiInfo$tabs, mouseX, mouseY);
		}
	}

	@Inject(method = "getBounds", at = @At("RETURN"), cancellable = true, remap = false)
	private void emiInfo$bounds(CallbackInfoReturnable<Bounds> cir) {
		if (emiInfo$bar) {
			Bounds bounds = cir.getReturnValue();
			int top = y - HubRules.PLATE_HEIGHT - HubRules.TAB_HEIGHT - HubRules.SELECTED_RAISE;
			if (top < bounds.y()) {
				cir.setReturnValue(new Bounds(bounds.x(), top, bounds.width(), bounds.bottom() - top));
			}
		}
	}
}
