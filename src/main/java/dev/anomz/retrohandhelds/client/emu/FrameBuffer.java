package dev.anomz.retrohandhelds.client.emu;

/**
 * Double buffer of up to 640x480 ABGR pixels (the layout {@code NativeImage.setPixelRGBA} expects).
 * A smaller frame (240x160 GBA, 256x224 SNES, 256x384 DS) fills the top-left corner; a bigger one is
 * scaled down to fit, keeping its shape. Copies only touch the frame's own rows, so a small frame
 * costs no more than it did in a small buffer. The emulator thread
 * fills {@link #back()} and swaps; any other thread copies the front buffer.
 */
public final class FrameBuffer {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;
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
                for (int y = 0; y < frontHeight; y++) {
                    System.arraycopy(front, y * WIDTH, dst, y * WIDTH, frontWidth);
                }
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
