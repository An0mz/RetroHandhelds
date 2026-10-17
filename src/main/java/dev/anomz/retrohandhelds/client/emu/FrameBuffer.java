package dev.anomz.retrohandhelds.client.emu;

/**
 * Double buffer of ABGR pixels (the layout {@code NativeImage.setPixelRGBA} expects), sized for the
 * largest picture its core produces. Each row is {@link #stride()} pixels long and a smaller frame
 * fills the top-left corner. The emulator thread fills {@link #back()} and swaps; any other thread
 * copies the front buffer.
 */
public final class FrameBuffer {
    private final Object lock = new Object();
    private final int maxWidth;
    private final int maxHeight;
    private int[] back;
    private int[] front;
    private long frameCount;
    private int frontWidth;
    private int frontHeight;
    /** Display aspect ratio from the core, or 0 to use the frame's own width/height. */
    private volatile float aspect;

    public FrameBuffer(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.back = new int[maxWidth * maxHeight];
        this.front = new int[maxWidth * maxHeight];
        this.frontWidth = maxWidth;
        this.frontHeight = maxHeight;
    }

    /** Row length of the pixel arrays, also the largest frame width. */
    public int stride() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    /** Size of an array that holds any frame. */
    public int capacity() {
        return maxWidth * maxHeight;
    }

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

    /** Set by the emulator thread from the core's geometry; {@code <= 0} means square pixels. */
    void setAspect(float aspect) {
        this.aspect = aspect;
    }

    /** How wide the picture should look for its height. Games with non-square pixels (PS1) need this. */
    public float aspect(int width, int height) {
        float a = aspect;
        return a > 0 ? a : width / (float) height;
    }

    /**
     * Copies the latest frame into {@code dst} (at least {@link #capacity()} long) and its size into
     * {@code sizeOut} (width, height), only if it is newer than {@code lastSeen}. Only the rows in use
     * are copied. Returns the current frame count either way.
     */
    public long copyIfNewer(int[] dst, long lastSeen, int[] sizeOut) {
        synchronized (lock) {
            if (frameCount != lastSeen) {
                System.arraycopy(front, 0, dst, 0, frontHeight * maxWidth);
                sizeOut[0] = frontWidth;
                sizeOut[1] = frontHeight;
            }
            return frameCount;
        }
    }

    /** Copies the latest frame and its size; returns how many frames were published so far. */
    public long copyLatest(int[] dst, int[] sizeOut) {
        return copyIfNewer(dst, -1, sizeOut);
    }
}
