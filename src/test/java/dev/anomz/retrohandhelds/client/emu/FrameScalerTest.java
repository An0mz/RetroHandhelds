package dev.anomz.retrohandhelds.client.emu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FrameScalerTest {
    @Test
    void fitKeepsTheShape() {
        int[] out = new int[2];
        FrameScaler.fit(320, 224, 240, 160, out);
        assertArrayEquals(new int[] {229, 160}, out);
        FrameScaler.fit(512, 448, 320, 240, out);
        assertArrayEquals(new int[] {274, 240}, out);
    }

    @Test
    void sizesThatFitAreUnchanged() {
        int[] out = new int[2];
        FrameScaler.fit(256, 224, FrameBuffer.WIDTH, FrameBuffer.HEIGHT, out);
        assertArrayEquals(new int[] {256, 224}, out);
        FrameScaler.fit(240, 160, 240, 160, out);
        assertArrayEquals(new int[] {240, 160}, out);
    }

    @Test
    void halvingAveragesEachBlock() {
        // 4x2 source: left 2x2 block black and white, right 2x2 block all red.
        int black = 0xFF000000;
        int white = 0xFFFFFFFF;
        int red = 0xFF0000FF;
        int[] src = {black, white, red, red, white, black, red, red};
        int[] dst = new int[2];
        new FrameScaler().scale(src, 4, 4, 2, dst, 2, 2, 1);
        assertEquals(0xFF7F7F7F, dst[0]);
        assertEquals(red, dst[1]);
    }

    @Test
    void sameSizeCopiesWithStrides() {
        int[] src = {1, 2, 0, 3, 4, 0};
        int[] dst = new int[4 * 2];
        new FrameScaler().scale(src, 3, 2, 2, dst, 4, 2, 2);
        assertArrayEquals(new int[] {1, 2, 0, 0, 3, 4, 0, 0}, dst);
    }
}
