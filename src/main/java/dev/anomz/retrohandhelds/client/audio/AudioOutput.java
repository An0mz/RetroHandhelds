package dev.anomz.retrohandhelds.client.audio;

import static org.lwjgl.openal.AL10.*;

import java.util.Optional;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;

/**
 * Streams a ring buffer through one OpenAL source on Minecraft's AL context. Owned and pumped by a
 * single thread. Either plays relative to the listener ({@link #LOCAL}: the player's own game), or
 * from a point in the world with distance falloff ({@link #openAt}: another player's console).
 */
public final class AudioOutput implements AutoCloseable {
    private static final int BUFFER_COUNT = 4;

    /**
     * @param channels 1 (mono, needed for 3D placement) or 2 (stereo)
     * @param chunksPerSecond OpenAL buffer length; four buffers are queued
     * @param startChunks buffers to have before starting or restarting playback
     * @param backlogChunks ring contents beyond this many buffers are stale and dropped
     */
    public record Settings(int channels, int chunksPerSecond, int startChunks, int backlogChunks) {
    }

    /** The player's own game: stereo, short buffers (about 31 ms) for low latency; pumped every frame. */
    public static final Settings LOCAL = new Settings(2, 32, 2, 2);
    /** Another player's game over the network: mono, 100 ms buffers and a deeper backlog to absorb jitter. */
    public static final Settings SHARED = new Settings(1, 10, 2, 6);
    /** Pumps between health reports (about 10 s at 60 pumps per second). */
    private static final int HEALTH_INTERVAL = 600;

    private final AudioRingBuffer ring;
    private final int sampleRate;
    private final int format;
    private final int startChunks;
    private final long context;
    private final int source;
    private final int[] buffers = new int[BUFFER_COUNT];
    private final int[] freeBuffers = new int[BUFFER_COUNT];
    private final short[] chunk;
    private final int chunkSamples;
    /** Anything beyond this waiting in the ring is stale; dropping it keeps latency bounded. */
    private final int maxRingBacklog;
    private int freeCount;
    private boolean broken;
    private boolean paused;
    private long pumps;
    private boolean started;
    private long underruns;
    private long droppedSamples;

    private AudioOutput(AudioRingBuffer ring, int sampleRate, long context, Settings settings) {
        this.ring = ring;
        this.sampleRate = sampleRate;
        this.context = context;
        this.format = settings.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
        this.startChunks = settings.startChunks();
        // Whole stereo pairs (and even mono counts), matching how the ring stores samples.
        this.chunkSamples = (Math.max(256, sampleRate / settings.chunksPerSecond()) * settings.channels()) & ~1;
        this.chunk = new short[chunkSamples];
        this.maxRingBacklog = chunkSamples * settings.backlogChunks();
        alGetError();
        this.source = alGenSources();
        alGenBuffers(buffers);
        System.arraycopy(buffers, 0, freeBuffers, 0, BUFFER_COUNT);
        freeCount = BUFFER_COUNT;
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(source, AL_POSITION, 0, 0, 0);
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            deleteObjects();
            throw new IllegalStateException("OpenAL error " + error + " while creating the audio source");
        }
    }

    /** The player's own game ({@link #LOCAL}). Empty if Minecraft's sound system is not running. */
    public static Optional<AudioOutput> open(AudioRingBuffer ring, int sampleRate) {
        return open(ring, sampleRate, LOCAL);
    }

    /** Empty if Minecraft's sound system is not running or the source cannot be created. */
    public static Optional<AudioOutput> open(AudioRingBuffer ring, int sampleRate, Settings settings) {
        try {
            long context = ALC10.alcGetCurrentContext();
            if (context == 0 || sampleRate <= 0) {
                RetroHandhelds.LOGGER.warn("Retro Handhelds: no sound (OpenAL context {}, sample rate {})", context, sampleRate);
                return Optional.empty();
            }
            return Optional.of(new AudioOutput(ring, sampleRate, context, settings));
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not open audio output", t);
            return Optional.empty();
        }
    }

    /**
     * Mono sound coming from a point in the world, like Minecraft's own sounds: full volume within
     * {@code fullVolumeDistance} blocks, fading linearly to silence at {@code silentDistance}.
     */
    public static Optional<AudioOutput> openAt(AudioRingBuffer ring, int sampleRate, double x, double y, double z,
            float fullVolumeDistance, float silentDistance) {
        Optional<AudioOutput> output = open(ring, sampleRate, SHARED);
        output.ifPresent(o -> o.placeAt(x, y, z, fullVolumeDistance, silentDistance));
        return output;
    }

    private void placeAt(double x, double y, double z, float fullVolumeDistance, float silentDistance) {
        try {
            alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
            alSource3f(source, AL_POSITION, (float) x, (float) y, (float) z);
            // Per-source distance model, which Minecraft enables for its own sounds.
            alSourcei(source, AL_DISTANCE_MODEL, AL11.AL_LINEAR_DISTANCE);
            alSourcef(source, AL_REFERENCE_DISTANCE, fullVolumeDistance);
            alSourcef(source, AL_MAX_DISTANCE, silentDistance);
            alSourcef(source, AL_ROLLOFF_FACTOR, 1);
        } catch (Throwable t) {
            broken = true;
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not place shared sound in the world", t);
        }
    }

    /** Moves a sound opened with {@link #openAt} (it follows a player around). */
    public void moveTo(double x, double y, double z) {
        if (broken) {
            return;
        }
        try {
            alSource3f(source, AL_POSITION, (float) x, (float) y, (float) z);
        } catch (Throwable t) {
            broken = true;
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not move shared sound", t);
        }
    }

    /** Moves finished buffers back to the free list and refills them from the ring. */
    public void pump(float gain) {
        if (broken || paused) {
            return;
        }
        try {
            // Minecraft recreates its context when the sound device changes; our objects die with the old one.
            if (ALC10.alcGetCurrentContext() != context) {
                broken = true;
                RetroHandhelds.LOGGER.warn("Retro Handhelds: OpenAL context changed, emulator sound stopped");
                return;
            }
            if (++pumps % HEALTH_INTERVAL == 0 && !checkHealth(gain)) {
                return;
            }
            alSourcef(source, AL_GAIN, gain);
            // On a stopped source OpenAL Soft reports every queued buffer as processed, even ones queued
            // a moment ago, so a stopped source is emptied here and must be refilled and restarted in
            // this same call; waiting for the queue to grow across calls would never succeed.
            for (int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED); processed > 0; processed--) {
                freeBuffers[freeCount++] = alSourceUnqueueBuffers(source);
            }
            int backlog = ring.available() - maxRingBacklog;
            if (backlog > 0) {
                droppedSamples += backlog;
                ring.skip(backlog);
            }
            boolean playing = alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
            // Not playing (first start or after an underrun): wait until there is enough to prime with.
            if (!playing && ring.available() < chunkSamples * startChunks) {
                return;
            }
            while (freeCount > 0 && ring.available() >= chunkSamples) {
                ring.read(chunk, chunkSamples);
                int buffer = freeBuffers[--freeCount];
                alBufferData(buffer, format, chunk, sampleRate);
                alSourceQueueBuffers(source, buffer);
            }
            if (!playing) {
                // The first start is not an underrun; any later restart is.
                if (started && ++underruns % 50 == 1) {
                    RetroHandhelds.LOGGER.warn("Retro Handhelds: audio underrun #{}, restarting playback", underruns);
                }
                alSourcePlay(source);
                started = true;
            }
        } catch (Throwable t) {
            broken = true;
            RetroHandhelds.LOGGER.error("Retro Handhelds: audio output failed, emulator sound stopped", t);
        }
    }

    /**
     * Logs a debug summary, and gives up loudly if our source no longer exists: in that case every
     * AL call on it fails without throwing, which would otherwise mean silence with nothing in the log.
     */
    private boolean checkHealth(float gain) {
        if (!alIsSource(source)) {
            broken = true;
            RetroHandhelds.LOGGER.warn("Retro Handhelds: OpenAL source {} was deleted outside the mod, emulator sound stopped", source);
            return false;
        }
        RetroHandhelds.LOGGER.debug("Retro Handhelds audio: state={} queued={} ring={} gain={} underruns={} droppedMs={}",
                alGetSourcei(source, AL_SOURCE_STATE), alGetSourcei(source, AL_BUFFERS_QUEUED), ring.available(),
                gain, underruns, droppedSamples * 1000 / 2 / sampleRate);
        return true;
    }

    /** Pauses or resumes playback where it left off. Queued audio is kept, so resuming is not an underrun. */
    public void setPaused(boolean paused) {
        if (broken || this.paused == paused) {
            return;
        }
        this.paused = paused;
        try {
            if (ALC10.alcGetCurrentContext() != context) {
                return;
            }
            if (paused) {
                alSourcePause(source);
            } else if (alGetSourcei(source, AL_BUFFERS_QUEUED) > 0) {
                alSourcePlay(source);
            }
        } catch (Throwable t) {
            broken = true;
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not pause or resume emulator sound", t);
        }
    }

    @Override
    public void close() {
        try {
            if (ALC10.alcGetCurrentContext() == context) {
                deleteObjects();
            }
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: error while closing audio output", t);
        }
    }

    private void deleteObjects() {
        alSourceStop(source);
        alSourcei(source, AL_BUFFER, 0);
        alDeleteSources(source);
        alDeleteBuffers(buffers);
    }
}
