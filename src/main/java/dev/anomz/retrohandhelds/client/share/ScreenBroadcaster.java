package dev.anomz.retrohandhelds.client.share;

import java.util.concurrent.locks.LockSupport;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.network.ShareStripPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the picture of the console this player is playing to the server, compressed in strips, so
 * nearby players can see it. Encoding runs on its own thread; packets are sent from the main thread.
 * Nothing is sent when nobody else is near the console.
 */
public final class ScreenBroadcaster {
    private record Target(BlockPos pos, FrameBuffer frames, int fps) {
    }

    @Nullable
    private static volatile Target target;
    private static Thread thread;

    private ScreenBroadcaster() {
    }

    /** Client tick (main thread): decide whether and what to share. */
    public static void tick(Minecraft minecraft) {
        target = computeTarget(minecraft);
        if (target != null && thread == null) {
            thread = new Thread(ScreenBroadcaster::run, "RetroHandhelds-ScreenShare");
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Nullable
    private static Target computeTarget(Minecraft minecraft) {
        BlockPos pos = ConsoleSessions.activeConsolePos();
        FrameBuffer frames = ConsoleSessions.activeFrames();
        if (pos == null || frames == null || minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null || ConsoleSessions.activePaused()
                || !RetroServerConfig.screenSharingEnabled() || !RetroClientConfig.shareMyScreen()) {
            return null;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        double radius = RetroServerConfig.screenShareRadius();
        boolean someoneNear = minecraft.level.players().stream()
                .anyMatch(p -> p != minecraft.player && p.distanceToSqr(center) <= radius * radius);
        return someoneNear ? new Target(pos, frames, RetroServerConfig.screenShareFps()) : null;
    }

    private static void run() {
        ScreenStrips.Encoder encoder = new ScreenStrips.Encoder();
        int[] pixels = new int[FrameBuffer.PIXELS];
        int[] size = {FrameBuffer.WIDTH, FrameBuffer.HEIGHT};
        Target last = null;
        long lastFrame = -1;
        while (true) {
            Target current = target;
            try {
                if (current == null) {
                    last = null;
                    LockSupport.parkNanos(200_000_000L);
                    continue;
                }
                if (last == null || !current.pos().equals(last.pos()) || current.frames() != last.frames()) {
                    // New console or new game: everyone nearby needs the full picture.
                    encoder.reset();
                    lastFrame = -1;
                }
                last = current;
                long frame = current.frames().copyIfNewer(pixels, lastFrame, size);
                if (frame != lastFrame) {
                    lastFrame = frame;
                    BlockPos pos = current.pos();
                    int width = size[0];
                    int height = size[1];
                    encoder.encode(pixels, width, height,
                            (strip, data) -> send(new ShareStripPayload(pos, width, height, strip, data)));
                }
                LockSupport.parkNanos(1_000_000_000L / Math.max(1, current.fps()));
            } catch (Throwable t) {
                RetroHandhelds.LOGGER.error("Retro Handhelds: screen sharing error", t);
                LockSupport.parkNanos(1_000_000_000L);
            }
        }
    }

    private static void send(ShareStripPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(payload);
            }
        });
    }
}
