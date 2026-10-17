package dev.anomz.retrohandhelds.client.libretro;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/** {@code struct retro_game_info}. */
@Structure.FieldOrder({ "path", "data", "size", "meta" })
public class RetroGameInfo extends Structure {
    public String path;
    public Pointer data;
    public long size;
    public String meta;
}
