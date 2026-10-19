package dev.anomz.retrohandhelds.client.emu;

/**
 * Double buffer of up to 320x240 ABGR pixels (the layout {@code NativeImage.setPixelRGBA} expects).
 * A smaller frame (240x160 GBA, 256x224 SNES, 160x144 Game Boy) fills the top-left corner; a bigger
 * one (SNES hi-res, 512 wide) is scaled down to fit, keeping its shape. The emulator thread
 * fills {@link #back()} and swaps; any other thread copies the front buffer.
 */
public final class FrameBuffer {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 240;
    public static final int PIXELS = WIDTH * HEIGHT;

    private final Object lock = new Object();
    private int[] back = new int[PIXELS];
    private int[] front = new int[PIXELS];
    private long frameCount;
    private int frontWidth = WIDTH;
    private int frontHeight = HEIGHT;

    /** Emulator thread only. */
    int[] back() {
        return back;
    }

    /** Emulator thread only. */
    void publish(int width, int height) {
        synchronized (lock) {
            int[] swap = front;
            front = back;
            back = swap;
            frontWidth = width;
            frontHeight = height;
            frameCount++;
        }
    }

    /**
     * Copies the latest frame into {@code dst} and its size into {@code sizeOut} (width, height),
     * only if it is newer than {@code lastSeen}. Returns the current frame count either way.
     */
    public long copyIfNewer(int[] dst, long lastSeen, int[] sizeOut) {
        synchronized (lock) {
            if (frameCount != lastSeen) {
                System.arraycopy(front, 0, dst, 0, PIXELS);
                sizeOut[0] = frontWidth;
                sizeOut[1] = frontHeight;
            }
            return frameCount;
        }
    }

    /** Copies the latest frame into {@code dst} and returns how many frames were published so far. */
    public long copyLatest(int[] dst) {
        synchronized (lock) {
            System.arraycopy(front, 0, dst, 0, PIXELS);
            return frameCount;
        }
    }
}
