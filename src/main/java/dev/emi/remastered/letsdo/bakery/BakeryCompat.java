package dev.emi.remastered.letsdo.bakery;

import java.util.List;
import java.util.Optional;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.letsdo.LetsDoContext;
import dev.emi.remastered.letsdo.LetsDoLayouts;
import dev.emi.remastered.letsdo.LetsDoRecipe;
import dev.emi.remastered.letsdo.LetsDoSubModule;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Bakery 2.1.10. The mod's own EMI plugin shows the baking station and the jam pot; cutting cakes with a knife is
 * missing there. The recipes come from {@link CakeCuts} and the item registry, so this class uses no Bakery class.
 */
public class BakeryCompat implements LetsDoSubModule {

	@Override
	public void register(LetsDoContext ctx) {
		ctx.run("cake_cut", () -> cakeCut(ctx));
	}

	private static void cakeCut(LetsDoContext ctx) {
		var knives = LetsDoContext.tag(LetsDoContext.itemTag(CakeCuts.NAMESPACE, "knives"));
		var category = ctx.addCategory(LetsDoContext.id("cake_cut"), EmiStack.of(Items.CAKE), knives);
		for (CakeCuts.Cut cut : CakeCuts.CUTS) {
			Optional<Item> whole = LetsDoContext.item(CakeCuts.NAMESPACE, cut.whole());
			Optional<Item> slice = LetsDoContext.item(CakeCuts.NAMESPACE, cut.slice());
			if (whole.isEmpty() || slice.isEmpty()) {
				continue;
			}
			ctx.addRecipe(new LetsDoRecipe(category, LetsDoContext.syntheticId("cake_cut", cut.whole()),
					LetsDoLayouts.TOOL_ON_INPUT, List.of(EmiStack.of(whole.get())), List.of(EmiStack.of(slice.get(), CakeCuts.SLICES)),
					List.of(knives), List.of())
					.tooltip(Text.translatable("emi_letsdo_compat.cake_cut.knife")));
		}
	}
}
