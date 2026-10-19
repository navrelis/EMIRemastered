package dev.emi.remastered.info.export;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.remastered.EmiRemastered;
import dev.emi.remastered.info.InfoModule;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The client command {@code /emiexport <recipes|items|sources|nosource|tags|all> [json|csv]} for pack developers
 * (see docs/EXPORT.md). Reached only from {@code InfoContent.initClient()}.
 */
public final class ExportCommand {
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	private static @Nullable ExportJob job;

	private ExportCommand() {
	}

	/** Registers the command and the tick that drives a running export. */
	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommand(dispatcher));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (job != null && job.tick()) {
				job = null;
			}
		});
	}

	private static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("emiexport");
		List<String> whats = new ArrayList<>();
		for (ExportKind kind : ExportKind.values()) {
			whats.add(kind.fileName);
		}
		whats.add(ExportKind.ALL);
		for (String what : whats) {
			LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal(what)
				.executes(context -> run(context.getSource(), what, ExportFormat.JSON));
			for (ExportFormat format : ExportFormat.values()) {
				node.then(ClientCommandManager.literal(format.extension)
					.executes(context -> run(context.getSource(), what, format)));
			}
			root.then(node);
		}
		root.then(ClientCommandManager.literal("cancel").executes(context -> cancel(context.getSource())));
		dispatcher.register(root);
	}

	private static int run(FabricClientCommandSource source, String what, ExportFormat format) {
		Text error = start(what, format);
		if (error != null) {
			source.sendError(error);
			return 0;
		}
		return 1;
	}

	private static int cancel(FabricClientCommandSource source) {
		if (job == null || job.isFinished()) {
			source.sendError(Text.translatable("emi_info.export.error.nothing_running"));
			return 0;
		}
		job.abort(Text.translatable("emi_info.export.cancelled"));
		job = null;
		return 1;
	}

	/** Starts an export; returns the error message when it cannot start. */
	private static @Nullable Text start(String what, ExportFormat format) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!InfoModule.enabled) {
			return Text.translatable("emi_info.export.error.module_off");
		}
		if (client.world == null || !EmiReloadManager.isLoaded()) {
			return Text.translatable("emi_info.export.error.not_loaded");
		}
		if (job != null && !job.isFinished()) {
			return Text.translatable("emi_info.export.error.running");
		}
		List<ExportKind> kinds = ExportKind.parse(what);
		if (kinds.isEmpty()) {
			return Text.translatable("emi_info.export.error.unknown", what);
		}
		Path dir = FabricLoader.getInstance().getGameDir().resolve("emi_export").resolve(LocalDateTime.now().format(STAMP));
		try {
			job = new ExportJob(client, kinds, format, dir);
		} catch (RuntimeException e) {
			EmiRemastered.LOGGER.error("Item info export could not start", e);
			return Text.translatable("emi_info.export.failed", String.valueOf(e.getMessage()));
		}
		say(Text.translatable("emi_info.export.start", what, format.extension));
		return null;
	}

	/** Starts {@code all} as JSON (for the dev tour's {@code call} step). Returns "started" or the error text. */
	public static String startAll() {
		Text error = start(ExportKind.ALL, ExportFormat.JSON);
		return error == null ? "started" : error.getString();
	}

	/** Whether an export is running (for the dev tour). */
	public static boolean isRunning() {
		return job != null && !job.isFinished();
	}

	/** Prints a line to the chat of the local player (or the log when no world is open). */
	static void say(Text text) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(text, false);
		} else {
			EmiRemastered.LOGGER.info("Item info export: {}", text.getString());
		}
	}

	/** The folder as a chat link that opens it with the file manager. */
	static Text pathText(Path dir) {
		String path = dir.toAbsolutePath().toString();
		MutableText text = Text.literal(path);
		return text.styled(style -> style.withColor(Formatting.AQUA).withUnderline(true)
			.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("emi_info.export.open"))));
	}
}
