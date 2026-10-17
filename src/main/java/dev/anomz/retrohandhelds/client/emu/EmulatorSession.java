package dev.anomz.retrohandhelds.client.emu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.jetbrains.annotations.Nullable;

import com.sun.jna.Memory;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.audio.AudioOutput;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;
import dev.anomz.retrohandhelds.client.libretro.RetroGameInfo;
import dev.anomz.retrohandhelds.client.libretro.RetroSystemAvInfo;

/**
 * Runs one ROM on a dedicated thread. Every native call happens on that thread, from
 * {@code retro_init} to {@code retro_deinit}. libretro callbacks are global to the loaded library,
 * so only one session can be active at a time.
 */
public final class EmulatorSession {
    public enum State { STARTING, RUNNING, FAILED, STOPPED }

    private static final AtomicReference<EmulatorSession> ACTIVE = new AtomicReference<>();
    private static final double GBA_FPS = 59.7275;
    private static final int MAX_FRAMES_BEHIND = 5;
    private static final long JOIN_TIMEOUT_MS = 2000;
    /** Half a second of stereo audio at mGBA's 65536 Hz output rate. */
    private static final int AUDIO_RING_SAMPLES = 1 << 16;
    /** How often save RAM is compared with the file; also written when the session stops. */
    private static final long SAVE_RAM_CHECK_NANOS = TimeUnit.SECONDS.toNanos(5);

    private record StateRequest(boolean save, CompletableFuture<StateResult> result) {
    }

    private final MgbaCore core;
    private final Path rom;
    private final FrameBuffer frames = new FrameBuffer();
    private final JoypadState joypad = new JoypadState();
    private final AudioRingBuffer audio = new AudioRingBuffer(AUDIO_RING_SAMPLES);
    private final boolean withSound;
    @Nullable
    private final SaveFiles saves;
    private final ConcurrentLinkedQueue<StateRequest> stateRequests = new ConcurrentLinkedQueue<>();
    private final CoreCallbacks callbacks;
    private final Thread thread;
    private volatile boolean running = true;
    private volatile State state = State.STARTING;
    private volatile String failure;
    private volatile float volume = 1f;
    private volatile boolean paused;
    /** Set once the emulator thread can no longer answer state requests. */
    private volatile boolean finished;
    // Kept as fields so the ROM memory stays reachable while the core may still read it.
    private Memory romData;
    private RetroGameInfo gameInfo;
    private AudioOutput audioOutput;
    private SaveRam saveRam;
    private SaveStates saveStates;

    private EmulatorSession(MgbaCore core, Path rom, Path systemDir, Path saveDir, @Nullable SaveFiles saves, boolean withSound) {
        this.core = core;
        this.rom = rom;
        this.saves = saves;
        this.withSound = withSound;
        this.callbacks = new CoreCallbacks(frames, joypad, audio, systemDir, saveDir);
        this.thread = new Thread(this::run, "RetroHandhelds-Emulator");
        this.thread.setDaemon(true);
    }

    /**
     * @param saves where save RAM and the save state live; null runs without touching any saves
     * @throws IllegalStateException if another session is still active
     */
    public static EmulatorSession start(MgbaCore core, Path rom, Path systemDir, Path saveDir,
            @Nullable SaveFiles saves, boolean withSound) {
        EmulatorSession session = new EmulatorSession(core, rom, systemDir, saveDir, saves, withSound);
        if (!ACTIVE.compareAndSet(null, session)) {
            throw new IllegalStateException("An emulator session is already running");
        }
        session.thread.start();
        return session;
    }

    public static void stopActive() {
        EmulatorSession session = ACTIVE.get();
        if (session != null) {
            session.stop();
        }
    }

    /** Asks the emulator thread to shut the core down and waits for it. */
    public void stop() {
        running = false;
        LockSupport.unpark(thread);
        if (Thread.currentThread() == thread) {
            return;
        }
        try {
            thread.join(JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            RetroHandhelds.LOGGER.warn("Retro Handhelds: emulator thread did not stop within {} ms", JOIN_TIMEOUT_MS);
        }
    }

    public FrameBuffer frames() {
        return frames;
    }

    /** Freezes emulation and sound while true (Minecraft paused). Safe to call from any thread. */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /** Gain for the emulator's sound, 0..1. Safe to call from any thread. */
    public void setVolume(float volume) {
        this.volume = volume;
    }

    /** Saves a state between frames. Completes on the emulator thread. */
    public CompletableFuture<StateResult> saveState() {
        return requestState(true);
    }

    /** Loads the saved state between frames. Completes on the emulator thread. */
    public CompletableFuture<StateResult> loadState() {
        return requestState(false);
    }

    private CompletableFuture<StateResult> requestState(boolean save) {
        if (saves == null || !running || finished) {
            return CompletableFuture.completedFuture(StateResult.FAILED);
        }
        CompletableFuture<StateResult> result = new CompletableFuture<>();
        stateRequests.add(new StateRequest(save, result));
        // The thread may have finished between the check and the add; nobody else would answer then.
        if (finished) {
            failPendingRequests();
        }
        return result;
    }

    public JoypadState joypad() {
        return joypad;
    }

    public State state() {
        return state;
    }

    public String failure() {
        return failure;
    }

    private void run() {
        boolean initialized = false;
        boolean loaded = false;
        try {
            byte[] bytes = Files.readAllBytes(rom);
            romData = new Memory(bytes.length);
            romData.write(0, bytes, 0, bytes.length);

            core.retro_set_environment(callbacks.environment);
            core.retro_set_video_refresh(callbacks.videoRefresh);
            core.retro_set_audio_sample(callbacks.audioSample);
            core.retro_set_audio_sample_batch(callbacks.audioSampleBatch);
            core.retro_set_input_poll(callbacks.inputPoll);
            core.retro_set_input_state(callbacks.inputState);
            core.retro_init();
            initialized = true;

            gameInfo = new RetroGameInfo();
            gameInfo.path = rom.toAbsolutePath().toString();
            gameInfo.data = romData;
            gameInfo.size = bytes.length;
            if (core.retro_load_game(gameInfo) == 0) {
                fail("The core could not load " + rom.getFileName());
                return;
            }
            loaded = true;
            if (saves != null) {
                saveRam = new SaveRam(core, saves.saveRam());
                saveRam.load();
                saveStates = new SaveStates(core, saves.state());
            }

            RetroSystemAvInfo av = new RetroSystemAvInfo();
            core.retro_get_system_av_info(av);
            double fps = av.timing.fps > 1 && av.timing.fps < 240 ? av.timing.fps : GBA_FPS;
            RetroHandhelds.LOGGER.info("Retro Handhelds: running {} ({}x{} @ {} fps, audio {} Hz)",
                    rom.getFileName(), av.geometry.baseWidth, av.geometry.baseHeight, fps, av.timing.sampleRate);
            if (withSound) {
                audioOutput = AudioOutput.open(audio, (int) av.timing.sampleRate).orElse(null);
            }

            state = State.RUNNING;
            loop((long) (TimeUnit.SECONDS.toNanos(1) / fps));
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: emulator session for {} failed", rom.getFileName(), t);
            fail(t.toString());
        } finally {
            if (audioOutput != null) {
                audioOutput.close();
            }
            if (saveRam != null) {
                saveRam.flushIfChanged();
            }
            finished = true;
            failPendingRequests();
            shutdownCore(initialized, loaded);
            if (state != State.FAILED) {
                state = State.STOPPED;
            }
            ACTIVE.compareAndSet(this, null);
        }
    }

    private void loop(long periodNanos) {
        long next = System.nanoTime();
        long nextSaveRamCheck = next + SAVE_RAM_CHECK_NANOS;
        while (running) {
            if (paused) {
                if (audioOutput != null) {
                    audioOutput.setPaused(true);
                }
                handleStateRequests();
                LockSupport.parkNanos(periodNanos);
                // Resume from now, without fast-forwarding through the paused time.
                next = System.nanoTime();
                continue;
            }
            if (audioOutput != null) {
                audioOutput.setPaused(false);
            }
            core.retro_run();
            if (audioOutput != null) {
                audioOutput.pump(volume);
            }
            handleStateRequests();
            if (saveRam != null && System.nanoTime() >= nextSaveRamCheck) {
                saveRam.flushIfChanged();
                nextSaveRamCheck = System.nanoTime() + SAVE_RAM_CHECK_NANOS;
            }
            next += periodNanos;
            long wait = next - System.nanoTime();
            if (wait > 0) {
                LockSupport.parkNanos(wait);
            } else if (-wait > MAX_FRAMES_BEHIND * periodNanos) {
                // Too far behind (debugger, suspended machine): resync instead of fast-forwarding.
                next = System.nanoTime();
            }
        }
    }

    private void handleStateRequests() {
        for (StateRequest request; (request = stateRequests.poll()) != null; ) {
            StateResult result = saveStates == null ? StateResult.FAILED
                    : request.save() ? saveStates.save() : saveStates.load();
            request.result().complete(result);
        }
    }

    private void failPendingRequests() {
        for (StateRequest request; (request = stateRequests.poll()) != null; ) {
            request.result().complete(StateResult.FAILED);
        }
    }

    private void shutdownCore(boolean initialized, boolean loaded) {
        try {
            if (loaded) {
                core.retro_unload_game();
            }
            if (initialized) {
                core.retro_deinit();
            }
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: error while shutting down the core", t);
        }
    }

    private void fail(String reason) {
        failure = reason;
        state = State.FAILED;
    }
}
