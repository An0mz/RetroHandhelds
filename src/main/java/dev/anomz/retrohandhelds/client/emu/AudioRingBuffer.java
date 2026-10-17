package dev.anomz.retrohandhelds.client.emu;

/**
 * Lock-free single-producer/single-consumer ring of interleaved stereo int16 samples.
 * Positions only grow; the volatile writes publish the copied samples to the other side.
 */
public final class AudioRingBuffer {
    private final short[] data;
    private final int mask;
    private volatile long writePos;
    private volatile long readPos;

    /** @param capacity in samples (not frames); must be a power of two */
    public AudioRingBuffer(int capacity) {
        if (Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two");
        }
        this.data = new short[capacity];
        this.mask = capacity - 1;
    }

    /** Producer side. Samples that do not fit are dropped. */
    public void write(short[] src, int count) {
        long write = writePos;
        int free = data.length - (int) (write - readPos);
        int n = Math.min(count, free) & ~1;
        int start = (int) (write & mask);
        int first = Math.min(n, data.length - start);
        System.arraycopy(src, 0, data, start, first);
        System.arraycopy(src, first, data, 0, n - first);
        writePos = write + n;
    }

    /** Samples ready to read. */
    public int available() {
        return (int) (writePos - readPos);
    }

    /** Consumer side. Caller must check {@link #available()} first. */
    public void read(short[] dst, int count) {
        long read = readPos;
        int start = (int) (read & mask);
        int first = Math.min(count, data.length - start);
        System.arraycopy(data, start, dst, 0, first);
        System.arraycopy(data, 0, dst, first, count - first);
        readPos = read + count;
    }

    /** Consumer side: discards the oldest samples. */
    public void skip(int count) {
        readPos += Math.min(count, available()) & ~1;
    }
}
