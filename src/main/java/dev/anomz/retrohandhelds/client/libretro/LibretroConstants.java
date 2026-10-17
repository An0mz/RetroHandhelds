package dev.anomz.retrohandhelds.client.libretro;

/** Values from libretro.h that this mod uses. */
public final class LibretroConstants {
    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    public static final int ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
    public static final int ENVIRONMENT_SET_PIXEL_FORMAT = 10;
    public static final int ENVIRONMENT_GET_SAVE_DIRECTORY = 31;

    public static final int PIXEL_FORMAT_0RGB1555 = 0;
    public static final int PIXEL_FORMAT_XRGB8888 = 1;
    public static final int PIXEL_FORMAT_RGB565 = 2;

    private LibretroConstants() {
    }
}
