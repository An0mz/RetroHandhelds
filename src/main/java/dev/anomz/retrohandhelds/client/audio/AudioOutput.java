package dev.anomz.retrohandhelds.client.audio;

import static org.lwjgl.openal.AL10.*;

import java.util.Optional;

import org.lwjgl.openal.ALC10;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.emu.AudioRingBuffer;

/**
 * Streams a ring buffer through one non-positional OpenAL source on Minecraft's AL context.
 * Owned and pumped by a single thread. The source plays relative to the listener, so only
 * Minecraft's listener gain (the Master slider) and our own gain apply.
 */
public final class AudioOutput implements AutoCloseable {
    private static final int BUFFER_COUNT = 4;
    /** Each OpenAL buffer holds about 1/32 s, so four queued buffers cover about 125 ms of hiccups. */
    private static final int CHUNKS_PER_SECOND = 32;
    /** Pumps between health reports (about 10 s at 60 pumps per second). */
    private static final int HEALTH_INTERVAL = 600;

    private final AudioRingBuffer ring;
    private final int sampleRate;
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
    private long pumps;
    private long underruns;
    private long droppedSamples;

    private AudioOutput(AudioRingBuffer ring, int sampleRate, long context) {
        this.ring = ring;
        this.sampleRate = sampleRate;
        this.context = context;
        this.chunkSamples = Math.max(256, sampleRate / CHUNKS_PER_SECOND) * 2;
        this.chunk = new short[chunkSamples];
        this.maxRingBacklog = chunkSamples * 2;
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

    /** Empty if Minecraft's sound system is not running or the source cannot be created. */
    public static Optional<AudioOutput> open(AudioRingBuffer ring, int sampleRate) {
        try {
            long context = ALC10.alcGetCurrentContext();
            if (context == 0 || sampleRate <= 0) {
                RetroHandhelds.LOGGER.warn("Retro Handhelds: no sound (OpenAL context {}, sample rate {})", context, sampleRate);
                return Optional.empty();
            }
            return Optional.of(new AudioOutput(ring, sampleRate, context));
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not open audio output", t);
            return Optional.empty();
        }
    }

    /** Moves finished buffers back to the free list and refills them from the ring. */
    public void pump(float gain) {
        if (broken) {
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
            for (int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED); processed > 0; processed--) {
                freeBuffers[freeCount++] = alSourceUnqueueBuffers(source);
            }
            int backlog = ring.available() - maxRingBacklog;
            if (backlog > 0) {
                droppedSamples += backlog;
                ring.skip(backlog);
            }
            while (freeCount > 0 && ring.available() >= chunkSamples) {
                ring.read(chunk, chunkSamples);
                int buffer = freeBuffers[--freeCount];
                alBufferData(buffer, AL_FORMAT_STEREO16, chunk, sampleRate);
                alSourceQueueBuffers(source, buffer);
            }
            // Also restarts the source after an underrun, once there is a little audio queued again.
            if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING && alGetSourcei(source, AL_BUFFERS_QUEUED) >= 2) {
                // The first start is not an underrun; any later restart is.
                if (pumps > 1 && ++underruns % 50 == 1) {
                    RetroHandhelds.LOGGER.warn("Retro Handhelds: audio underrun #{}, restarting playback", underruns);
                }
                alSourcePlay(source);
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
