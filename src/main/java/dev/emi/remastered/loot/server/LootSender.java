package dev.emi.remastered.loot.server;

import com.mojang.serialization.DataResult;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Function;

public interface LootSender<T extends LootBuilder> {
    String getId();
    void send(ServerPlayerEntity player);
    void addBuilder(T builder);
    List<T> getBuilders();
    void build();

    static String getIdToSend(Identifier id) {
        if (id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            if (path.contains("blocks/")) {
                return "b/" + path.substring(7);
            } else if (path.contains("entities/")) {
                return "e/"+ path.substring(9);
            } else if (path.contains("chests/")) {
                return "c/" + path.substring(7);
            } else if (path.contains("gameplay/")) {
                return "g/" + path.substring(9);
            } else if (path.contains("archaeology/")) {
                return "a/" + path.substring(12);
            } else {
                return path;
            }
        }
        return id.toString();
    }

    default void writeItemStack(PacketByteBuf buf, ItemStack stack, World world) {
        buf.writeVarInt(Registries.ITEM.getRawId(stack.getItem()));
        buf.writeVarInt(stack.getCount());
        ComponentChanges changes = stack.getComponentChanges();
        if (changes.isEmpty()) {
            buf.writeBoolean(false);
        } else {
            DataResult<NbtElement> nbtResult = ComponentChanges.CODEC.encodeStart(NbtOps.INSTANCE, changes);
            nbtResult.ifSuccess(nbt -> {
                buf.writeBoolean(true);
                buf.writeNbt(nbt);
            }).ifError(err -> {
                buf.writeBoolean(false);
            });
        }
    }
}