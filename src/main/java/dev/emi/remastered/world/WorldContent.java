package dev.emi.remastered.world;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.remastered.world.client.WorldClient;
import dev.emi.remastered.world.emi.WorldEmiRegistration;
import dev.emi.remastered.world.server.WorldServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Entry points of the world module. Nothing reachable from {@link #init()} touches client classes.
 */
public final class WorldContent {

	private WorldContent() {
	}

	/** Runs on the client and on the dedicated server, also when the module is switched off on the client. */
	public static void init() {
		WorldServer.registerPayloads();
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(WorldServer::onSyncDataPackContents);
		ServerLifecycleEvents.SERVER_STARTED.register(WorldServer::prefetch);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorldServer.invalidate());
	}

	/** Client only, called when the module is enabled. */
	public static void initClient() {
		WorldClient.init();
	}

	/** EMI initialisation (stack serializers), only while the module is enabled. */
	public static void initialize(EmiInitRegistry registry) {
		WorldEmiRegistration.initialize(registry);
	}

	/** EMI registration, only while the module is enabled. */
	public static void register(EmiRegistry registry) {
		WorldEmiRegistration.register(registry);
	}
}
