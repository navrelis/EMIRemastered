package dev.emi.remastered.info.providers;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Built-in provider: marks items and fluids that no provider lists a source for (only the creative inventory or
 * commands give them). Runs last and looks at the sections of all other providers.
 */
public final class NoSourceInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EmiPort.id("emi_info", "no_source");

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public int getOrder() {
		return ORDER_LAST;
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return InfoSettings.showNoSource && (stack.getKey() instanceof Item || stack.getKey() instanceof Fluid);
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		if (collector.hasSource()) {
			return;
		}
		collector.addSection(ID, InfoSection.Kind.OBTAIN, EmiPort.translatable("emi_info.section.no_source", Formatting.DARK_RED))
			.icon(EmiStack.of(Items.BARRIER))
			.source(false)
			.text(EmiPort.translatable("emi_info.no_source"));
	}
}
