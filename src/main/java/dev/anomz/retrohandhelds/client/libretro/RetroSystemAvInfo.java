package dev.anomz.retrohandhelds.client.libretro;

import com.sun.jna.Structure;

/** {@code struct retro_system_av_info}; the nested structs are laid out inline as in C. */
@Structure.FieldOrder({ "geometry", "timing" })
public class RetroSystemAvInfo extends Structure {
    public Geometry geometry = new Geometry();
    public Timing timing = new Timing();

    @Structure.FieldOrder({ "baseWidth", "baseHeight", "maxWidth", "maxHeight", "aspectRatio" })
    public static class Geometry extends Structure {
        public int baseWidth;
        public int baseHeight;
        public int maxWidth;
        public int maxHeight;
        public float aspectRatio;
    }

    @Structure.FieldOrder({ "fps", "sampleRate" })
    public static class Timing extends Structure {
        public double fps;
        public double sampleRate;
    }
}
