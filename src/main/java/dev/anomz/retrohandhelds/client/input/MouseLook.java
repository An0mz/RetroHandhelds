package dev.anomz.retrohandhelds.client.input;

import dev.anomz.retrohandhelds.client.emu.JoypadState;

/**
 * Turns mouse movement into the right stick, one of two ways:
 * <ul>
 * <li>{@link Mode#STICK}: the faster the mouse moves, the further the stick goes, back to centre when
 * it stops. Smoothed over a few frames so uneven mouse reports and frame times don't make it jitter.
 * For games with an analog camera (PlayStation).</li>
 * <li>{@link Mode#TAPS}: every so many pixels of movement is one short, full tap in that direction,
 * with a gap before the next. For the N64, whose right stick is the C-buttons: games such as Super
 * Mario 64 turn the camera one step per press, so a held button or a wobbling stick does nothing useful.</li>
 * </ul>
 */
public final class MouseLook {
    public enum Mode { STICK, TAPS }

    /** Mouse speed, in window pixels per second, that tilts the stick all the way at sensitivity 1. */
    static final double FULL_TILT_SPEED = 1500;
    private static final double SMOOTHING_SECONDS = 0.05;
    /** Movement, in window pixels at sensitivity 1, that makes one tap. */
    static final double TAP_PIXELS = 120;
    /** How long a tap holds the stick, and the release after it: long enough for a game reading once per frame. */
    static final long TAP_NANOS = 80_000_000L;
    static final long GAP_NANOS = 80_000_000L;
    /** At most this many taps wait their turn, so a long swipe doesn't keep turning after the mouse stops. */
    private static final double MAX_QUEUED_TAPS = 2;

    private double speedX;
    private double speedY;

    private double pendingX;
    private double pendingY;
    private int tapX;
    private int tapY;
    private long tapEnds;
    private long gapEnds;

    /** {@link Mode#STICK}: adds {@code dx}, {@code dy} pixels of movement over the last {@code seconds}. */
    public void update(double dx, double dy, double seconds) {
        if (seconds <= 0) {
            return;
        }
        double blend = 1 - Math.exp(-seconds / SMOOTHING_SECONDS);
        speedX += (dx / seconds - speedX) * blend;
        speedY += (dy / seconds - speedY) * blend;
    }

    /**
     * {@link Mode#TAPS}: adds {@code dx}, {@code dy} pixels of movement at {@code now} (nanoseconds),
     * starting or ending taps. Horizontal movement wins over vertical when both are due.
     */
    public void updateTaps(double dx, double dy, long now, float sensitivity) {
        double step = TAP_PIXELS / sensitivity;
        double max = step * MAX_QUEUED_TAPS;
        pendingX = Math.max(-max, Math.min(max, pendingX + dx));
        pendingY = Math.max(-max, Math.min(max, pendingY + dy));
        if ((tapX != 0 || tapY != 0) && now >= tapEnds) {
            tapX = 0;
            tapY = 0;
            gapEnds = now + GAP_NANOS;
        }
        if (tapX == 0 && tapY == 0 && now >= gapEnds) {
            if (Math.abs(pendingX) >= step) {
                tapX = pendingX > 0 ? 1 : -1;
                pendingX -= tapX * step;
                pendingY = 0;
                tapEnds = now + TAP_NANOS;
            } else if (Math.abs(pendingY) >= step) {
                tapY = pendingY > 0 ? 1 : -1;
                pendingY -= tapY * step;
                pendingX = 0;
                tapEnds = now + TAP_NANOS;
            }
        }
    }

    public void reset() {
        speedX = 0;
        speedY = 0;
        pendingX = 0;
        pendingY = 0;
        tapX = 0;
        tapY = 0;
        tapEnds = 0;
        gapEnds = 0;
    }

    public int stickX(Mode mode, float sensitivity) {
        return mode == Mode.TAPS ? tapX * JoypadState.AXIS_MAX : toStick(speedX, sensitivity);
    }

    public int stickY(Mode mode, float sensitivity) {
        return mode == Mode.TAPS ? tapY * JoypadState.AXIS_MAX : toStick(speedY, sensitivity);
    }

    private static int toStick(double speed, float sensitivity) {
        double tilt = Math.max(-1, Math.min(1, speed * sensitivity / FULL_TILT_SPEED));
        return (int) Math.round(tilt * JoypadState.AXIS_MAX);
    }
}
