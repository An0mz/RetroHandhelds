package dev.anomz.retrohandhelds.client.share;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dev.anomz.retrohandhelds.block.TvLayout;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.audio.AudioOutput;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Sound other players share, with distance falloff like Minecraft's own sounds: from each console (or
 * the middle of the TV screen showing it), or from the player holding the handheld, following them as
 * they move. Volume is the player's "other players" volume. Main thread only.
 */
public final class RemoteAudio {
    private static final int MAX_SOURCES = 8;
    private static final long EXPIRE_MS = 3000;
    /** Same falloff as the player's own console: full volume within 4 blocks, silent at 32. */
    private static final float FULL_VOLUME_DISTANCE = 4;
    private static final float SILENT_DISTANCE = 32;
    /** About 1.5 s of shared sound. */
    private static final int RING_SAMPLES = 1 << 15;
    /** A handheld's sound comes from about chest height. */
    private static final double HANDHELD_HEIGHT = 1.2;

    private static final Map<BlockPos, Source> CONSOLES = new HashMap<>();
    private static final Int2ObjectMap<Source> HANDHELDS = new Int2ObjectOpenHashMap<>();
    private static final short[] DECODED = new short[AudioCodec.PACKET_SAMPLES];

    private RemoteAudio() {
    }

    /** A sound packet arrived from the server: from a console, or from the handheld of {@code player} if it isn't -1. */
    public static void receive(int player, BlockPos pos, byte[] data) {
        // Our own game on that console is already playing from the real thing.
        if (!RetroClientConfig.showOtherScreens() || (player < 0 && ConsoleSessions.displayAt(pos) != null)
                || !AudioCodec.decode(data, DECODED)) {
            return;
        }
        Source source = player >= 0 ? HANDHELDS.get(player) : CONSOLES.get(pos);
        if (source == null) {
            if (CONSOLES.size() + HANDHELDS.size() >= MAX_SOURCES) {
                return;
            }
            Vec3 at = player >= 0 ? handheldPosition(player) : consolePosition(pos);
            if (at == null || (source = Source.open(at)) == null) {
                return;
            }
            if (player >= 0) {
                HANDHELDS.put(player, source);
            } else {
                CONSOLES.put(pos.immutable(), source);
            }
        }
        source.ring.write(DECODED, AudioCodec.PACKET_SAMPLES);
        source.lastUpdate = Util.getMillis();
    }

    /** Client tick: feed OpenAL, follow handheld players and drop sources whose player stopped sharing. */
    public static void tick(Minecraft minecraft) {
        if (CONSOLES.isEmpty() && HANDHELDS.isEmpty()) {
            return;
        }
        float playerVolume = RetroClientConfig.otherPlayersMuted() ? 0
                : RetroClientConfig.otherPlayersVolume() / (float) RetroClientConfig.VOLUME_STEPS;
        float gain = minecraft.options.getSoundSourceVolume(SoundSource.RECORDS) * playerVolume;
        long now = Util.getMillis();
        if (minecraft.level != null) {
            // TVs may be placed or broken while the game plays.
            for (Map.Entry<BlockPos, Source> entry : CONSOLES.entrySet()) {
                Vec3 at = TvLayout.soundCenter(minecraft.level, entry.getKey());
                entry.getValue().output.moveTo(at.x, at.y, at.z);
            }
        }
        for (Int2ObjectMap.Entry<Source> entry : HANDHELDS.int2ObjectEntrySet()) {
            Entity player = minecraft.level == null ? null : minecraft.level.getEntity(entry.getIntKey());
            if (player != null) {
                entry.getValue().output.moveTo(player.getX(), player.getY() + HANDHELD_HEIGHT, player.getZ());
            }
        }
        pump(CONSOLES.values(), gain, now);
        pump(HANDHELDS.values(), gain, now);
    }

    private static void pump(Collection<Source> sources, float gain, long now) {
        Iterator<Source> it = sources.iterator();
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

    /** Whether any other player's game can be heard. */
    public static boolean isPlaying() {
        return !CONSOLES.isEmpty() || !HANDHELDS.isEmpty();
    }

    public static void clear() {
        CONSOLES.values().forEach(source -> source.output.close());
        CONSOLES.clear();
        HANDHELDS.values().forEach(source -> source.output.close());
        HANDHELDS.clear();
    }

    private static Vec3 consolePosition(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : TvLayout.soundCenter(minecraft.level, pos);
    }

    private static Vec3 handheldPosition(int player) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(player);
        return entity == null ? null : entity.position().add(0, HANDHELD_HEIGHT, 0);
    }

    private static final class Source {
        private final AudioRingBuffer ring;
        private final AudioOutput output;
        private long lastUpdate = Util.getMillis();

        private Source(AudioRingBuffer ring, AudioOutput output) {
            this.ring = ring;
            this.output = output;
        }

        static Source open(Vec3 at) {
            AudioRingBuffer ring = new AudioRingBuffer(RING_SAMPLES);
            return AudioOutput.openAt(ring, AudioCodec.SAMPLE_RATE, at.x, at.y, at.z,
                            FULL_VOLUME_DISTANCE, SILENT_DISTANCE)
                    .map(output -> new Source(ring, output))
                    .orElse(null);
        }
    }
}
