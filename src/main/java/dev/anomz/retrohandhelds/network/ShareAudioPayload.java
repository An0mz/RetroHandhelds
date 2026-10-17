package dev.anomz.retrohandhelds.network;

import dev.anomz.retrohandhelds.RetroHandhelds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: about 93 ms of the sound this player is playing, on a console or on the handheld
 * in their hand ({@code console} is ignored then). Encoded; opaque to the server.
 */
public record ShareAudioPayload(boolean handheld, BlockPos console, byte[] data) implements CustomPacketPayload {
    public static final Type<ShareAudioPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "share_audio"));
    public static final StreamCodec<ByteBuf, ShareAudioPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShareAudioPayload::handheld,
            BlockPos.STREAM_CODEC, ShareAudioPayload::console,
            ByteBufCodecs.byteArray(ScreenShare.AUDIO_PACKET_BYTES), ShareAudioPayload::data,
            ShareAudioPayload::new);

    @Override
    public Type<ShareAudioPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(ShareAudioPayload p, IPayloadContext context) {
        if (p.data().length != ScreenShare.AUDIO_PACKET_BYTES) {
            return;
        }
        ShareRelay.relay(context, p.handheld(), p.console(), p.data().length,
                player -> new AudioChunkPayload(player, p.console(), p.data()));
    }
}
