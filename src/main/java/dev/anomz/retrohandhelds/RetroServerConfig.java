package dev.anomz.retrohandhelds;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings for server owners ({@code config/retrohandhelds-server.toml}; a world can override it in {@code serverconfig/}).
 * NeoForge syncs these to clients, so client-side checks see the server's values.
 */
public final class RetroServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_CONSOLES = BUILDER
            .comment("Allow playing games on Retro Console blocks. Ejecting cartridges always works.")
            .define("enableConsoles", true);
    private static final ModConfigSpec.BooleanValue ENABLE_HANDHELDS = BUILDER
            .comment("Allow playing games on Retro Handheld items.")
            .define("enableHandhelds", true);

    private static final ModConfigSpec.BooleanValue SCREEN_SHARING = BUILDER
            .comment("Let players near a console see the game being played on it (and on TVs next to it).",
                    "The server only forwards compressed pictures; it never runs games.")
            .define("screenSharing", true);
    private static final ModConfigSpec.IntValue SCREEN_SHARE_FPS = BUILDER
            .comment("Pictures per second sent for a shared screen.")
            .defineInRange("screenShareFps", 10, 1, 20);
    private static final ModConfigSpec.IntValue SCREEN_SHARE_RADIUS = BUILDER
            .comment("Players within this many blocks of a console see its shared screen.")
            .defineInRange("screenShareRadius", 32, 8, 128);
    private static final ModConfigSpec.IntValue SCREEN_SHARE_KBPS = BUILDER
            .comment("Most kilobytes per second one player may share. Anything above is dropped.")
            .defineInRange("screenShareKBps", 96, 16, 1024);

    static final ModConfigSpec SPEC = BUILDER.build();

    private RetroServerConfig() {
    }

    // Before a world loads the config is not there yet; default to allowed.
    public static boolean consolesEnabled() {
        return !SPEC.isLoaded() || ENABLE_CONSOLES.get();
    }

    public static boolean handheldsEnabled() {
        return !SPEC.isLoaded() || ENABLE_HANDHELDS.get();
    }

    public static boolean screenSharingEnabled() {
        return !SPEC.isLoaded() || SCREEN_SHARING.get();
    }

    public static int screenShareFps() {
        return SPEC.isLoaded() ? SCREEN_SHARE_FPS.get() : 10;
    }

    public static int screenShareRadius() {
        return SPEC.isLoaded() ? SCREEN_SHARE_RADIUS.get() : 32;
    }

    public static int screenShareBytesPerSecond() {
        return (SPEC.isLoaded() ? SCREEN_SHARE_KBPS.get() : 96) * 1024;
    }
}
