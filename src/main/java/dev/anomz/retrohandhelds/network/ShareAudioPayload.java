package dev.anomz.retrohandhelds.network;

import dev.anomz.retrohandhelds.RetroHandhelds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: about 93 ms of the sound of the console this player is playing (encoded, opaque to the server). */
public record ShareAudioPayload(BlockPos console, byte[] data) implements CustomPacketPayload {
    public static final Type<ShareAudioPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "share_audio"));
    public static final StreamCodec<ByteBuf, ShareAudioPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShareAudioPayload::console,
            ByteBufCodecs.byteArray(ScreenShare.AUDIO_PACKET_BYTES), ShareAudioPayload::data,
            ShareAudioPayload::new);

    @Override
    public Type<ShareAudioPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(ShareAudioPayload payload, IPayloadContext context) {
        if (payload.data().length != ScreenShare.AUDIO_PACKET_BYTES) {
            return;
        }
        ShareRelay.relay(context, payload.console(), payload.data().length,
                new AudioChunkPayload(payload.console(), payload.data()));
    }
}
