package dev.anomz.retrohandhelds.client.libretro;

/** Values from libretro.h that this mod uses. */
public final class LibretroConstants {
    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    public static final int ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
    public static final int ENVIRONMENT_SET_PIXEL_FORMAT = 10;
    public static final int ENVIRONMENT_GET_VARIABLE = 15;
    public static final int ENVIRONMENT_GET_VARIABLE_UPDATE = 17;
    public static final int ENVIRONMENT_GET_SAVE_DIRECTORY = 31;

    public static final int PIXEL_FORMAT_0RGB1555 = 0;
    public static final int PIXEL_FORMAT_XRGB8888 = 1;
    public static final int PIXEL_FORMAT_RGB565 = 2;

    public static final int MEMORY_SAVE_RAM = 0;

    public static final int DEVICE_JOYPAD = 1;
    public static final int DEVICE_ID_JOYPAD_B = 0;
    public static final int DEVICE_ID_JOYPAD_SELECT = 2;
    public static final int DEVICE_ID_JOYPAD_START = 3;
    public static final int DEVICE_ID_JOYPAD_UP = 4;
    public static final int DEVICE_ID_JOYPAD_DOWN = 5;
    public static final int DEVICE_ID_JOYPAD_LEFT = 6;
    public static final int DEVICE_ID_JOYPAD_RIGHT = 7;
    public static final int DEVICE_ID_JOYPAD_A = 8;
    public static final int DEVICE_ID_JOYPAD_L = 10;
    public static final int DEVICE_ID_JOYPAD_R = 11;
    /** Asks for all buttons at once as a bitmask. */
    public static final int DEVICE_ID_JOYPAD_MASK = 256;

    private LibretroConstants() {
    }
}
