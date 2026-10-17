package dev.anomz.retrohandhelds.network;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.share.RemoteScreens;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: one strip of another player's console screen. */
public record ScreenStripPayload(BlockPos console, int width, int height, int strip, byte[] data)
        implements CustomPacketPayload {
    public static final Type<ScreenStripPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "screen_strip"));
    public static final StreamCodec<ByteBuf, ScreenStripPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScreenStripPayload::console,
            ByteBufCodecs.VAR_INT, ScreenStripPayload::width,
            ByteBufCodecs.VAR_INT, ScreenStripPayload::height,
            ByteBufCodecs.VAR_INT, ScreenStripPayload::strip,
            ByteBufCodecs.byteArray(ScreenShare.MAX_DATA), ScreenStripPayload::data,
            ScreenStripPayload::new);

    @Override
    public Type<ScreenStripPayload> type() {
        return TYPE;
    }

    /** Runs on the client main thread. RemoteScreens is only touched here, so servers never load it. */
    public static void handle(ScreenStripPayload payload, IPayloadContext context) {
        RemoteScreens.receive(payload.console(), payload.width(), payload.height(), payload.strip(), payload.data());
    }
}
