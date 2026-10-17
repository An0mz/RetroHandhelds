package dev.anomz.retrohandhelds.network;

import dev.anomz.retrohandhelds.RetroHandhelds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: one strip of the picture this player is playing, on a console or on the handheld
 * in their hand ({@code console} is ignored then). The server only checks and forwards it; it never decodes it.
 */
public record ShareStripPayload(boolean handheld, BlockPos console, int width, int height, int strip, byte[] data)
        implements CustomPacketPayload {
    public static final Type<ShareStripPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "share_strip"));
    public static final StreamCodec<ByteBuf, ShareStripPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShareStripPayload::handheld,
            BlockPos.STREAM_CODEC, ShareStripPayload::console,
            ByteBufCodecs.VAR_INT, ShareStripPayload::width,
            ByteBufCodecs.VAR_INT, ShareStripPayload::height,
            ByteBufCodecs.VAR_INT, ShareStripPayload::strip,
            ByteBufCodecs.byteArray(ScreenShare.MAX_DATA), ShareStripPayload::data,
            ShareStripPayload::new);

    @Override
    public Type<ShareStripPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(ShareStripPayload p, IPayloadContext context) {
        if (!ScreenShare.validStrip(p.width(), p.height(), p.strip(), p.data().length)) {
            return;
        }
        ShareRelay.relay(context, p.handheld(), p.console(), p.data().length,
                player -> new ScreenStripPayload(player, p.console(), p.width(), p.height(), p.strip(), p.data()));
    }

    /** Forgets a player's sharing budget (picture and sound) when they log out. */
    public static void forget(Player player) {
        ShareRelay.forget(player);
    }
}
