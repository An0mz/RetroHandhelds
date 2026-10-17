package dev.anomz.retrohandhelds.client.emu;

/**
 * Pressed buttons as bitmasks indexed by libretro joypad id: one from the keyboard, one from a
 * gamepad, combined. Only the render thread writes, so a plain read-modify-write on the volatiles
 * is safe; the emulator thread only reads.
 */
public final class JoypadState {
    private volatile int pressed;
    private volatile int padPressed;
    /** Both sticks packed as four signed 16-bit values (left x, left y, right x, right y), y down. */
    private volatile long sticks;

    public void set(int retroId, boolean down) {
        int bit = 1 << retroId;
        pressed = down ? pressed | bit : pressed & ~bit;
    }

    /** The whole gamepad state at once, as polled each frame. */
    public void setPad(int mask) {
        padPressed = mask;
    }

    /** Analog sticks, -32767..32767 with y pointing down, as polled each frame. */
    public void setSticks(short leftX, short leftY, short rightX, short rightY) {
        sticks = (leftX & 0xFFFFL) | (leftY & 0xFFFFL) << 16 | (rightX & 0xFFFFL) << 32 | (rightY & 0xFFFFL) << 48;
    }

    public void clear() {
        pressed = 0;
        padPressed = 0;
        sticks = 0;
    }

    int snapshot() {
        return pressed | padPressed;
    }

    long snapshotSticks() {
        return sticks;
    }

    /** One axis of a {@link #snapshotSticks()} value: 0 left x, 1 left y, 2 right x, 3 right y. */
    static short axis(long sticks, int index) {
        return (short) (sticks >> (16 * index));
    }
}
