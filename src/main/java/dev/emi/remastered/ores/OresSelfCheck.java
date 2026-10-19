package dev.emi.remastered.ores;

import dev.emi.remastered.ores.network.OresFeaturesSender;
import dev.emi.remastered.ores.network.payload.S2CSyncBatchPayload;
import dev.emi.remastered.ores.network.payload.S2CSyncStartPayload;
import dev.emi.remastered.ores.sync.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.PlacedFeature;

public class OresSelfCheck {
	public static void run(MinecraftServer server) {
		OresSyncPlan<PlacedFeature> plan = OresFeaturesSender.buildPlan(server, 99);
		OresSyncState<PlacedFeature> state = new OresSyncState<>();
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		S2CSyncStartPayload.encode(buf, new S2CSyncStartPayload(plan.header()));
		int startBytes = buf.readableBytes();
		state.begin(S2CSyncStartPayload.decode(buf).header());
		OresSyncState.Status st = null;
		int maxBatch = 0;
		for (SyncBatch<PlacedFeature> b : plan.batches()) {
			PacketByteBuf bb = new PacketByteBuf(Unpooled.buffer());
			S2CSyncBatchPayload.encode(bb, new S2CSyncBatchPayload(b));
			maxBatch = Math.max(maxBatch, bb.readableBytes());
			st = state.accept(S2CSyncBatchPayload.decode(bb).batch());
		}
		OresSnapshot<PlacedFeature> snap = state.snapshot();
		Ores.LOG.info("SELFCHECK status={} features={} expected={} batches={} startBytes={} maxBatchBytes={}", st, snap.features().size(), snap.expectedFeatures(), plan.batches().size(), startBytes, maxBatch);
		snap.dimensions().values().forEach(d -> Ores.LOG.info("SELFCHECK dim {} y {}..{} biomes={}", d.id(), d.bottomY(), d.topY(), d.biomes().size()));
		for (String f : new String[] {"ore_diamond_large", "ore_diamond", "ore_debris_small", "ore_debris_large", "ore_gold_nether", "ore_coal_upper", "amethyst_geode", "ore_iron_upper"}) {
			Identifier id = Identifier.of("minecraft", f);
			Ores.LOG.info("SELFCHECK feature {} biomes={} dims={}", f, snap.biomesOf(id).size(), snap.dimensionsOf(id).stream().map(d -> d.id().toString()).toList());
		}
	}
}
