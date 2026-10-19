package dev.emi.remastered.info.client;

import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.screen.BoMScreen;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.remastered.info.InfoModule;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.providers.BuiltinProviders;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client setup of the {@code info} module (reached only from {@code InfoContent.initClient()}). */
public final class InfoClient {

	private InfoClient() {
	}

	public static void init() {
		BuiltinProviders.ensureRegistered();
		ItemTooltipCallback.EVENT.register(InfoClient::appendHint);
	}

	private static void appendHint(ItemStack stack, Item.TooltipContext context, TooltipType type, List<Text> lines) {
		if (!InfoModule.enabled || !InfoSettings.tooltipHint || stack.isEmpty()) {
			return;
		}
		EmiBind bind = InfoSettings.openInfo;
		Screen screen = MinecraftClient.getInstance().currentScreen;
		if (bind.isBound() && (screen instanceof HandledScreen<?> || screen instanceof RecipeScreen
				|| screen instanceof BoMScreen || screen instanceof InfoScreen)) {
			lines.add(EmiPort.translatable("emi_info.tooltip.hint", bind.getBindText()).formatted(Formatting.DARK_GRAY));
		}
	}
}
