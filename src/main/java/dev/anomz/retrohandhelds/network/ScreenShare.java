package dev.anomz.retrohandhelds.network;

/** Limits of the screen-sharing wire format, shared by client and server. */
public final class ScreenShare {
    public static final int STRIPS = 8;
    public static final int MAX_WIDTH = 240;
    public static final int MAX_HEIGHT = 160;
    /** Largest compressed strip; well under the 32 KB limit for client-to-server packets. */
    public static final int MAX_DATA = 16384;
    /** A player may share a console's screen from this far away (matches when the client switches it off). */
    public static final double MAX_SENDER_DISTANCE = 48;

    private ScreenShare() {
    }

    public static boolean validStrip(int width, int height, int strip, int dataLength) {
        return width > 0 && width <= MAX_WIDTH && height > 0 && height <= MAX_HEIGHT
                && strip >= 0 && strip < STRIPS && dataLength > 0 && dataLength <= MAX_DATA;
    }
}
