package dev.emi.remastered.loot.client;

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
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.function.Function;

public interface LootReceiver {
    boolean isEmpty();
    Identifier getId();
    LootReceiver fromBuf(PacketByteBuf buf);

    default ItemStack readItemStack(PacketByteBuf buf) {
       RegistryEntry<Item> byId = Registries.ITEM.getEntry(Registries.ITEM.get(buf.readVarInt()));
        int count = buf.readVarInt();
        if (buf.readBoolean()) {
            NbtElement nbt = buf.readNbt();
            DataResult<ComponentChanges> changesResult = ComponentChanges.CODEC.parse(NbtOps.INSTANCE, nbt);
            ComponentChanges changes = changesResult.mapOrElse(Function.identity(), err -> ComponentChanges.EMPTY);
            if (changes.isEmpty()) {
                return new ItemStack(byId, count);
            } else {
                return new ItemStack(byId, count, changes);
            }
        } else {
            return new ItemStack(byId, count);
        }
    }
}