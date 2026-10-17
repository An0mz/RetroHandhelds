package dev.anomz.retrohandhelds.client.emu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AudioRingBufferTest {
    private static short[] samples(int from, int count) {
        short[] out = new short[count];
        for (int i = 0; i < count; i++) {
            out[i] = (short) (from + i);
        }
        return out;
    }

    @Test
    void capacityMustBePowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> new AudioRingBuffer(100));
    }

    @Test
    void readsBackWhatWasWritten() {
        AudioRingBuffer ring = new AudioRingBuffer(16);
        ring.write(samples(0, 6), 6);
        assertEquals(6, ring.available());
        short[] out = new short[6];
        ring.read(out, 6);
        assertArrayEquals(samples(0, 6), out);
        assertEquals(0, ring.available());
    }

    @Test
    void wrapsAroundTheEnd() {
        AudioRingBuffer ring = new AudioRingBuffer(8);
        short[] scratch = new short[6];
        ring.write(samples(0, 6), 6);
        ring.read(scratch, 6);
        ring.write(samples(100, 6), 6); // crosses the end of the array
        short[] out = new short[6];
        ring.read(out, 6);
        assertArrayEquals(samples(100, 6), out);
    }

    @Test
    void dropsWhatDoesNotFitAndKeepsStereoPairs() {
        AudioRingBuffer ring = new AudioRingBuffer(8);
        ring.write(samples(0, 7), 7);
        ring.write(samples(50, 6), 6);
        // 7 samples is an odd count, so only 6 (3 stereo frames) go in; then 2 more fit.
        assertEquals(8, ring.available());
    }

    @Test
    void skipDiscardsOldestWholeFrames() {
        AudioRingBuffer ring = new AudioRingBuffer(16);
        ring.write(samples(0, 8), 8);
        ring.skip(3); // rounded down to 2 to keep left/right aligned
        short[] out = new short[6];
        ring.read(out, 6);
        assertArrayEquals(samples(2, 6), out);
    }
}
