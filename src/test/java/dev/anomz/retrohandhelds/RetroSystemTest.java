package dev.anomz.retrohandhelds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.anomz.retrohandhelds.client.emu.SaveFiles;

class RetroSystemTest {
    @Test
    void systemComesFromTheExtension() {
        assertEquals(Optional.of(RetroSystem.GAME_BOY_ADVANCE), RetroSystem.forFileName("Game.gba"));
        assertEquals(Optional.of(RetroSystem.GAME_BOY_ADVANCE), RetroSystem.forFileName("Game.GB"));
        assertEquals(Optional.of(RetroSystem.NES), RetroSystem.forFileName("Game.nes"));
        assertEquals(Optional.of(RetroSystem.SNES), RetroSystem.forFileName("Game.sfc"));
        assertEquals(Optional.of(RetroSystem.SNES), RetroSystem.forFileName("Game.smc"));
        assertEquals(Optional.of(RetroSystem.GENESIS), RetroSystem.forFileName("Sonic (USA, Europe).md"));
        assertEquals(Optional.of(RetroSystem.GENESIS), RetroSystem.forFileName("Game.gen"));
        assertEquals(Optional.of(RetroSystem.MASTER_SYSTEM), RetroSystem.forFileName("Game.gg"));
    }

    @Test
    void unknownOrAmbiguousExtensionsAreRejected() {
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game.bin"));
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game"));
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game.gba.txt"));
        assertTrue(RomNames.isValid("Game.sms"));
        assertFalse(RomNames.isValid("Game.n64"));
    }

    @Test
    void statesAreKeptPerCoreButMgbaKeepsItsOldNames() {
        SaveFiles mgba = SaveFiles.forId(Path.of("saves"), "id", "mgba").orElseThrow();
        SaveFiles snes9x = SaveFiles.forId(Path.of("saves"), "id", "snes9x").orElseThrow();
        assertEquals("state.st", mgba.state().getFileName().toString());
        assertEquals("resume.snes9x.st", snes9x.resume().getFileName().toString());
        assertEquals(mgba.saveRam(), snes9x.saveRam());
    }
}
