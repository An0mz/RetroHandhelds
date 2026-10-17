package dev.anomz.retrohandhelds.client.emu;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/** Where one cartridge keeps its saves: {@code <saves>/<saveId>/game.sav} and {@code state.st}. */
public record SaveFiles(Path saveRam, Path state) {
    // Save IDs arrive from the server (cartridge data), so they must not be able to name another folder.
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    /** Empty if {@code saveId} is not a safe folder name. */
    public static Optional<SaveFiles> forId(Path savesRoot, String saveId) {
        if (!VALID_ID.matcher(saveId).matches() || saveId.startsWith(".")) {
            return Optional.empty();
        }
        Path dir = savesRoot.resolve(saveId);
        return Optional.of(new SaveFiles(dir.resolve("game.sav"), dir.resolve("state.st")));
    }

    /** A save ID derived from a file name, for sessions without a cartridge. */
    public static String idFromName(String prefix, String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        String id = prefix + cleaned;
        return id.length() > 80 ? id.substring(0, 80) : id;
    }
}
