# Retro Handhelds

Play your own GBA games inside Minecraft. Put a cartridge in a console block or a handheld, and play on a full-screen view, on the block itself, or in your hands while you walk around.

Powered by [mGBA](https://mgba.io). **No games or BIOS files are included; you bring your own.**

![Build](https://github.com/An0mz/RetroHandhelds/actions/workflows/build.yml/badge.svg)

## Features

- **Retro Console** block: insert a cartridge, power it on, and the game shows live on the block's screen, with sound that fades with distance.
- **Retro Handheld** item: carry a game with you. Held in your hands it shows like a map, with the game playing on its screen.
- **Cartridges** remember which ROM they play and keep their own saves, so a game continues across consoles and handhelds.
- In-game saves (battery save RAM) are written automatically, plus a save-state slot per cartridge.
- Sound through Minecraft's audio, with its own volume and mute keys.
- Keyboard and gamepad (Xbox, PlayStation and other controllers GLFW recognises), all rebindable keys.
- Fast-forward (2x to 4x), optional GBA color correction and LCD ghosting.
- Also runs Game Boy and Game Boy Color games.
- Server-friendly: all emulation runs on each player's own computer, so the server does no extra work.

## Requirements

- Minecraft 1.21.1 with NeoForge 21.1.x (Java 21)
- Install on **both** the client and the server (the mod adds blocks and items).
- Windows x64, Linux x64, or macOS (Apple Silicon or Intel). Linux and macOS are checked automatically in CI; Windows is also tested in game.

## Getting started

1. Start Minecraft once with the mod installed. It creates `retro_handhelds/roms/` in your Minecraft folder (next to `mods/`).
2. Put your own `.gba`, `.gb` or `.gbc` files in `retro_handhelds/roms/`.
3. Craft a **Cartridge**, hold it and right-click: pick a ROM from the list.
4. Play it:
   - **Console:** place a Retro Console, right-click it with the cartridge to insert, then right-click with an empty hand to power on. Sneak + right-click with an empty hand ejects the cartridge.
   - **Handheld:** in your inventory, right-click the cartridge onto the Retro Handheld (right-click the handheld with an empty cursor to take it out), then right-click with the handheld in hand.
5. Press **Esc** to go back to Minecraft. The game keeps running on the console or the handheld; right-click again to pick it back up. The **Power Off** button in the corner shuts it down.

ROMs never leave your computer: a cartridge only stores the ROM's file name. In multiplayer, each player needs the ROM in their own `roms` folder.

### Recipes

| Item | Recipe |
|---|---|
| Retro Console | 7 iron ingots around a glass pane, redstone at the bottom center (`III / IGI / IRI`) |
| Retro Handheld | `IGI / RIR` (iron ingot, glass pane, redstone) |
| Cartridge | Shapeless: iron ingot + redstone + paper |

## Controls

All keys are under **Retro Handhelds** in *Options → Controls → Key Binds*. A hint with your current keys shows for a few seconds when a game opens.

| Action | Keyboard | Gamepad |
|---|---|---|
| D-pad | Arrow keys | D-pad or left stick |
| A / B | X / Z | A / B (or Nintendo positions, see config) |
| L / R | A / S | Bumpers or triggers |
| Start / Select | Enter / Backspace | Start / Back (Options / Share) |
| Fast-forward (hold) | Tab | |
| Save / load state | F5 / F9 | |
| Mute, volume down/up | M, -, = (also in the world) | |
| Back to Minecraft | Esc | |

The game pauses when Minecraft pauses (singleplayer). On servers and LAN worlds Minecraft keeps running, and so does the game.

## Saves

Everything is stored in your Minecraft folder under `retro_handhelds/`:

| Folder | Contents |
|---|---|
| `roms/` | Your ROMs |
| `saves/<cartridge id>/game.sav` | In-game save, written every few seconds when it changes and when the game stops |
| `saves/<cartridge id>/state.st` | Save state (F5 / F9) |
| `system/` | Optional BIOS files. Not needed: mGBA has a built-in replacement |

Saves belong to the cartridge, not the console: give a cartridge to someone and it starts their own save on their computer.

## Configuration

**Player settings** (`config/retrohandhelds-client.toml`, also in *Mods → Retro Handhelds → Config*):

| Option | Default | |
|---|---|---|
| `volume` | 10 | 0-10, on top of the Jukebox/Note Blocks slider |
| `muted` | false | |
| `colorCorrection` | false | Colors like a real GBA screen |
| `lcdGhosting` | false | Frame blending like the original LCD |
| `fastForwardSpeed` | 2 | 2-4x |
| `gamepadEnabled` | true | |
| `gamepadDeadzone` | 35 | Left stick deadzone, in % |
| `gamepadPositionalAB` | false | Use Nintendo button positions instead of pad labels |

**Server settings** (`config/retrohandhelds-server.toml`): `enableConsoles` and `enableHandhelds`. When off, games can't be started, but cartridges can always be ejected.

## Commands

- `/retrohandhelds play <rom>`: play a ROM directly, without a console (saves per ROM file).
- `/retrohandhelds test <rom>`: runs a ROM for 5 seconds without a screen and writes the last frame to `retro_handhelds/debug_frame.png`. Useful for bug reports.

## Building from source

```bash
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runClient` starts a development client. Requires JDK 21.

## Legal

Retro Handhelds does not include, download or link to any games or BIOS files. Only play games you own. This project is not affiliated with or endorsed by Nintendo.

## License and credits

- Retro Handhelds is licensed under the [MIT License](LICENSE).
- Emulation by [mGBA](https://github.com/mgba-emu/mgba) by Vicki Pfau and contributors, licensed under the Mozilla Public License 2.0 ([LICENSE-mGBA.txt](LICENSE-mGBA.txt)). The bundled cores are the libretro builds from the [libretro buildbot](https://buildbot.libretro.com/nightly/).
- Build files come from the [NeoForge MDK](https://github.com/NeoForged/MDK) template (MIT, [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)).
