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
}
