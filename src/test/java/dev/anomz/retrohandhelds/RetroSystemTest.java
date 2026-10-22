package dev.anomz.retrohandhelds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    void newSystemsComeFromTheirExtensions() {
        assertEquals(Optional.of(RetroSystem.N64), RetroSystem.forFileName("Super Mario 64 (USA).z64"));
        assertEquals(Optional.of(RetroSystem.N64), RetroSystem.forFileName("Game.v64"));
        assertEquals(Optional.of(RetroSystem.PLAYSTATION), RetroSystem.forFileName("Game (Disc 1).cue"));
        assertEquals(Optional.of(RetroSystem.PLAYSTATION), RetroSystem.forFileName("Game.chd"));
        assertEquals(Optional.of(RetroSystem.NINTENDO_DS), RetroSystem.forFileName("Game.nds"));
        assertEquals(Optional.of(RetroSystem.SEGA_32X), RetroSystem.forFileName("Game.32x"));
        assertEquals(Optional.of(RetroSystem.MASTER_SYSTEM), RetroSystem.forFileName("Game.sg"));
        assertEquals(Optional.of(RetroSystem.WONDERSWAN), RetroSystem.forFileName("Game.wsc"));
        assertEquals(Optional.of(RetroSystem.POKEMON_MINI), RetroSystem.forFileName("Game.min"));
    }

    @Test
    void noExtensionBelongsToTwoSystems() {
        Set<String> seen = new HashSet<>();
        for (RetroSystem system : RetroSystem.values()) {
            for (String extension : system.extensions()) {
                assertTrue(seen.add(extension), "." + extension + " is claimed twice");
            }
        }
    }

    @Test
    void analogSystemsUseL2R2() {
        assertTrue(RetroSystem.N64.controls().analog);
        assertFalse(RetroSystem.N64.controls().stickPressesDpad);
        assertTrue(RetroSystem.PLAYSTATION.controls().triggersAreL2R2);
        assertTrue(RetroSystem.NINTENDO_DS.controls().touch());
        assertFalse(RetroSystem.NES.controls().analog);
    }

    @Test
    void unknownOrAmbiguousExtensionsAreRejected() {
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game.bin"));
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game"));
        assertEquals(Optional.empty(), RetroSystem.forFileName("Game.gba.txt"));
        assertTrue(RomNames.isValid("Game.sms"));
        assertFalse(RomNames.isValid("Game.iso"));
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
