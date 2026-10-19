package dev.emi.remastered.info.providers.advancement;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.api.InfoCollector;
import dev.emi.remastered.info.api.InfoEntry;
import dev.emi.remastered.info.api.InfoLink;
import dev.emi.remastered.info.api.InfoSection;
import dev.emi.remastered.info.api.ItemInfoProvider;
import dev.emi.remastered.info.providers.advancement.AdvancementIndex.Info;
import dev.emi.remastered.info.providers.advancement.AdvancementMatching.Candidate;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Built-in provider: advancements the client knows whose icon is the stack's item (criteria are not synced to the
 * client). Hidden advancements are listed only once done, like the advancement screen. A click opens the advancement
 * screen on the tab of the advancement.
 */
public final class AdvancementInfoProvider implements ItemInfoProvider {
	public static final Identifier ID = EmiPort.id("emi_info", "advancements");
	private static final int MAX_ROWS = 25;

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public int getOrder() {
		return ORDER_DEFAULT + 500;
	}

	@Override
	public String getModuleId() {
		return "info";
	}

	@Override
	public boolean appliesTo(EmiStack stack) {
		return InfoSettings.showAdvancements && stack.getKey() instanceof Item;
	}

	@Override
	public void collect(EmiStack stack, InfoCollector collector) {
		List<Candidate<Item, Info>> matches = AdvancementIndex.get((Item) stack.getKey());
		if (matches.isEmpty()) {
			return;
		}
		InfoSection section = collector.addSection(ID, InfoSection.Kind.DETAILS, EmiPort.translatable("emi_info.section.advancements"))
			.icon(EmiStack.of(Items.KNOWLEDGE_BOOK));
		int shown = Math.min(matches.size(), MAX_ROWS);
		for (int i = 0; i < shown; i++) {
			Candidate<Item, Info> match = matches.get(i);
			AdvancementDisplay display = match.value().display();
			boolean done = match.done();
			Text label = EmiPort.translatable(done ? "emi_info.advancements.done" : "emi_info.advancements.open", display.getTitle())
				.formatted(done ? Formatting.DARK_GREEN : Formatting.DARK_GRAY);
			Text frame = EmiPort.translatable("emi_info.advancements.frame." + display.getFrame().asString())
				.formatted(display.getFrame() == AdvancementFrame.CHALLENGE ? Formatting.DARK_PURPLE : Formatting.DARK_GRAY);
			List<Text> tooltip = new ArrayList<>();
			tooltip.add(display.getDescription().copy().formatted(Formatting.GRAY));
			tooltip.add(EmiPort.translatable(done ? "emi_info.advancements.status.done" : "emi_info.advancements.status.open")
				.formatted(done ? Formatting.GREEN : Formatting.DARK_GRAY));
			InfoEntry entry = section.entry(EmiStack.of(display.getIcon()), label)
				.detail(frame)
				.tooltip(tooltip);
			Identifier id = match.value().id();
			entry.link(() -> open(id));
		}
		if (matches.size() > shown) {
			section.text(EmiPort.translatable("emi_info.advancements.more", matches.size() - shown));
		}
	}

	@Override
	public void onReload() {
		AdvancementIndex.reset();
	}

	/** Opens the advancement screen on the tab of the advancement, returning to the current screen on close. */
	private static boolean open(Identifier id) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler network = client.getNetworkHandler();
		if (network == null) {
			return false;
		}
		ClientAdvancementManager handler = network.getAdvancementHandler();
		PlacedAdvancement placed = handler.getManager().get(id);
		if (placed == null) {
			return false;
		}
		handler.selectTab(placed.getRoot().getAdvancementEntry(), false);
		client.setScreen(new AdvancementsScreen(handler, client.currentScreen));
		return true;
	}
}
