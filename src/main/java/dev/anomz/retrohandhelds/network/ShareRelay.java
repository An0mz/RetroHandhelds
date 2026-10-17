package dev.anomz.retrohandhelds.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.item.HandheldItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server side of screen and sound sharing: checks a shared packet and forwards it to nearby players.
 * The source is either a console (near the sender, holding a cartridge) or the handheld in the
 * sender's hand. Picture and sound share one bandwidth budget per player. Server thread only.
 */
final class ShareRelay {
    /** Sent in place of a player id when the source is a console. */
    static final int CONSOLE = -1;

    // Entries are removed on logout.
    private static final Map<UUID, Budget> BUDGETS = new HashMap<>();

    private ShareRelay() {
    }

    /**
     * Forwards the packet built by {@code forwarded} (given the sender's entity id for a handheld, or
     * {@link #CONSOLE}) to players near the source, if the source checks out and the sender has
     * budget left for {@code bytes}.
     */
    static void relay(IPayloadContext context, boolean handheld, BlockPos console, int bytes,
            IntFunction<CustomPacketPayload> forwarded) {
        if (!RetroServerConfig.screenSharingEnabled()
                || !(context.player() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center;
        if (handheld) {
            if (!RetroServerConfig.handheldsEnabled() || !holdsLoadedHandheld(player)) {
                return;
            }
            center = player.position();
        } else {
            center = Vec3.atCenterOf(console);
            if (player.distanceToSqr(center) > ScreenShare.MAX_SENDER_DISTANCE * ScreenShare.MAX_SENDER_DISTANCE
                    || !level.isLoaded(console)
                    || !(level.getBlockEntity(console) instanceof ConsoleBlockEntity entity)
                    || entity.cartridgeData() == null) {
                return;
            }
        }
        if (!budgetFor(player).spend(bytes, RetroServerConfig.screenShareBytesPerSecond())) {
            return;
        }
        PacketDistributor.sendToPlayersNear(level, player, center.x, center.y, center.z,
                RetroServerConfig.screenShareRadius(), forwarded.apply(handheld ? player.getId() : CONSOLE));
    }

    private static boolean holdsLoadedHandheld(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(RetroRegistries.HANDHELD) && HandheldItem.cartridgeData(stack) != null) {
                return true;
            }
        }
        return false;
    }

    static void forget(Player player) {
        BUDGETS.remove(player.getUUID());
    }

    private static Budget budgetFor(ServerPlayer player) {
        return BUDGETS.computeIfAbsent(player.getUUID(), id -> new Budget());
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
