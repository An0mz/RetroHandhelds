package dev.anomz.retrohandhelds.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** This player's own settings ({@code config/retrohandhelds-client.toml}), also editable from the Mods menu. */
public final class RetroClientConfig {
    public static final int VOLUME_STEPS = 10;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue VOLUME = BUILDER
            .comment("Emulator volume in steps of 10% (0-10), on top of the Jukebox/Note Blocks slider.")
            .defineInRange("volume", VOLUME_STEPS, 0, VOLUME_STEPS);
    private static final ModConfigSpec.IntValue OTHER_PLAYERS_VOLUME = BUILDER
            .comment("Volume of other players' consoles and handhelds in steps of 10% (0-10).",
                    "Changed in game with the Other Players' Volume keys.")
            .defineInRange("otherPlayersVolume", VOLUME_STEPS, 0, VOLUME_STEPS);
    private static final ModConfigSpec.BooleanValue MUTED = BUILDER
            .comment("Mute your own game.")
            .define("muted", false);
    private static final ModConfigSpec.BooleanValue OTHER_PLAYERS_MUTED = BUILDER
            .comment("Mute other players' games, without touching your own.")
            .define("otherPlayersMuted", false);

    private static final ModConfigSpec.BooleanValue COLOR_CORRECTION = BUILDER
            .comment("Adjust colors to look like a real GBA/GBC screen (less saturated, slightly darker).")
            .define("colorCorrection", false);
    private static final ModConfigSpec.BooleanValue LCD_GHOSTING = BUILDER
            .comment("Blend each frame with the previous one, like the slow LCD of the original hardware.")
            .define("lcdGhosting", false);

    private static final ModConfigSpec.BooleanValue IN_WORLD_VIEW = BUILDER
            .comment("Open games in the in-world view (watch the console or handheld) instead of full screen.",
                    "Toggled in game with the View key.")
            .define("inWorldView", false);

    private static final ModConfigSpec.IntValue FAST_FORWARD_SPEED = BUILDER
            .comment("How fast the game runs while the Fast-Forward key is held (2-4x).")
            .defineInRange("fastForwardSpeed", 2, 2, 4);

    private static final ModConfigSpec.BooleanValue GAMEPAD_ENABLED = BUILDER
            .comment("Use a connected gamepad (Xbox, PlayStation, ...) in the emulator screen.")
            .define("gamepadEnabled", true);
    private static final ModConfigSpec.IntValue GAMEPAD_DEADZONE = BUILDER
            .comment("How far (in %) the left stick must move before it counts as a D-pad press.")
            .defineInRange("gamepadDeadzone", 35, 10, 90);
    private static final ModConfigSpec.BooleanValue GAMEPAD_POSITIONAL_AB = BUILDER
            .comment("Use Nintendo button positions: the right face button is A and the bottom one is B.",
                    "When false, the pad's A/B labels match the game's A/B.")
            .define("gamepadPositionalAB", false);

    private static final ModConfigSpec.BooleanValue SHARE_MY_SCREEN = BUILDER
            .comment("Let players near your console see your game (if the server allows screen sharing).")
            .define("shareMyScreen", true);
    private static final ModConfigSpec.BooleanValue SHOW_OTHER_SCREENS = BUILDER
            .comment("Show the games other players share on their consoles and TVs.")
            .define("showOtherScreens", true);

    private static final ModConfigSpec.BooleanValue RESUME_GAMES = BUILDER
            .comment("Continue games where you left off: when a game stops (you close Minecraft, walk away,",
                    "put the handheld away, power off...) its exact state is saved, and restored the next time",
                    "you play that cartridge. Separate from your own save-state slot.")
            .define("resumeGames", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RetroClientConfig() {
    }

    public static int volume() {
        return SPEC.isLoaded() ? VOLUME.get() : VOLUME_STEPS;
    }

    public static int otherPlayersVolume() {
        return SPEC.isLoaded() ? OTHER_PLAYERS_VOLUME.get() : VOLUME_STEPS;
    }

    public static void setOtherPlayersVolume(int volume) {
        OTHER_PLAYERS_VOLUME.set(Math.clamp(volume, 0, VOLUME_STEPS));
        OTHER_PLAYERS_VOLUME.save();
    }

    public static boolean otherPlayersMuted() {
        return SPEC.isLoaded() && OTHER_PLAYERS_MUTED.get();
    }

    public static void setOtherPlayersMuted(boolean muted) {
        OTHER_PLAYERS_MUTED.set(muted);
        OTHER_PLAYERS_MUTED.save();
    }

    public static boolean muted() {
        return SPEC.isLoaded() && MUTED.get();
    }

    public static boolean colorCorrection() {
        return SPEC.isLoaded() && COLOR_CORRECTION.get();
    }

    public static boolean lcdGhosting() {
        return SPEC.isLoaded() && LCD_GHOSTING.get();
    }

    public static boolean inWorldView() {
        return SPEC.isLoaded() && IN_WORLD_VIEW.get();
    }

    public static void setInWorldView(boolean inWorld) {
        IN_WORLD_VIEW.set(inWorld);
        IN_WORLD_VIEW.save();
    }

    public static int fastForwardSpeed() {
        return SPEC.isLoaded() ? FAST_FORWARD_SPEED.get() : 2;
    }

    public static boolean gamepadEnabled() {
        return !SPEC.isLoaded() || GAMEPAD_ENABLED.get();
    }

    public static float gamepadDeadzone() {
        return (SPEC.isLoaded() ? GAMEPAD_DEADZONE.get() : 35) / 100f;
    }

    public static boolean gamepadPositionalAB() {
        return SPEC.isLoaded() && GAMEPAD_POSITIONAL_AB.get();
    }

    public static boolean shareMyScreen() {
        return !SPEC.isLoaded() || SHARE_MY_SCREEN.get();
    }

    public static boolean resumeGames() {
        return !SPEC.isLoaded() || RESUME_GAMES.get();
    }

    public static boolean showOtherScreens() {
        return !SPEC.isLoaded() || SHOW_OTHER_SCREENS.get();
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
