package dev.anomz.retrohandhelds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.anomz.retrohandhelds.client.emu.SaveFiles;

/** File names and IDs that arrive over the network must never reach outside the mod's folders. */
class SafetyChecksTest {
    @Test
    void romNamesAcceptPlainRomFiles() {
        assertTrue(RomNames.isValid("Some Game (USA).gba"));
        assertTrue(RomNames.isValid("classic.GB"));
        assertTrue(RomNames.isValid("color.gbc"));
    }

    @Test
    void romNamesRejectPathsAndOddFiles() {
        assertFalse(RomNames.isValid(null));
        assertFalse(RomNames.isValid(""));
        assertFalse(RomNames.isValid("../../evil.gba"));
        assertFalse(RomNames.isValid("sub/dir.gba"));
        assertFalse(RomNames.isValid("C:\\evil.gba"));
        assertFalse(RomNames.isValid("C:evil.gba"));
        assertFalse(RomNames.isValid(".hidden.gba"));
        assertFalse(RomNames.isValid("notes.txt"));
        assertFalse(RomNames.isValid("x".repeat(RomNames.MAX_LENGTH) + ".gba"));
    }

    @Test
    void romDisplayNameDropsExtension() {
        assertEquals("Some Game (USA)", RomNames.displayName("Some Game (USA).gba"));
    }

    @Test
    void saveIdsStayInsideTheSavesFolder() {
        Path saves = Path.of("saves");
        assertTrue(SaveFiles.forId(saves, "9d17dff2-6061-4101-9138-1cb965502b6c", "mgba").isPresent());
        assertFalse(SaveFiles.forId(saves, "../escape", "mgba").isPresent());
        assertFalse(SaveFiles.forId(saves, "..", "mgba").isPresent());
        assertFalse(SaveFiles.forId(saves, "a/b", "mgba").isPresent());
        assertFalse(SaveFiles.forId(saves, "", "mgba").isPresent());
    }

    @Test
    void saveIdFromNameIsAlwaysSafe() {
        String id = SaveFiles.idFromName("standalone_", "../We!rd Name (USA).gba");
        assertTrue(SaveFiles.forId(Path.of("saves"), id, "mgba").isPresent(), id);
    }
}
