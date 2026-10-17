package dev.anomz.retrohandhelds.client.emu;

/**
 * Double buffer of 240x160 ABGR pixels (the layout {@code NativeImage.setPixelRGBA} expects).
 * The emulator thread fills {@link #back()} and swaps; any other thread copies the front buffer.
 */
public final class FrameBuffer {
    public static final int WIDTH = 240;
    public static final int HEIGHT = 160;
    public static final int PIXELS = WIDTH * HEIGHT;

    private final Object lock = new Object();
    private int[] back = new int[PIXELS];
    private int[] front = new int[PIXELS];
    private long frameCount;

    /** Emulator thread only. */
    int[] back() {
        return back;
    }

    /** Emulator thread only. */
    void publish() {
        synchronized (lock) {
            int[] swap = front;
            front = back;
            back = swap;
            frameCount++;
        }
    }

    /**
     * Copies the latest frame into {@code dst} only if it is newer than {@code lastSeen}.
     * Returns the current frame count either way.
     */
    public long copyIfNewer(int[] dst, long lastSeen) {
        synchronized (lock) {
            if (frameCount != lastSeen) {
                System.arraycopy(front, 0, dst, 0, PIXELS);
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
