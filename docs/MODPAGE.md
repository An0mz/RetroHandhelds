# Mod page kit (Modrinth / CurseForge)

Everything needed to fill in a project page. Copy the parts you need.

## Basics

| Field | Value |
|---|---|
| Name | Retro Handhelds |
| Slug / URL | `retro-handhelds` |
| Icon | `docs/icon.png` (256x256) |
| Loader | NeoForge |
| Game version | 1.21.1 |
| Environment | Client: required. Server: required |
| License | MIT |
| Source | https://github.com/An0mz/RetroHandhelds |
| Issues | https://github.com/An0mz/RetroHandhelds/issues |

Suggested categories (Modrinth): **Decoration**, **Utility**, **Technology**.

Keep "Nintendo", "Game Boy" and their logos out of the project name, icon and screenshots. Plain mentions like "GBA games" in the text are fine.

## Summary (short, max 256 characters)

> Play your own GBA games inside Minecraft: on a console block, on a handheld you carry, or full screen. Saves, sound, gamepad support. Powered by mGBA. No games included.

## Description

```markdown
# Retro Handhelds

Play your own GBA games inside Minecraft.

Put a cartridge in a **Retro Console** and the game plays live on the block's screen. Carry a **Retro Handheld** and it shows in your hands like a map while you walk around. Or go full screen and just play.

**No games or BIOS files are included: you bring your own.**

## Features
- Console block with the game on its screen, and sound that fades with distance
- Handheld item that shows the game in your hands
- Cartridges remember their game and keep their own saves, across consoles and handhelds
- Automatic in-game saves plus save states (F5 / F9)
- Keyboard and gamepad (Xbox, PlayStation and more), all rebindable
- Fast-forward up to 4x, optional GBA color correction and LCD ghosting
- Also plays Game Boy and Game Boy Color games
- Server friendly: games run on each player's own computer, never on the server

## How to start
1. Launch once, then put your `.gba`, `.gb` or `.gbc` files in `retro_handhelds/roms/` in your Minecraft folder.
2. Craft a Cartridge, right-click it and pick a game.
3. Insert it into a Retro Console (right-click), then right-click the console with an empty hand. Or right-click the cartridge onto a Retro Handheld in your inventory, then use the handheld.

Full guide, controls and config: [README on GitHub](https://github.com/An0mz/RetroHandhelds#readme)

## Multiplayer
Install on the server and on every client. Each player needs the game file in their own `roms` folder; only the file name is stored on the cartridge. Server owners can turn consoles or handhelds off in `retrohandhelds-server.toml`.

## Credits
Emulation by [mGBA](https://mgba.io) (MPL 2.0). Not affiliated with or endorsed by Nintendo. Only play games you own.
```

## Screenshots to take

Use F2 in game (saved to `screenshots/` in your Minecraft folder). Hide the HUD with F1 for clean shots. Use a game you're comfortable showing publicly.

1. **Hero shot:** a Retro Console in a nice build, game running on its screen, player standing in front of it.
2. **Handheld in hand:** first person, two-handed pose, game on its screen, an interesting background (sunset, village).
3. **Full-screen play:** the emulator screen right after opening, with the controls hint visible.
4. **ROM picker:** the "Choose a ROM" list with a few games (blur or crop file names you'd rather not show).
5. **Multiple consoles:** a small "arcade room" of console blocks (only one runs at a time, so show one on).
6. **Config screen:** Mods → Retro Handhelds → Config, to show the options.
7. Optional: a short GIF of walking around with the handheld playing.

## Release checklist

1. Bump `mod_version` in `gradle.properties` (for example to 1.0.0 for the first public release).
2. Push to GitHub and wait for the CI run to pass on all four platforms.
3. Download the jar from the CI run's artifacts, or run `./gradlew build` and take `build/libs/retrohandhelds-<version>.jar`.
4. Upload it with a short changelog.
