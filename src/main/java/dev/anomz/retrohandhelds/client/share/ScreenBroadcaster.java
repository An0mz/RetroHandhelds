package dev.anomz.retrohandhelds.client.share;

import java.util.concurrent.locks.LockSupport;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.network.ShareAudioPayload;
import dev.anomz.retrohandhelds.network.ShareStripPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shares the picture and sound of the console this player is playing, so nearby players can see and
 * hear it. Encoding runs on its own thread; packets are sent from the main thread. Nothing is sent
 * (and the sound isn't even copied) when nobody else is near the console.
 */
public final class ScreenBroadcaster {
    /** How often the sharing thread wakes up; sound must be drained at least this often. */
    private static final long LOOP_NANOS = 50_000_000L;

    private record Target(BlockPos pos, EmulatorSession session, int fps) {
    }

    @Nullable
    private static volatile Target target;
    /** Main thread only: the session whose sound is currently being copied for sharing. */
    @Nullable
    private static EmulatorSession soundSource;
    private static Thread thread;

    private ScreenBroadcaster() {
    }

    /** Client tick (main thread): decide whether and what to share. */
    public static void tick(Minecraft minecraft) {
        Target next = computeTarget(minecraft);
        EmulatorSession nextSound = next == null ? null : next.session();
        if (nextSound != soundSource) {
            if (soundSource != null) {
                soundSource.setAudioSharing(false);
            }
            if (nextSound != null) {
                nextSound.setAudioSharing(true);
            }
            soundSource = nextSound;
        }
        target = next;
        if (next != null && thread == null) {
            thread = new Thread(ScreenBroadcaster::run, "RetroHandhelds-ScreenShare");
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Nullable
    private static Target computeTarget(Minecraft minecraft) {
        BlockPos pos = ConsoleSessions.activeConsolePos();
        EmulatorSession session = ConsoleSessions.activeConsoleSession();
        if (pos == null || session == null || minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null || session.isPaused()
                || !RetroServerConfig.screenSharingEnabled() || !RetroClientConfig.shareMyScreen()) {
            return null;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        double radius = RetroServerConfig.screenShareRadius();
        boolean someoneNear = minecraft.level.players().stream()
                .anyMatch(p -> p != minecraft.player && p.distanceToSqr(center) <= radius * radius);
        return someoneNear ? new Target(pos, session, RetroServerConfig.screenShareFps()) : null;
    }

    private static void run() {
        ScreenStrips.Encoder strips = new ScreenStrips.Encoder();
        AudioCodec.Encoder sound = new AudioCodec.Encoder();
        AudioCodec.Downmixer downmixer = null;
        int[] pixels = new int[FrameBuffer.PIXELS];
        int[] size = {FrameBuffer.WIDTH, FrameBuffer.HEIGHT};
        short[] stereo = new short[1 << 16];
        short[] mono = new short[AudioCodec.PACKET_SAMPLES * 4];
        int monoCount = 0;
        Target last = null;
        long lastFrame = -1;
        long nextPicture = 0;
        while (true) {
            Target current = target;
            try {
                if (current == null) {
                    last = null;
                    LockSupport.parkNanos(200_000_000L);
                    continue;
                }
                AudioRingBuffer ring = current.session().sharedAudio();
                if (last == null || !current.pos().equals(last.pos()) || current.session() != last.session()) {
                    // New console or new game: everyone nearby needs the full picture and fresh sound.
                    strips.reset();
                    sound.reset();
                    downmixer = null;
                    monoCount = 0;
                    lastFrame = -1;
                    ring.skip(ring.available());
                }
                last = current;
                BlockPos pos = current.pos();

                // Sound: drain everything since last time, mix down, send whole packets.
                int rate = current.session().sampleRate();
                if (downmixer == null && rate > 0) {
                    downmixer = new AudioCodec.Downmixer(rate);
                }
                int available = Math.min(ring.available(), stereo.length) & ~1;
                if (available > 0) {
                    ring.read(stereo, available);
                    if (downmixer != null) {
                        monoCount += downmixer.process(stereo, available, mono, monoCount);
                    }
                    while (monoCount >= AudioCodec.PACKET_SAMPLES) {
                        send(new ShareAudioPayload(pos, sound.encode(mono, 0)));
                        monoCount -= AudioCodec.PACKET_SAMPLES;
                        System.arraycopy(mono, AudioCodec.PACKET_SAMPLES, mono, 0, monoCount);
                    }
                }

                // Picture: at the configured rate, only strips that changed.
                long now = System.nanoTime();
                if (now >= nextPicture) {
                    nextPicture = now + 1_000_000_000L / Math.max(1, current.fps());
                    long frame = current.session().frames().copyIfNewer(pixels, lastFrame, size);
                    if (frame != lastFrame) {
                        lastFrame = frame;
                        int width = size[0];
                        int height = size[1];
                        strips.encode(pixels, width, height,
                                (strip, data) -> send(new ShareStripPayload(pos, width, height, strip, data)));
                    }
                }
                LockSupport.parkNanos(LOOP_NANOS);
            } catch (Throwable t) {
                RetroHandhelds.LOGGER.error("Retro Handhelds: screen sharing error", t);
                LockSupport.parkNanos(1_000_000_000L);
            }
        }
    }

    private static void send(CustomPacketPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(payload);
            }
        });
    }
}
