package dev.anomz.retrohandhelds.network;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.share.RemoteAudio;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a chunk of another player's console sound. */
public record AudioChunkPayload(BlockPos console, byte[] data) implements CustomPacketPayload {
    public static final Type<AudioChunkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "audio_chunk"));
    public static final StreamCodec<ByteBuf, AudioChunkPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AudioChunkPayload::console,
            ByteBufCodecs.byteArray(ScreenShare.AUDIO_PACKET_BYTES), AudioChunkPayload::data,
            AudioChunkPayload::new);

    @Override
    public Type<AudioChunkPayload> type() {
        return TYPE;
    }

    /** Runs on the client main thread. RemoteAudio is only touched here, so servers never load it. */
    public static void handle(AudioChunkPayload payload, IPayloadContext context) {
        RemoteAudio.receive(payload.console(), payload.data());
    }
}
