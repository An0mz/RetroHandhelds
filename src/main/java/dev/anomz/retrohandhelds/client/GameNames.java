package dev.anomz.retrohandhelds.client;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.anomz.retrohandhelds.RomNames;

/** How games are labelled and found in the game picker. */
public final class GameNames {
    /** Short system tag per file extension, as shown before a game's name. */
    private static final Map<String, String> TAGS = Map.ofEntries(
            Map.entry("gba", "GBA"), Map.entry("gb", "GB"), Map.entry("gbc", "GBC"),
            Map.entry("nes", "NES"), Map.entry("sfc", "SNES"), Map.entry("smc", "SNES"),
            Map.entry("md", "MD"), Map.entry("gen", "MD"), Map.entry("smd", "MD"), Map.entry("32x", "32X"),
            Map.entry("sms", "SMS"), Map.entry("gg", "GG"), Map.entry("sg", "SG"),
            Map.entry("n64", "N64"), Map.entry("v64", "N64"), Map.entry("z64", "N64"),
            Map.entry("cue", "PS1"), Map.entry("chd", "PS1"), Map.entry("m3u", "PS1"), Map.entry("pbp", "PS1"),
            Map.entry("nds", "DS"), Map.entry("pce", "PCE"), Map.entry("ngp", "NGP"), Map.entry("ngc", "NGPC"),
            Map.entry("ws", "WS"), Map.entry("wsc", "WSC"), Map.entry("lnx", "LYNX"),
            Map.entry("a26", "2600"), Map.entry("a78", "7800"), Map.entry("min", "MINI"));

    private GameNames() {
    }

    /** "Super Mario 64 (USA).z64" gives "N64"; an unknown extension gives "". */
    public static String tag(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : TAGS.getOrDefault(fileName.substring(dot + 1).toLowerCase(Locale.ROOT), "");
    }

    /** Whether {@code fileName} matches what the player typed: every word, in the name or the tag. */
    public static boolean matches(String fileName, String query) {
        String haystack = (RomNames.displayName(fileName) + " " + tag(fileName)).toLowerCase(Locale.ROOT);
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!word.isEmpty() && !haystack.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** The games matching {@code query}, in their original order. */
    public static List<String> filter(List<String> fileNames, String query) {
        return fileNames.stream().filter(name -> matches(name, query)).toList();
    }
}
