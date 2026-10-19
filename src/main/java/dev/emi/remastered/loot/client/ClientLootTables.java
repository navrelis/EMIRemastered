package dev.emi.remastered.loot.client;

import dev.emi.remastered.loot.EMILoot;
import dev.emi.remastered.loot.structure.StructureRef;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ClientLootTables {
    public static final ClientLootTables INSTANCE = new ClientLootTables();

    public final List<LootReceiver> chestLoots = new Vector<>();
    public final List<LootReceiver> blockLoots = new Vector<>();
    public final List<LootReceiver> mobLoots = new Vector<>();
    public final List<LootReceiver> gameplayLoots = new Vector<>();
    public final List<LootReceiver> archaeologyLoots = new Vector<>();

    public void clearLoots() {
        chestLoots.clear();
        blockLoots.clear();
        mobLoots.clear();
        gameplayLoots.clear();
        archaeologyLoots.clear();
    }

    void receiveChestSender(PacketByteBuf buf) {
        try {
            LootReceiver table = ClientChestLootTable.INSTANCE.fromBuf(buf);
            chestLoots.add(table);
            if (EMILoot.Type.CHEST.isDebug()) EMILoot.LOGGER.info("received chest {}", table.getId());
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while receiving Chest Loot Packet");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    /**
     * Structures of chest tables received before the server's structure lookup was done: updates the tables in place,
     * which are the same objects EMI's chest recipes hold. Returns how many tables changed.
     */
    int receiveChestStructures(PacketByteBuf buf) {
        try {
            int count = buf.readVarInt();
            Map<Identifier, List<StructureRef>> structures = new HashMap<>();
            for (int i = 0; i < count; i++) {
                Identifier id = AbstractTextKeyParsingClientLootTable.getIdFromBuf(buf);
                structures.put(id, StructureRef.read(buf));
            }
            int changed = 0;
            synchronized (chestLoots) {
                for (LootReceiver receiver : chestLoots) {
                    if (receiver instanceof ClientChestLootTable chest) {
                        List<StructureRef> refs = structures.get(chest.getId());
                        if (refs != null && !refs.equals(chest.getStructures())) {
                            chest.setStructures(refs);
                            changed++;
                        }
                    }
                }
            }
            EMILoot.LOGGER.info("Received the structures of {} chest loot tables, {} pages updated", structures.size(), changed);
            return changed;
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Could not read the structures of chest loot tables", e);
            return 0;
        }
    }

    void receiveBlockSender(PacketByteBuf buf) {
        try {
            LootReceiver table = ClientBlockLootTable.INSTANCE.fromBuf(buf);
            blockLoots.add(table);
            if (EMILoot.Type.BLOCK.isDebug()) EMILoot.LOGGER.info("received block {}", table.getId());
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while receiving Block Loot Packet");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    void receiveMobSender(PacketByteBuf buf) {
        try {
            LootReceiver table = ClientMobLootTable.INSTANCE.fromBuf(buf);
            mobLoots.add(table);
            if (EMILoot.Type.MOB.isDebug()) EMILoot.LOGGER.info("received mob {}", table.getId());
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while receiving Mob Loot Packet");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    void receiveGameplaySender(PacketByteBuf buf) {
        try {
            LootReceiver table = ClientGameplayLootTable.INSTANCE.fromBuf(buf);
            gameplayLoots.add(table);
            if (EMILoot.Type.GAMEPLAY.isDebug()) EMILoot.LOGGER.info("received gameplay loot: {}", table.getId());
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while receiving Gameplay Loot Packet");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

    void receiveArchaeologySender(PacketByteBuf buf) {
        try {
            LootReceiver table = ClientArchaeologyLootTable.INSTANCE.fromBuf(buf);
            archaeologyLoots.add(table);
            if (EMILoot.Type.ARCHAEOLOGY.isDebug()) EMILoot.LOGGER.info("received archaeology loot: {}", table.getId());
        } catch (Throwable e) {
            EMILoot.LOGGER.error("Critical error encountered while receiving Archaeology Loot Packet");
            EMILoot.LOGGER.error("Thrown Error: ", e);
        }
    }

}