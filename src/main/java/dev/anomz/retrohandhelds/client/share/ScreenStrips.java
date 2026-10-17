package dev.anomz.retrohandhelds.client.share;

import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import dev.anomz.retrohandhelds.network.ScreenShare;

/**
 * Wire format for sharing a game's picture: the frame is cut into {@link #STRIPS} horizontal strips
 * of 16-bit RGB565 pixels (lossless for GBA's 15-bit colour), each compressed with deflate. Only
 * strips that changed are sent, plus one rotating refresh strip per frame for players who arrive late.
 */
public final class ScreenStrips {
    public static final int STRIPS = ScreenShare.STRIPS;
    public static final int MAX_WIDTH = ScreenShare.MAX_WIDTH;
    public static final int MAX_HEIGHT = ScreenShare.MAX_HEIGHT;
    /** Upper bound for one compressed strip; also enforced by the packet codec. */
    public static final int MAX_DATA = ScreenShare.MAX_DATA;
    private static final int MAX_STRIP_PIXELS = MAX_WIDTH * ((MAX_HEIGHT + STRIPS - 1) / STRIPS);

    private ScreenStrips() {
    }

    public static int rowsPerStrip(int height) {
        return (height + STRIPS - 1) / STRIPS;
    }

    public static boolean validSize(int width, int height) {
        return width > 0 && width <= MAX_WIDTH && height > 0 && height <= MAX_HEIGHT;
    }

    @FunctionalInterface
    public interface StripSink {
        /** {@code data} is a fresh array the sink may keep. */
        void accept(int strip, byte[] data);
    }

    /** Remembers what was sent last, so unchanged strips are skipped. Single-threaded. */
    public static final class Encoder {
        private final short[][] lastSent = new short[STRIPS][MAX_STRIP_PIXELS];
        private final short[] current = new short[MAX_STRIP_PIXELS];
        private final byte[] raw = new byte[MAX_STRIP_PIXELS * 2];
        private final byte[] out = new byte[MAX_DATA];
        private final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        private int lastWidth = -1;
        private int lastHeight = -1;
        private int refreshStrip;

        /** Forget what was sent; the next frame goes out in full. */
        public void reset() {
            lastWidth = -1;
            lastHeight = -1;
        }

        /**
         * Encodes the strips of {@code abgr} (row length {@link #MAX_WIDTH}, picture in the top-left
         * {@code width x height}) that changed since the last call, plus one refresh strip.
         *
         * @return the number of strips handed to {@code sink}
         */
        public int encode(int[] abgr, int width, int height, StripSink sink) {
            boolean resized = width != lastWidth || height != lastHeight;
            lastWidth = width;
            lastHeight = height;
            int rows = rowsPerStrip(height);
            int refresh = refreshStrip;
            refreshStrip = (refreshStrip + 1) % STRIPS;
            int sent = 0;
            for (int strip = 0; strip < STRIPS; strip++) {
                int y0 = strip * rows;
                int y1 = Math.min(height, y0 + rows);
                if (y0 >= y1) {
                    continue;
                }
                int pixels = (y1 - y0) * width;
                for (int y = y0, i = 0; y < y1; y++) {
                    for (int x = 0; x < width; x++, i++) {
                        current[i] = toRgb565(abgr[y * MAX_WIDTH + x]);
                    }
                }
                boolean changed = resized || !Arrays.equals(current, 0, pixels, lastSent[strip], 0, pixels);
                if (!changed && strip != refresh) {
                    continue;
                }
                System.arraycopy(current, 0, lastSent[strip], 0, pixels);
                byte[] data = compress(pixels);
                if (data != null) {
                    sink.accept(strip, data);
                    sent++;
                }
            }
            return sent;
        }

        private byte[] compress(int pixels) {
            for (int i = 0; i < pixels; i++) {
                raw[2 * i] = (byte) current[i];
                raw[2 * i + 1] = (byte) (current[i] >> 8);
            }
            deflater.reset();
            deflater.setInput(raw, 0, pixels * 2);
            deflater.finish();
            int length = deflater.deflate(out);
            // Incompressible beyond the packet limit: skip it, the refresh will retry this strip later.
            return deflater.finished() ? Arrays.copyOf(out, length) : null;
        }
    }

    /** Decodes strips into an ABGR picture. Single-threaded (the client main thread). */
    public static final class Decoder {
        private final byte[] raw = new byte[MAX_STRIP_PIXELS * 2];
        private final Inflater inflater = new Inflater();

        /**
         * Writes strip {@code strip} of a {@code width x height} picture into {@code abgr}
         * (row length {@link #MAX_WIDTH}). Rejects anything that does not decode to exactly that strip.
         */
        public boolean decode(byte[] data, int strip, int width, int height, int[] abgr) {
            if (!validSize(width, height) || strip < 0 || strip >= STRIPS || data.length == 0 || data.length > MAX_DATA) {
                return false;
            }
            int rows = rowsPerStrip(height);
            int y0 = strip * rows;
            int y1 = Math.min(height, y0 + rows);
            if (y0 >= y1) {
                return false;
            }
            int expected = (y1 - y0) * width * 2;
            try {
                inflater.reset();
                inflater.setInput(data);
                int n = inflater.inflate(raw, 0, expected);
                if (n != expected || !inflater.finished() || inflater.getRemaining() != 0) {
                    return false;
                }
            } catch (DataFormatException e) {
                return false;
            }
            for (int y = y0, i = 0; y < y1; y++) {
                for (int x = 0; x < width; x++, i++) {
                    abgr[y * MAX_WIDTH + x] = fromRgb565((raw[2 * i] & 0xFF) | (raw[2 * i + 1] & 0xFF) << 8);
                }
            }
            return true;
        }
    }

    static short toRgb565(int abgr) {
        int r = abgr & 0xFF;
        int g = abgr >> 8 & 0xFF;
        int b = abgr >> 16 & 0xFF;
        return (short) ((r >> 3) << 11 | (g >> 2) << 5 | b >> 3);
    }

    static int fromRgb565(int p) {
        int r = p >> 11 & 0x1F;
        int g = p >> 5 & 0x3F;
        int b = p & 0x1F;
        return 0xFF000000 | (b << 3 | b >> 2) << 16 | (g << 2 | g >> 4) << 8 | (r << 3 | r >> 2);
    }
}
