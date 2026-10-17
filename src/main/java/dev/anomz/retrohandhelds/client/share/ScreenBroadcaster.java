package dev.anomz.retrohandhelds.client.share;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.item.CartridgeData;
import dev.anomz.retrohandhelds.item.HandheldItem;
import dev.anomz.retrohandhelds.network.ShareAudioPayload;
import dev.anomz.retrohandhelds.network.ShareStripPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shares the picture and sound of the game this player is playing, on a console or on the handheld in
 * their hand, so nearby players can see and hear it. Encoding runs on its own thread; packets are sent
 * from the main thread. Nothing is sent (and the sound isn't even copied) when nobody else is near.
 */
public final class ScreenBroadcaster {
    /** How often the sharing thread wakes up; sound must be drained at least this often. */
    private static final long LOOP_NANOS = 50_000_000L;

    /** @param pos the console, or {@link BlockPos#ZERO} (unused) for a handheld */
    private record Target(boolean handheld, BlockPos pos, EmulatorSession session, int fps) {
        boolean sameSource(@Nullable Target other) {
            return other != null && handheld == other.handheld && session == other.session && pos.equals(other.pos);
        }
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
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null
                || !RetroServerConfig.screenSharingEnabled() || !RetroClientConfig.shareMyScreen()) {
            return null;
        }
        BlockPos pos = ConsoleSessions.activeConsolePos();
        EmulatorSession session = ConsoleSessions.activeConsoleSession();
        boolean handheld = false;
        Vec3 center;
        if (pos != null && session != null) {
            center = Vec3.atCenterOf(pos);
        } else {
            // A handheld is only shared while it's in a hand, where others can see it.
            ConsoleSessions.Active active = ConsoleSessions.activeHandheld();
            if (active == null || !holds(minecraft.player, active.cartridge())) {
                return null;
            }
            session = active.session();
            pos = BlockPos.ZERO;
            handheld = true;
            center = minecraft.player.position();
        }
        if (session.isPaused()) {
            return null;
        }
        double radius = RetroServerConfig.screenShareRadius();
        boolean someoneNear = minecraft.level.players().stream()
                .anyMatch(p -> p != minecraft.player && p.distanceToSqr(center) <= radius * radius);
        return someoneNear ? new Target(handheld, pos, session, RetroServerConfig.screenShareFps()) : null;
    }

    private static boolean holds(Player player, @Nullable CartridgeData cartridge) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(RetroRegistries.HANDHELD) && Objects.equals(HandheldItem.cartridgeData(stack), cartridge)) {
                return true;
            }
        }
        return false;
    }

    private static void run() {
        ScreenStrips.Encoder strips = new ScreenStrips.Encoder();
        AudioCodec.Encoder sound = new AudioCodec.Encoder();
        AudioCodec.Downmixer downmixer = null;
        int[] pixels = new int[0];
        int[] size = new int[2];
        int[] shared = new int[ScreenStrips.MAX_WIDTH * ScreenStrips.MAX_HEIGHT];
        int[] sharedSize = new int[2];
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
                FrameBuffer frames = current.session().frames();
                if (!current.sameSource(last)) {
                    // New console, handheld or game: everyone nearby needs the full picture and fresh sound.
                    if (pixels.length < frames.capacity()) {
                        pixels = new int[frames.capacity()];
                    }
                    strips.reset();
                    sound.reset();
                    downmixer = null;
                    monoCount = 0;
                    lastFrame = -1;
                    ring.skip(ring.available());
                }
                last = current;
                BlockPos pos = current.pos();
                boolean handheld = current.handheld();

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
                        send(new ShareAudioPayload(handheld, pos, sound.encode(mono, 0)));
                        monoCount -= AudioCodec.PACKET_SAMPLES;
                        System.arraycopy(mono, AudioCodec.PACKET_SAMPLES, mono, 0, monoCount);
                    }
                }

                // Picture: at the configured rate, only strips that changed.
                long now = System.nanoTime();
                if (now >= nextPicture) {
                    nextPicture = now + 1_000_000_000L / Math.max(1, current.fps());
                    long frame = frames.copyIfNewer(pixels, lastFrame, size);
                    if (frame != lastFrame) {
                        lastFrame = frame;
                        ScreenStrips.fit(pixels, frames.stride(), size[0], size[1],
                                frames.aspect(size[0], size[1]), shared, sharedSize);
                        int width = sharedSize[0];
                        int height = sharedSize[1];
                        strips.encode(shared, width, height,
                                (strip, data) -> send(new ShareStripPayload(handheld, pos, width, height, strip, data)));
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
