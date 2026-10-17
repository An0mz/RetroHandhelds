package dev.anomz.retrohandhelds.client.emu;

/**
 * Pressed buttons as bitmasks indexed by libretro joypad id: one from the keyboard, one from a
 * gamepad, combined. Only the render thread writes, so a plain read-modify-write on the volatiles
 * is safe; the emulator thread only reads.
 */
public final class JoypadState {
    private volatile int pressed;
    private volatile int padPressed;

    public void set(int retroId, boolean down) {
        int bit = 1 << retroId;
        pressed = down ? pressed | bit : pressed & ~bit;
    }

    /** The whole gamepad state at once, as polled each frame. */
    public void setPad(int mask) {
        padPressed = mask;
    }

    public void clear() {
        pressed = 0;
        padPressed = 0;
    }

    int snapshot() {
        return pressed | padPressed;
    }
}
