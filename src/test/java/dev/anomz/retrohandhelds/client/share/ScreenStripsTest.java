package dev.anomz.retrohandhelds.client.share;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ScreenStripsTest {
    private static final int W = ScreenStrips.MAX_WIDTH;
    private static final int H = ScreenStrips.MAX_HEIGHT;

    /** A frame whose colours survive RGB565 exactly (GBA colours are 15-bit, so this is the real case). */
    private static int[] frame(long seed) {
        Random random = new Random(seed);
        int[] px = new int[W * H];
        for (int i = 0; i < px.length; i++) {
            px[i] = ScreenStrips.fromRgb565(random.nextInt(1 << 16));
        }
        return px;
    }

    private record Sent(int strip, byte[] data) {
    }

    private static List<Sent> encode(ScreenStrips.Encoder encoder, int[] px, int w, int h) {
        List<Sent> sent = new ArrayList<>();
        encoder.encode(px, w, h, (strip, data) -> sent.add(new Sent(strip, data)));
        return sent;
    }

    @Test
    void firstFrameSendsEveryStripAndDecodesExactly() {
        int[] px = frame(1);
        List<Sent> sent = encode(new ScreenStrips.Encoder(), px, W, H);
        assertEquals(ScreenStrips.STRIPS, sent.size());

        int[] out = new int[W * H];
        ScreenStrips.Decoder decoder = new ScreenStrips.Decoder();
        for (Sent s : sent) {
            assertTrue(decoder.decode(s.data(), s.strip(), W, H, out));
        }
        assertArrayEquals(px, out);
    }

    @Test
    void unchangedFrameSendsOnlyTheRefreshStrip() {
        ScreenStrips.Encoder encoder = new ScreenStrips.Encoder();
        int[] px = frame(2);
        encode(encoder, px, W, H);
        assertEquals(1, encode(encoder, px, W, H).size());
    }

    @Test
    void changeInOneRowSendsOnlyItsStripPlusRefresh() {
        ScreenStrips.Encoder encoder = new ScreenStrips.Encoder();
        int[] px = frame(3);
        encode(encoder, px, W, H);
        px[100 * W + 10] ^= 0x00F800F8; // row 100 is in strip 5 (20 rows each)
        List<Sent> sent = encode(encoder, px, W, H);
        assertTrue(sent.stream().anyMatch(s -> s.strip() == 5));
        assertTrue(sent.size() <= 2);
    }

    @Test
    void refreshCyclesThroughAllStrips() {
        ScreenStrips.Encoder encoder = new ScreenStrips.Encoder();
        int[] px = frame(4);
        encode(encoder, px, W, H);
        boolean[] seen = new boolean[ScreenStrips.STRIPS];
        for (int i = 0; i < ScreenStrips.STRIPS; i++) {
            encode(encoder, px, W, H).forEach(s -> seen[s.strip()] = true);
        }
        for (boolean b : seen) {
            assertTrue(b);
        }
    }

    @Test
    void gameBoySizeRoundTrips() {
        int w = 160;
        int h = 144;
        int[] px = frame(5);
        List<Sent> sent = encode(new ScreenStrips.Encoder(), px, w, h);
        int[] out = new int[W * H];
        ScreenStrips.Decoder decoder = new ScreenStrips.Decoder();
        for (Sent s : sent) {
            assertTrue(decoder.decode(s.data(), s.strip(), w, h, out));
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                assertEquals(px[y * W + x], out[y * W + x]);
            }
        }
    }

    @Test
    void flatScreenCompressesToAlmostNothing() {
        int[] px = new int[W * H];
        java.util.Arrays.fill(px, ScreenStrips.fromRgb565(0x001F));
        int total = encode(new ScreenStrips.Encoder(), px, W, H).stream().mapToInt(s -> s.data().length).sum();
        assertTrue(total < 1000, "total bytes " + total);
    }

    @Test
    void rejectsBadInput() {
        int[] px = frame(6);
        List<Sent> sent = encode(new ScreenStrips.Encoder(), px, W, H);
        byte[] good = sent.get(0).data();
        int[] out = new int[W * H];
        ScreenStrips.Decoder decoder = new ScreenStrips.Decoder();
        assertFalse(decoder.decode(new byte[] {1, 2, 3}, 0, W, H, out), "garbage");
        assertFalse(decoder.decode(good, 0, W, 100, out), "wrong height");
        assertFalse(decoder.decode(good, 0, 300, H, out), "too wide");
        assertFalse(decoder.decode(good, 9, W, H, out), "strip out of range");
        byte[] padded = java.util.Arrays.copyOf(good, good.length + 5);
        assertFalse(decoder.decode(padded, 0, W, H, out), "trailing bytes");
    }
}
