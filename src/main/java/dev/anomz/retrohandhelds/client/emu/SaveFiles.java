package dev.anomz.retrohandhelds.client.emu;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Where one cartridge keeps its saves, in {@code <saves>/<saveId>/}: {@code game.sav} (the game's own
 * save), {@code state.st} (the player's save-state slot) and {@code resume.st} (written automatically
 * when the game stops, to continue where the player left off). States only load in the core that
 * wrote them, so cores other than mGBA get their own, e.g. {@code state.snes9x.st}; the game's own
 * save is the same format in every core and stays shared.
 */
public record SaveFiles(Path saveRam, Path state, Path resume) {
    // Save IDs arrive from the server (cartridge data), so they must not be able to name another folder.
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    /** Empty if {@code saveId} is not a safe folder name. */
    public static Optional<SaveFiles> forId(Path savesRoot, String saveId, String coreName) {
        if (!VALID_ID.matcher(saveId).matches() || saveId.startsWith(".")) {
            return Optional.empty();
        }
        Path dir = savesRoot.resolve(saveId);
        // mGBA keeps the names from before other cores existed, so existing states still load.
        String suffix = coreName.equals("mgba") ? ".st" : "." + coreName + ".st";
        return Optional.of(new SaveFiles(dir.resolve("game.sav"), dir.resolve("state" + suffix), dir.resolve("resume" + suffix)));
    }

    /** A save ID derived from a file name, for sessions without a cartridge. */
    public static String idFromName(String prefix, String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        String id = prefix + cleaned;
        return id.length() > 80 ? id.substring(0, 80) : id;
    }
}
