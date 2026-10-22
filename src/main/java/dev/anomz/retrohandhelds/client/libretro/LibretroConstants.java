package dev.anomz.retrohandhelds.client.libretro;

/** Values from libretro.h that this mod uses. */
public final class LibretroConstants {
    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    public static final int ENVIRONMENT_SET_ROTATION = 1;
    public static final int ENVIRONMENT_GET_CAN_DUPE = 3;
    public static final int ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
    public static final int ENVIRONMENT_SET_PIXEL_FORMAT = 10;
    public static final int ENVIRONMENT_GET_VARIABLE = 15;
    public static final int ENVIRONMENT_GET_VARIABLE_UPDATE = 17;
    public static final int ENVIRONMENT_GET_LOG_INTERFACE = 27;
    public static final int ENVIRONMENT_GET_SAVE_DIRECTORY = 31;
    public static final int ENVIRONMENT_SET_SYSTEM_AV_INFO = 32;
    public static final int ENVIRONMENT_SET_GEOMETRY = 37;
    /** {@code 51 | RETRO_ENVIRONMENT_EXPERIMENTAL}: whether input_state answers {@link #DEVICE_ID_JOYPAD_MASK}. */
    public static final int ENVIRONMENT_GET_INPUT_BITMASKS = 51 | 0x10000;
    /** Offset of {@code aspect_ratio} in {@code retro_game_geometry}, which also starts {@code retro_system_av_info}. */
    public static final int GEOMETRY_ASPECT_RATIO_OFFSET = 16;

    public static final int PIXEL_FORMAT_0RGB1555 = 0;
    public static final int PIXEL_FORMAT_XRGB8888 = 1;
    public static final int PIXEL_FORMAT_RGB565 = 2;

    public static final int MEMORY_SAVE_RAM = 0;

    public static final int DEVICE_JOYPAD = 1;
    public static final int DEVICE_ID_JOYPAD_B = 0;
    public static final int DEVICE_ID_JOYPAD_Y = 1;
    public static final int DEVICE_ID_JOYPAD_SELECT = 2;
    public static final int DEVICE_ID_JOYPAD_START = 3;
    public static final int DEVICE_ID_JOYPAD_UP = 4;
    public static final int DEVICE_ID_JOYPAD_DOWN = 5;
    public static final int DEVICE_ID_JOYPAD_LEFT = 6;
    public static final int DEVICE_ID_JOYPAD_RIGHT = 7;
    public static final int DEVICE_ID_JOYPAD_A = 8;
    public static final int DEVICE_ID_JOYPAD_X = 9;
    public static final int DEVICE_ID_JOYPAD_L = 10;
    public static final int DEVICE_ID_JOYPAD_R = 11;
    public static final int DEVICE_ID_JOYPAD_L2 = 12;
    public static final int DEVICE_ID_JOYPAD_R2 = 13;
    public static final int DEVICE_ID_JOYPAD_L3 = 14;
    public static final int DEVICE_ID_JOYPAD_R3 = 15;
    /** Asks for all buttons at once as a bitmask. */
    public static final int DEVICE_ID_JOYPAD_MASK = 256;


    public static final int DEVICE_ANALOG = 5;
    public static final int DEVICE_INDEX_ANALOG_LEFT = 0;
    public static final int DEVICE_INDEX_ANALOG_RIGHT = 1;
    public static final int DEVICE_ID_ANALOG_X = 0;
    public static final int DEVICE_ID_ANALOG_Y = 1;

    /** Absolute position over the picture, -0x7FFF (left, top) to 0x7FFF (right, bottom). */
    public static final int DEVICE_POINTER = 6;
    public static final int DEVICE_ID_POINTER_X = 0;
    public static final int DEVICE_ID_POINTER_Y = 1;
    public static final int DEVICE_ID_POINTER_PRESSED = 2;

    private LibretroConstants() {
    }
}
