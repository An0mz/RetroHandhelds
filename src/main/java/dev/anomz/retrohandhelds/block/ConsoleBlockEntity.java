package dev.anomz.retrohandhelds.block;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.item.CartridgeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the inserted cartridge. Synced to clients so they know which ROM to run. */
public class ConsoleBlockEntity extends BlockEntity {
    private static final String CARTRIDGE_KEY = "cartridge";

    private ItemStack cartridge = ItemStack.EMPTY;

    public ConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(RetroRegistries.CONSOLE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack cartridge() {
        return cartridge;
    }

    @Nullable
    public CartridgeData cartridgeData() {
        return cartridge.get(RetroRegistries.CARTRIDGE_DATA);
    }

    /** Server side. Returns the cartridge that was inside before, if any. */
    public ItemStack swapCartridge(ItemStack newCartridge) {
        ItemStack old = cartridge;
        cartridge = newCartridge;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            boolean inserted = !newCartridge.isEmpty();
            if (state.hasProperty(ConsoleBlock.CARTRIDGE) && state.getValue(ConsoleBlock.CARTRIDGE) != inserted) {
                // Same block, so this block entity is kept; only the model changes.
                level.setBlock(worldPosition, state.setValue(ConsoleBlock.CARTRIDGE, inserted), Block.UPDATE_ALL);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return old;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always written, even when empty: NeoForge drops update packets whose tag is empty,
        // so an ejection would otherwise never reach the client.
        tag.put(CARTRIDGE_KEY, cartridge.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cartridge = ItemStack.parseOptional(registries, tag.getCompound(CARTRIDGE_KEY));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
