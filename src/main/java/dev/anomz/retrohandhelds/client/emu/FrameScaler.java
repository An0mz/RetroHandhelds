package dev.anomz.retrohandhelds.client.emu;

import java.util.Arrays;

/**
 * Shrinks an ABGR picture to fit a smaller one, keeping its shape. Each output pixel averages the
 * block of source pixels it covers, so thin lines and text survive. One instance per thread: it
 * keeps scratch arrays so scaling every frame allocates nothing once warmed up.
 */
public final class FrameScaler {
    private int[] columnStart = new int[0];
    private int[] sumR = new int[0];
    private int[] sumG = new int[0];
    private int[] sumB = new int[0];

    /**
     * The largest size with the shape of {@code width x height} that fits {@code maxWidth x maxHeight},
     * written to {@code out} as (width, height). Sizes that already fit are returned unchanged.
     */
    public static void fit(int width, int height, int maxWidth, int maxHeight, int[] out) {
        if (width <= maxWidth && height <= maxHeight) {
            out[0] = width;
            out[1] = height;
            return;
        }
        double scale = Math.min(maxWidth / (double) width, maxHeight / (double) height);
        out[0] = Math.max(1, Math.min(maxWidth, (int) Math.round(width * scale)));
        out[1] = Math.max(1, Math.min(maxHeight, (int) Math.round(height * scale)));
    }

    /**
     * Scales the {@code srcWidth x srcHeight} picture at the top-left of {@code src} (rows
     * {@code srcStride} apart) into the {@code dstWidth x dstHeight} top-left of {@code dst} (rows
     * {@code dstStride} apart). The destination must not be larger than the source.
     */
    public void scale(int[] src, int srcStride, int srcWidth, int srcHeight,
            int[] dst, int dstStride, int dstWidth, int dstHeight) {
        if (srcWidth == dstWidth && srcHeight == dstHeight) {
            for (int y = 0; y < srcHeight; y++) {
                System.arraycopy(src, y * srcStride, dst, y * dstStride, srcWidth);
            }
            return;
        }
        ensureCapacity(dstWidth);
        for (int x = 0; x <= dstWidth; x++) {
            columnStart[x] = (int) ((long) x * srcWidth / dstWidth);
        }
        for (int oy = 0; oy < dstHeight; oy++) {
            int y0 = (int) ((long) oy * srcHeight / dstHeight);
            int y1 = Math.max(y0 + 1, (int) ((long) (oy + 1) * srcHeight / dstHeight));
            Arrays.fill(sumR, 0, dstWidth, 0);
            Arrays.fill(sumG, 0, dstWidth, 0);
            Arrays.fill(sumB, 0, dstWidth, 0);
            for (int y = y0; y < y1; y++) {
                int row = y * srcStride;
                for (int ox = 0; ox < dstWidth; ox++) {
                    int x1 = Math.max(columnStart[ox] + 1, columnStart[ox + 1]);
                    for (int x = columnStart[ox]; x < x1; x++) {
                        int p = src[row + x];
                        sumR[ox] += p & 0xFF;
                        sumG[ox] += p >> 8 & 0xFF;
                        sumB[ox] += p >> 16 & 0xFF;
                    }
                }
            }
            int out = oy * dstStride;
            for (int ox = 0; ox < dstWidth; ox++) {
                int n = (y1 - y0) * Math.max(1, columnStart[ox + 1] - columnStart[ox]);
                dst[out + ox] = 0xFF000000 | (sumB[ox] / n) << 16 | (sumG[ox] / n) << 8 | sumR[ox] / n;
            }
        }
    }

    private void ensureCapacity(int width) {
        if (sumR.length < width) {
            columnStart = new int[width + 1];
            sumR = new int[width];
            sumG = new int[width];
            sumB = new int[width];
        }
    }
}
