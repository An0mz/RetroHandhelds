package dev.anomz.retrohandhelds.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameNamesTest {
    @Test
    void tagsComeFromTheExtension() {
        assertEquals("N64", GameNames.tag("Super Mario 64 (USA).z64"));
        assertEquals("GB", GameNames.tag("Tetris.GB"));
        assertEquals("PS1", GameNames.tag("Crash Bandicoot (USA).cue"));
        assertEquals("MD", GameNames.tag("Sonic.md"));
        assertEquals("", GameNames.tag("notes.txt"));
        assertEquals("", GameNames.tag("no extension"));
    }

    @Test
    void searchMatchesEveryWordInTheNameOrTag() {
        assertTrue(GameNames.matches("Super Mario 64 (USA).z64", "mario"));
        assertTrue(GameNames.matches("Super Mario 64 (USA).z64", "MARIO n64"));
        assertTrue(GameNames.matches("Super Mario 64 (USA).z64", "  "), "an empty search shows everything");
        assertFalse(GameNames.matches("Super Mario 64 (USA).z64", "mario gba"));
        assertFalse(GameNames.matches("Super Mario 64 (USA).z64", "z64"), "the extension itself is not searched");
    }

    @Test
    void filterKeepsTheOrder() {
        List<String> games = List.of("Pokemon Emerald.gba", "Super Mario 64.z64", "Pokemon Snap.z64");
        assertEquals(List.of("Pokemon Emerald.gba", "Pokemon Snap.z64"), GameNames.filter(games, "pokemon"));
        assertEquals(List.of("Super Mario 64.z64", "Pokemon Snap.z64"), GameNames.filter(games, "n64"));
    }
}
