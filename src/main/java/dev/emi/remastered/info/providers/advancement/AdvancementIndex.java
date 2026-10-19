package dev.emi.remastered.info.providers.advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import dev.emi.remastered.info.InfoSettings;
import dev.emi.remastered.info.mixin.ClientAdvancementManagerAccessor;
import dev.emi.remastered.info.providers.advancement.AdvancementMatching.Candidate;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Item to advancements index built from what the server sent to the client (advancements with a display; criteria are
 * not synced, so the match is on the icon item). Client render thread only.
 * <p>
 * The client's progress map is read through a mixin accessor (read only, no listener involved; a missing entry counts as
 * not done). A refresh runs at most once a second and only while someone asks (the open Item Info screen).
 */
public final class AdvancementIndex {
	/** What an entry of the page needs. */
	public record Info(Identifier id, Identifier rootId, AdvancementDisplay display) {
	}

	private static final long REFRESH_MS = 1000;
	private static final Map<Item, List<Candidate<Item, Info>>> NONE = Map.of();

	private static @Nullable ClientAdvancementManager source;
	private static Map<Item, List<Candidate<Item, Info>>> index = NONE;
	private static long fingerprint = 0;
	private static long version = 0;
	private static long lastRefresh = Long.MIN_VALUE;

	private AdvancementIndex() {
	}

	/** Changes whenever the index content changes; -1 while the setting is off. Cheap enough to call every tick. */
	public static long stamp() {
		if (!InfoSettings.showAdvancements) {
			return -1;
		}
		refreshIfDue();
		return version;
	}

	/** The visible advancements whose icon is {@code item}, open ones first. */
	public static List<Candidate<Item, Info>> get(Item item) {
		refreshIfDue();
		return index.getOrDefault(item, List.of());
	}

	/** The next access re-reads the client's advancements (EMI reload). */
	public static void reset() {
		lastRefresh = Long.MIN_VALUE;
	}

	private static @Nullable ClientAdvancementManager handler() {
		ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
		return network == null ? null : network.getAdvancementHandler();
	}

	private static void refreshIfDue() {
		ClientAdvancementManager handler = handler();
		if (handler != source) {
			source = handler;
			lastRefresh = Long.MIN_VALUE;
		}
		long now = Util.getMeasuringTimeMs();
		if (lastRefresh != Long.MIN_VALUE && now - lastRefresh < REFRESH_MS) {
			return;
		}
		lastRefresh = now;
		List<Candidate<Item, Info>> candidates = handler == null ? List.of() : read(handler);
		long print = AdvancementMatching.fingerprint(candidates);
		if (print != fingerprint || index == NONE) {
			fingerprint = print;
			index = AdvancementMatching.index(candidates);
			version++;
		}
	}

	private static List<Candidate<Item, Info>> read(ClientAdvancementManager handler) {
		Map<AdvancementEntry, AdvancementProgress> progresses = ((ClientAdvancementManagerAccessor) handler).emiInfo$getProgresses();
		List<Candidate<Item, Info>> out = new ArrayList<>();
		for (PlacedAdvancement placed : handler.getManager().getAdvancements()) {
			AdvancementEntry entry = placed.getAdvancementEntry();
			AdvancementDisplay display = entry.value().display().orElse(null);
			if (display == null || display.getIcon().isEmpty()) {
				continue;
			}
			AdvancementProgress progress = progresses.get(entry);
			out.add(new Candidate<>(entry.id().toString(), display.getIcon().getItem(), display.getTitle().getString(),
				display.isHidden(), progress != null && progress.isDone(),
				new Info(entry.id(), placed.getRoot().getAdvancementEntry().id(), display)));
		}
		return out;
	}
}
