package dev.emi.remastered.enchanting;

import java.lang.reflect.Field;
import java.util.List;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import dev.emi.remastered.enchanting.recipe.EnchantmentRecipe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

public class EnchantingDevDriver implements ClientModInitializer {
	private int tick, step, wait, resourceReloads;
	private boolean sawReloading;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void log(String s) {
		EmiLog.info("[ENCH-TEST] " + s);
	}

	private void tick(MinecraftClient client) {
		if (client.world == null) {
			return;
		}
		if (wait > 0) {
			wait--;
			return;
		}
		try {
			run(client);
		} catch (Throwable t) {
			EmiLog.error("[ENCH-TEST] driver failure", t);
			step = 999;
		}
	}

	private EmiStack book(MinecraftClient client, net.minecraft.registry.RegistryKey<Enchantment> key, int level) {
		RegistryEntry<Enchantment> e = client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key).get();
		return EmiStack.of(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(e, level)));
	}

	private void shot(MinecraftClient client, String name) {
		ScreenshotRecorder.saveScreenshot(client.runDirectory, "ench_" + name + ".png", client.getFramebuffer(), t -> log("screenshot " + name));
	}

	@SuppressWarnings("unchecked")
	private void clickNext(MinecraftClient client) throws Exception {
		Field f = RecipeScreen.class.getDeclaredField("currentPage");
		f.setAccessible(true);
		List<WidgetGroup> groups = (List<WidgetGroup>) f.get(client.currentScreen);
		for (WidgetGroup g : groups) {
			if (g.recipe instanceof EnchantmentRecipe) {
				int mx = g.x() + g.getWidth() - 6, my = g.y() + g.getHeight() - 6;
				log("click next at " + mx + "," + my);
				client.currentScreen.mouseClicked(mx, my, 0);
				client.currentScreen.mouseReleased(mx, my, 0);
			}
		}
	}

	private void run(MinecraftClient client) throws Exception {
		switch (step) {
		case 0 -> {
			if (!EmiReloadManager.isLoaded()) return;
			wait = 100;
			step = 1;
		}
		case 1 -> {
			log("STEP1 loaded: category recipes=" + EmiApi.getRecipeManager().getRecipes(EnchantmentRecipe.CATEGORY).size()
				+ " total=" + EmiApi.getRecipeManager().getRecipes().size());
			EmiStack s3 = book(client, Enchantments.SHARPNESS, 3);
			log("lookup sharpness 3 exact: " + EmiApi.getRecipeManager().getRecipesByOutput(s3).size());
			EmiStack s50 = book(client, Enchantments.SHARPNESS, 50);
			log("lookup sharpness 50 exact (expect 0): " + EmiApi.getRecipeManager().getRecipesByOutput(s50).size());
			EmiApi.displayRecipes(s50);
			log("after displayRecipes(sharpness 50): screen=" + client.currentScreen);
			wait = 30;
			step = 2;
		}
		case 2 -> {
			shot(client, "over_max_p1");
			wait = 20;
			step = 3;
		}
		case 3 -> {
			clickNext(client);
			wait = 10;
			step = 4;
		}
		case 4 -> {
			shot(client, "over_max_p2");
			wait = 10;
			step = 5;
		}
		case 5 -> {
			clickNext(client);
			wait = 10;
			step = 6;
		}
		case 6 -> {
			shot(client, "over_max_p3");
			client.setScreen(null);
			// multi enchantment book
			var reg = client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
			net.minecraft.component.type.ItemEnchantmentsComponent.Builder b = new net.minecraft.component.type.ItemEnchantmentsComponent.Builder(net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);
			b.add(reg.getEntry(Enchantments.SHARPNESS).get(), 7);
			b.add(reg.getEntry(Enchantments.LOOTING).get(), 9);
			ItemStack multi = new ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK);
			multi.set(net.minecraft.component.DataComponentTypes.STORED_ENCHANTMENTS, b.build());
			EmiApi.displayRecipes(EmiStack.of(multi));
			log("after displayRecipes(multi): screen=" + client.currentScreen);
			wait = 20;
			step = 7;
		}
		case 7 -> {
			shot(client, "multi");
			client.setScreen(null);
			log("STEP7 reloading resources (F3+T)");
			resourceReloads++;
			client.reloadResources();
			sawReloading = false;
			wait = 5;
			step = 8;
		}
		case 8 -> {
			if (EmiReloadManager.getStatus() == 1 || !EmiReloadManager.isLoaded()) {
				sawReloading = true;
			}
			if (!sawReloading || !EmiReloadManager.isLoaded()) return;
			wait = 20;
			step = 9;
		}
		case 9 -> {
			log("STEP9 after F3+T #" + resourceReloads + ": category recipes=" + EmiApi.getRecipeManager().getRecipes(EnchantmentRecipe.CATEGORY).size());
			EmiStack s50 = book(client, Enchantments.SHARPNESS, 50);
			EmiApi.displayRecipes(s50);
			log("after F3+T displayRecipes(sharpness 50): screen=" + client.currentScreen);
			wait = 30;
			step = 10;
		}
		case 10 -> {
			shot(client, "after_reload");
			client.setScreen(null);
			if (resourceReloads < 3) {
				step = 7;
			} else {
				log("DONE");
				step = 999;
			}
		}
		default -> {
		}
		}
	}
}
