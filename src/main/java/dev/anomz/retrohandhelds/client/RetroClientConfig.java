package dev.anomz.retrohandhelds.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** This player's own settings ({@code config/retrohandhelds-client.toml}), also editable from the Mods menu. */
public final class RetroClientConfig {
    public static final int VOLUME_STEPS = 10;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue VOLUME = BUILDER
            .comment("Emulator volume in steps of 10% (0-10), on top of the Jukebox/Note Blocks slider.")
            .defineInRange("volume", VOLUME_STEPS, 0, VOLUME_STEPS);
    private static final ModConfigSpec.BooleanValue MUTED = BUILDER
            .comment("Mute the emulator.")
            .define("muted", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RetroClientConfig() {
    }

    public static int volume() {
        return SPEC.isLoaded() ? VOLUME.get() : VOLUME_STEPS;
    }

    public static boolean muted() {
        return SPEC.isLoaded() && MUTED.get();
    }

    public static void setVolume(int volume) {
        VOLUME.set(Math.clamp(volume, 0, VOLUME_STEPS));
        VOLUME.save();
    }

    public static void setMuted(boolean muted) {
        MUTED.set(muted);
        MUTED.save();
    }
}
