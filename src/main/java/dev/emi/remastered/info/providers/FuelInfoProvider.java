package dev.emi.remastered.info.providers;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/** Built-in provider: furnace burn time from Fabric's {@link FuelRegistry} (vanilla fuel map plus mod fuels). */
public final class FuelInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EmiPort.id("emi_info", "fuel");
	/** Ticks a furnace needs to smelt one item. */
	private static final int SMELT_TICKS = 200;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public int getOrder() {
		return ORDER_DEFAULT - 10;
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return stack.getKey() instanceof Item;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		Integer ticks = FuelRegistry.INSTANCE.get((Item) stack.getKey());
		if (ticks == null || ticks <= 0) {
			return;
		}
		collector.addSection(ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_info.section.fuel"))
			.icon((draw, x, y, delta) -> EmiTexture.FULL_FLAME.render(draw, x + 1, y + 1, delta))
			.text(EmiPort.translatable("emi_info.fuel.burn_time", EmiRenderHelper.TEXT_FORMAT.format(ticks / 20.0), ticks))
			.text(EmiPort.translatable("emi_info.fuel.smelts", EmiRenderHelper.TEXT_FORMAT.format(ticks / (double) SMELT_TICKS)))
			.link(EmiPort.translatable("emi_info.fuel.open"), InfoLink.usesOf(stack, VanillaEmiRecipeCategories.FUEL));
	}
}
