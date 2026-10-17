package dev.anomz.retrohandhelds.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: one strip of the picture of the console this player is playing. The server
 * only checks and forwards it to nearby players; it never decodes it.
 */
public record ShareStripPayload(BlockPos console, int width, int height, int strip, byte[] data)
        implements CustomPacketPayload {
    public static final Type<ShareStripPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "share_strip"));
    public static final StreamCodec<ByteBuf, ShareStripPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShareStripPayload::console,
            ByteBufCodecs.VAR_INT, ShareStripPayload::width,
            ByteBufCodecs.VAR_INT, ShareStripPayload::height,
            ByteBufCodecs.VAR_INT, ShareStripPayload::strip,
            ByteBufCodecs.byteArray(ScreenShare.MAX_DATA), ShareStripPayload::data,
            ShareStripPayload::new);

    // Server thread only; entries are removed on logout.
    private static final Map<UUID, Budget> BUDGETS = new HashMap<>();

    @Override
    public Type<ShareStripPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(ShareStripPayload payload, IPayloadContext context) {
        if (!RetroServerConfig.screenSharingEnabled()
                || !ScreenShare.validStrip(payload.width(), payload.height(), payload.strip(), payload.data().length)
                || !(context.player() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = payload.console();
        Vec3 center = Vec3.atCenterOf(pos);
        if (player.distanceToSqr(center) > ScreenShare.MAX_SENDER_DISTANCE * ScreenShare.MAX_SENDER_DISTANCE
                || !level.isLoaded(pos)
                || !(level.getBlockEntity(pos) instanceof ConsoleBlockEntity console)
                || console.cartridgeData() == null) {
            return;
        }
        Budget budget = BUDGETS.computeIfAbsent(player.getUUID(), id -> new Budget());
        if (!budget.spend(payload.data().length, RetroServerConfig.screenShareBytesPerSecond())) {
            return;
        }
        PacketDistributor.sendToPlayersNear(level, player, center.x, center.y, center.z,
                RetroServerConfig.screenShareRadius(),
                new ScreenStripPayload(pos, payload.width(), payload.height(), payload.strip(), payload.data()));
    }

    public static void forget(Player player) {
        BUDGETS.remove(player.getUUID());
    }

    /** Token bucket: refills at the configured rate, holds at most one second's worth. */
    private static final class Budget {
        private double bytes = -1;
        private long lastNanos;

        boolean spend(int cost, int bytesPerSecond) {
            long now = System.nanoTime();
            bytes = bytes < 0 ? bytesPerSecond
                    : Math.min(bytesPerSecond, bytes + (now - lastNanos) / 1e9 * bytesPerSecond);
            lastNanos = now;
            if (bytes < cost) {
                return false;
            }
            bytes -= cost;
            return true;
        }
    }
}
