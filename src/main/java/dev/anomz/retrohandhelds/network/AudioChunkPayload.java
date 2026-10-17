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

/**
 * Server to client: a chunk of another player's game sound, from a console ({@code player} is -1) or
 * from the handheld in that player's hand ({@code player} is their entity id, set by the server).
 */
public record AudioChunkPayload(int player, BlockPos console, byte[] data) implements CustomPacketPayload {
    public static final Type<AudioChunkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "audio_chunk"));
    public static final StreamCodec<ByteBuf, AudioChunkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AudioChunkPayload::player,
            BlockPos.STREAM_CODEC, AudioChunkPayload::console,
            ByteBufCodecs.byteArray(ScreenShare.AUDIO_PACKET_BYTES), AudioChunkPayload::data,
            AudioChunkPayload::new);

    @Override
    public Type<AudioChunkPayload> type() {
        return TYPE;
    }

    /** Runs on the client main thread. RemoteAudio is only touched here, so servers never load it. */
    public static void handle(AudioChunkPayload p, IPayloadContext context) {
        RemoteAudio.receive(p.player(), p.console(), p.data());
    }
}
