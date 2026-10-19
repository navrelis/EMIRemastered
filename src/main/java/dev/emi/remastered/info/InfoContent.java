package dev.emi.remastered.info;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.info.client.InfoClient;
import dev.emi.remastered.info.export.ExportCommand;

/** Entry points of the {@code info} module (see docs/MODULES.md). */
public final class InfoContent {

	private InfoContent() {
	}

	/** Both sides, always: nothing, the module is client only. */
	public static void init() {
	}

	/** Client, only when the module is enabled at startup. */
	public static void initClient() {
		InfoClient.init();
		ExportCommand.register();
	}

	/** The module has no EMI plugin; kept for the module contract. */
	public static void register(EmiRegistry registry) {
	}
}
