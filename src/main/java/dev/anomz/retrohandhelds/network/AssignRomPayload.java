package dev.anomz.retrohandhelds.network;

import java.util.UUID;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RomNames;
import dev.anomz.retrohandhelds.item.CartridgeData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: "write this ROM file name onto the cartridge in my hand". */
public record AssignRomPayload(boolean offhand, String romFile) implements CustomPacketPayload {
    public static final Type<AssignRomPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "assign_rom"));
    public static final StreamCodec<ByteBuf, AssignRomPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AssignRomPayload::offhand,
            ByteBufCodecs.stringUtf8(RomNames.MAX_LENGTH), AssignRomPayload::romFile,
            AssignRomPayload::new);

    @Override
    public Type<AssignRomPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(AssignRomPayload payload, IPayloadContext context) {
        if (!RomNames.isValid(payload.romFile())) {
            return;
        }
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = context.player().getItemInHand(hand);
        if (!stack.is(RetroRegistries.CARTRIDGE)) {
            return;
        }
        CartridgeData old = stack.get(RetroRegistries.CARTRIDGE_DATA);
        // Same ROM keeps its save; a different ROM gets a fresh save slot.
        String saveId = old != null && old.romFile().equals(payload.romFile()) ? old.saveId() : UUID.randomUUID().toString();
        stack.set(RetroRegistries.CARTRIDGE_DATA, new CartridgeData(payload.romFile(), saveId));
    }
}
