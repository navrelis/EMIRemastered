package dev.emi.remastered.mobs;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.mobs.client.MobsClient;
import dev.emi.remastered.mobs.emi.MobsEmiRegistration;
import dev.emi.remastered.mobs.server.MobsServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Entry points of the mobs module. Nothing reachable from {@link #init()} touches client classes.
 */
public class MobsContent {

	/** Runs on the client and on the dedicated server, also when the module is switched off on the client. */
	public static void init() {
		MobsServer.registerPayloads();
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(MobsServer::onSyncDataPackContents);
	}

	/** Client only, called when the module is enabled. */
	public static void initClient() {
		MobsClient.init();
	}

	public static void register(EmiRegistry registry) {
		MobsEmiRegistration.register(registry);
	}
}
