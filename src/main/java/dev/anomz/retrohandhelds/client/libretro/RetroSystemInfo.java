package dev.anomz.retrohandhelds.client.libretro;

import com.sun.jna.Structure;

/** {@code struct retro_system_info}. The strings belong to the core. C99 {@code bool} is {@code byte}. */
@Structure.FieldOrder({ "libraryName", "libraryVersion", "validExtensions", "needFullpath", "blockExtract" })
public class RetroSystemInfo extends Structure {
    public String libraryName;
    public String libraryVersion;
    public String validExtensions;
    /** Non-zero: the core loads the game from {@code path} itself and ignores {@code data}. */
    public byte needFullpath;
    public byte blockExtract;
}
