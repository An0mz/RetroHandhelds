package dev.anomz.retrohandhelds.client.emu;

/**
 * What the player is pressing: buttons as bitmasks indexed by libretro joypad id (one from the
 * keyboard, one from a gamepad, combined), analog sticks, and a touch pointer. Only the render thread
 * writes, so a plain read-modify-write on the volatiles is safe; the emulator thread only reads, once
 * per frame through {@link #snapshot}.
 */
public final class JoypadState {
    /** Full tilt of an analog axis. */
    public static final int AXIS_MAX = 0x7FFF;

    /** Bits of {@link #setStickKey}: left stick up/down/left/right, then the right stick's. */
    public static final int STICK_UP = 0;
    public static final int STICK_DOWN = 1;
    public static final int STICK_LEFT = 2;
    public static final int STICK_RIGHT = 3;
    public static final int RIGHT_STICK = 4;

    private volatile int pressed;
    private volatile int padPressed;
    private volatile int stickKeys;
    /** Gamepad axes, 16 bits each: left x, left y, right x, right y. */
    private volatile long padAxes;
    /** Right stick from the mouse (mouse look), 16 bits per axis. */
    private volatile int mouseStick;
    /** Pointer x and y, 16 bits each, in libretro's -0x7FFF..0x7FFF; bit 32 set while touching. */
    private volatile long pointer;

    public void set(int retroId, boolean down) {
        int bit = 1 << retroId;
        pressed = down ? pressed | bit : pressed & ~bit;
    }

    /** A keyboard stick key: {@code STICK_*}, plus {@link #RIGHT_STICK} for the right stick. */
    public void setStickKey(int key, boolean down) {
        int bit = 1 << key;
        stickKeys = down ? stickKeys | bit : stickKeys & ~bit;
    }

    /** The whole gamepad state at once, as polled each frame. */
    public void setPad(int mask) {
        padPressed = mask;
    }

    /** The gamepad's sticks, each axis -0x7FFF..0x7FFF (y down). */
    public void setPadAxes(int leftX, int leftY, int rightX, int rightY) {
        padAxes = (leftX & 0xFFFFL) | (leftY & 0xFFFFL) << 16 | (rightX & 0xFFFFL) << 32 | (rightY & 0xFFFFL) << 48;
    }

    /** The right stick as moved by the mouse, each axis -0x7FFF..0x7FFF; wins when it is tilted further. */
    public void setMouseStick(int x, int y) {
        mouseStick = (x & 0xFFFF) | (y & 0xFFFF) << 16;
    }

    /** Where the mouse is over the picture, in libretro pointer units, and whether it touches. */
    public void setPointer(int x, int y, boolean touching) {
        pointer = (x & 0xFFFFL) | (y & 0xFFFFL) << 16 | (touching ? 1L << 32 : 0);
    }

    /** 0..1 across the picture to libretro's pointer range, -0x7FFF..0x7FFF. */
    public static int toPointer(double fraction) {
        double clamped = Math.max(0, Math.min(1, fraction));
        return (int) Math.round(clamped * 0xFFFE) - 0x7FFF;
    }

    public void clear() {
        pressed = 0;
        padPressed = 0;
        stickKeys = 0;
        padAxes = 0;
        mouseStick = 0;
        pointer = 0;
    }

    /** Fills {@code out} with this frame's input. Emulator thread, allocation-free. */
    void snapshot(Snapshot out) {
        out.buttons = pressed | padPressed;
        long axes = padAxes;
        int stick = stickKeys;
        for (int s = 0; s < 2; s++) {
            int x = (short) (axes >> (32 * s));
            int y = (short) (axes >> (32 * s + 16));
            int k = stick >> (RIGHT_STICK * s);
            x = tilt(x, k, STICK_LEFT, STICK_RIGHT);
            y = tilt(y, k, STICK_UP, STICK_DOWN);
            if (s == 1) {
                int mouse = mouseStick;
                int mx = (short) mouse;
                int my = (short) (mouse >> 16);
                if (Math.abs(mx) + Math.abs(my) > Math.abs(x) + Math.abs(y)) {
                    x = mx;
                    y = my;
                }
            }
            out.axes[2 * s] = (short) x;
            out.axes[2 * s + 1] = (short) y;
        }
        long p = pointer;
        out.pointerX = (short) p;
        out.pointerY = (short) (p >> 16);
        out.pointerPressed = (p >> 32 & 1) != 0;
    }

    /** {@code value}, overridden by full tilt while exactly one of the two key bits is held. */
    private static int tilt(int value, int keys, int negativeBit, int positiveBit) {
        boolean negative = (keys >> negativeBit & 1) != 0;
        boolean positive = (keys >> positiveBit & 1) != 0;
        if (negative != positive) {
            return negative ? -AXIS_MAX : AXIS_MAX;
        }
        return value;
    }

    /** One frame's input, read by every input_state call of that frame. */
    static final class Snapshot {
        int buttons;
        /** Left x, left y, right x, right y. */
        final short[] axes = new short[4];
        short pointerX;
        short pointerY;
        boolean pointerPressed;
    }
}
