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

/**
 * Server to client: one strip of another player's game, from a console ({@code player} is -1) or from
 * the handheld in that player's hand ({@code player} is their entity id, set by the server).
 */
public record ScreenStripPayload(int player, BlockPos console, int width, int height, int strip, byte[] data)
        implements CustomPacketPayload {
    public static final Type<ScreenStripPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "screen_strip"));
    public static final StreamCodec<ByteBuf, ScreenStripPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScreenStripPayload::player,
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
    public static void handle(ScreenStripPayload p, IPayloadContext context) {
        RemoteScreens.receive(p.player(), p.console(), p.width(), p.height(), p.strip(), p.data());
    }
}
