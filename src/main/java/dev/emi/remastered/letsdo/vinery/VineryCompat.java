package dev.emi.remastered.letsdo.vinery;

import java.util.List;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.letsdo.LetsDoContext;
import dev.emi.remastered.letsdo.LetsDoLayouts;
import dev.emi.remastered.letsdo.LetsDoRecipe;
import dev.emi.remastered.letsdo.LetsDoSubModule;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.satisfy.vinery.core.registry.GrapeTypeRegistry;
import net.satisfy.vinery.core.registry.ObjectRegistry;
import net.satisfy.vinery.core.wine.GrapeType;

/**
 * Vinery 1.5.5. The mod's own EMI plugin shows the barrel fermentation and both apple press categories; only the
 * grape mashing in the grapevine pot is missing there.
 */
public class VineryCompat implements LetsDoSubModule {

	@Override
	public void register(LetsDoContext ctx) {
		ctx.run("grape_mashing", () -> grapeMashing(ctx));
	}

	private static void grapeMashing(LetsDoContext ctx) {
		EmiStack pot = EmiStack.of(ObjectRegistry.GRAPEVINE_POT.get());
		var category = ctx.addCategory(LetsDoContext.id("grape_mashing"), pot, pot);
		for (GrapeType type : GrapeTypeRegistry.GRAPE_TYPE_TYPES) {
			Item fruit = type.getFruit();
			Item bottle = type.getBottle();
			if (fruit == null || bottle == null || fruit == Items.AIR || bottle == Items.AIR) {
				continue;
			}
			EmiStack grapes = EmiStack.of(fruit);
			ctx.addRecipe(new LetsDoRecipe(category, LetsDoContext.syntheticId("grape_mashing", type.getId()),
					LetsDoLayouts.USE_ON_BLOCK, List.of(grapes), List.of(EmiStack.of(bottle)), List.of(pot), List.of())
					.tooltip(Text.translatable("emi_letsdo_compat.grape_mashing.click"),
							Text.translatable("emi_letsdo_compat.grape_mashing.jump"),
							Text.translatable("emi_letsdo_compat.grape_mashing.bottle")));
		}
	}
}
