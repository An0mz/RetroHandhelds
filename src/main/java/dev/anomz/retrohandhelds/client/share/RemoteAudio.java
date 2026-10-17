package dev.anomz.retrohandhelds.client.share;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.audio.AudioOutput;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Sound other players share from their consoles, played from each console's position with distance
 * falloff, like Minecraft's own sounds. Main thread only.
 */
public final class RemoteAudio {
    private static final int MAX_SOURCES = 8;
    private static final long EXPIRE_MS = 3000;
    /** Same falloff as the player's own console: full volume within 4 blocks, silent at 32. */
    private static final float FULL_VOLUME_DISTANCE = 4;
    private static final float SILENT_DISTANCE = 32;
    /** About 1.5 s of shared sound. */
    private static final int RING_SAMPLES = 1 << 15;

    private static final Map<BlockPos, Source> SOURCES = new HashMap<>();
    private static final short[] DECODED = new short[AudioCodec.PACKET_SAMPLES];

    private RemoteAudio() {
    }

    /** A sound packet arrived from the server. */
    public static void receive(BlockPos pos, byte[] data) {
        // Our own game on that console is already playing from the real thing.
        if (!RetroClientConfig.showOtherScreens() || ConsoleSessions.displayAt(pos) != null
                || !AudioCodec.decode(data, DECODED)) {
            return;
        }
        Source source = SOURCES.get(pos);
        if (source == null) {
            if (SOURCES.size() >= MAX_SOURCES) {
                return;
            }
            source = Source.open(pos);
            if (source == null) {
                return;
            }
            SOURCES.put(pos.immutable(), source);
        }
        source.ring.write(DECODED, AudioCodec.PACKET_SAMPLES);
        source.lastUpdate = Util.getMillis();
    }

    /** Client tick: feed OpenAL and drop sources whose player stopped sharing. */
    public static void tick(Minecraft minecraft) {
        if (SOURCES.isEmpty()) {
            return;
        }
        float playerVolume = RetroClientConfig.muted() ? 0
                : RetroClientConfig.volume() / (float) RetroClientConfig.VOLUME_STEPS;
        float gain = minecraft.options.getSoundSourceVolume(SoundSource.RECORDS) * playerVolume;
        long now = Util.getMillis();
        Iterator<Source> it = SOURCES.values().iterator();
        while (it.hasNext()) {
            Source source = it.next();
            if (now - source.lastUpdate > EXPIRE_MS || !RetroClientConfig.showOtherScreens()) {
                source.output.close();
                it.remove();
            } else {
                source.output.pump(gain);
            }
        }
    }

    public static void clear() {
        SOURCES.values().forEach(source -> source.output.close());
        SOURCES.clear();
    }

    private static final class Source {
        private final AudioRingBuffer ring;
        private final AudioOutput output;
        private long lastUpdate = Util.getMillis();

        private Source(AudioRingBuffer ring, AudioOutput output) {
            this.ring = ring;
            this.output = output;
        }

        static Source open(BlockPos pos) {
            AudioRingBuffer ring = new AudioRingBuffer(RING_SAMPLES);
            Vec3 center = Vec3.atCenterOf(pos);
            return AudioOutput.openAt(ring, AudioCodec.SAMPLE_RATE, center.x, center.y, center.z,
                            FULL_VOLUME_DISTANCE, SILENT_DISTANCE)
                    .map(output -> new Source(ring, output))
                    .orElse(null);
        }
    }
}
