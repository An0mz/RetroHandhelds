package dev.anomz.retrohandhelds.client.emu;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;

/**
 * The frontend side of libretro. Every callback catches everything, since an exception must never
 * unwind into native code. All callbacks run on the emulator thread.
 */
final class CoreCallbacks {
    // Fields, not locals: JNA only keeps weak references, and the core keeps calling these.
    final MgbaCore.EnvironmentCallback environment = this::environment;
    final MgbaCore.VideoRefreshCallback videoRefresh = this::videoRefresh;
    final MgbaCore.AudioSampleCallback audioSample = this::audioSample;
    final MgbaCore.AudioSampleBatchCallback audioSampleBatch = this::audioSampleBatch;
    final MgbaCore.InputPollCallback inputPoll = this::inputPoll;
    final MgbaCore.InputStateCallback inputState = this::inputState;

    private final FrameBuffer frames;
    private final JoypadState joypad;
    private final AudioRingBuffer audio;
    private final Memory systemDir;
    private final Memory saveDir;
    private final int[] row32 = new int[FrameBuffer.WIDTH];
    private final short[] row16 = new short[FrameBuffer.WIDTH];
    private final short[] audioScratch = new short[4096];
    private final short[] audioPair = new short[2];
    private int pixelFormat = PIXEL_FORMAT_0RGB1555;
    private int polledButtons;
    private boolean errorLogged;

    CoreCallbacks(FrameBuffer frames, JoypadState joypad, AudioRingBuffer audio, Path systemDir, Path saveDir) {
        this.frames = frames;
        this.joypad = joypad;
        this.audio = audio;
        this.systemDir = nativeString(systemDir.toAbsolutePath().toString());
        this.saveDir = nativeString(saveDir.toAbsolutePath().toString());
    }

    private byte environment(int cmd, Pointer data) {
        try {
            if (data == null) {
                return FALSE;
            }
            switch (cmd) {
                case ENVIRONMENT_SET_PIXEL_FORMAT -> {
                    int format = data.getInt(0);
                    // The core chooses the format; we accept every format we can convert.
                    if (format != PIXEL_FORMAT_XRGB8888 && format != PIXEL_FORMAT_RGB565 && format != PIXEL_FORMAT_0RGB1555) {
                        return FALSE;
                    }
                    pixelFormat = format;
                    return TRUE;
                }
                case ENVIRONMENT_GET_SYSTEM_DIRECTORY -> {
                    data.setPointer(0, systemDir);
                    return TRUE;
                }
                case ENVIRONMENT_GET_SAVE_DIRECTORY -> {
                    data.setPointer(0, saveDir);
                    return TRUE;
                }
                default -> {
                    return FALSE;
                }
            }
        } catch (Throwable t) {
            logOnce("environment", t);
            return FALSE;
        }
    }

    private void videoRefresh(Pointer data, int width, int height, long pitch) {
        // NULL means "same frame as last time".
        if (data == null) {
            return;
        }
        try {
            int w = Math.min(width, FrameBuffer.WIDTH);
            int h = Math.min(height, FrameBuffer.HEIGHT);
            int[] dst = frames.back();
            for (int y = 0; y < h; y++) {
                int out = y * FrameBuffer.WIDTH;
                long offset = y * pitch;
                if (pixelFormat == PIXEL_FORMAT_XRGB8888) {
                    data.read(offset, row32, 0, w);
                    for (int x = 0; x < w; x++) {
                        dst[out + x] = xrgb8888ToAbgr(row32[x]);
                    }
                } else {
                    data.read(offset, row16, 0, w);
                    boolean rgb565 = pixelFormat == PIXEL_FORMAT_RGB565;
                    for (int x = 0; x < w; x++) {
                        dst[out + x] = rgb565 ? rgb565ToAbgr(row16[x]) : xrgb1555ToAbgr(row16[x]);
                    }
                }
            }
            frames.publish(w, h);
        } catch (Throwable t) {
            logOnce("video refresh", t);
        }
    }

    private long audioSampleBatch(Pointer data, long frameCount) {
        try {
            if (data != null) {
                long total = frameCount * 2;
                for (long done = 0; done < total; ) {
                    int n = (int) Math.min(total - done, audioScratch.length);
                    data.read(done * Short.BYTES, audioScratch, 0, n);
                    audio.write(audioScratch, n);
                    done += n;
                }
            }
        } catch (Throwable t) {
            logOnce("audio batch", t);
        }
        return frameCount;
    }

    private void audioSample(short left, short right) {
        audioPair[0] = left;
        audioPair[1] = right;
        audio.write(audioPair, 2);
    }

    // One snapshot per frame, so every input_state call within retro_run sees the same buttons.
    private void inputPoll() {
        polledButtons = joypad.snapshot();
    }

    private short inputState(int port, int device, int index, int id) {
        if (port != 0 || device != DEVICE_JOYPAD || index != 0) {
            return 0;
        }
        if (id == DEVICE_ID_JOYPAD_MASK) {
            return (short) polledButtons;
        }
        return (short) (id >= 0 && id < 16 ? polledButtons >> id & 1 : 0);
    }

    static int xrgb8888ToAbgr(int p) {
        return 0xFF000000 | (p & 0x0000FF) << 16 | (p & 0x00FF00) | (p >> 16) & 0xFF;
    }

    static int rgb565ToAbgr(short s) {
        int p = s & 0xFFFF;
        int r = p >> 11 & 0x1F;
        int g = p >> 5 & 0x3F;
        int b = p & 0x1F;
        return abgr(r << 3 | r >> 2, g << 2 | g >> 4, b << 3 | b >> 2);
    }

    static int xrgb1555ToAbgr(short s) {
        int p = s & 0xFFFF;
        int r = p >> 10 & 0x1F;
        int g = p >> 5 & 0x1F;
        int b = p & 0x1F;
        return abgr(r << 3 | r >> 2, g << 3 | g >> 2, b << 3 | b >> 2);
    }

    private static int abgr(int r, int g, int b) {
        return 0xFF000000 | b << 16 | g << 8 | r;
    }

    private void logOnce(String where, Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            RetroHandhelds.LOGGER.error("Retro Handhelds: error in libretro {} callback (further errors suppressed)", where, t);
        }
    }

    private static Memory nativeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        Memory memory = new Memory(bytes.length + 1L);
        memory.write(0, bytes, 0, bytes.length);
        memory.setByte(bytes.length, (byte) 0);
        return memory;
    }
}
