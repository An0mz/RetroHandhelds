package dev.anomz.retrohandhelds.client.emu;

/**
 * Pressed buttons as a bitmask indexed by libretro joypad id. Only the render thread writes it,
 * so a plain read-modify-write on the volatile is safe; the emulator thread only reads.
 */
public final class JoypadState {
    private volatile int pressed;

    public void set(int retroId, boolean down) {
        int bit = 1 << retroId;
        pressed = down ? pressed | bit : pressed & ~bit;
    }

    public void clear() {
        pressed = 0;
    }

    int snapshot() {
        return pressed;
    }
}
