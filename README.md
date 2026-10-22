# Retro Handhelds

Play your own retro games inside Minecraft: Game Boy, GBA, NES, SNES, N64, PlayStation, Nintendo DS, Genesis, Master System and more. Put a cartridge in a console block or a handheld, and play on a full-screen view, on the block itself, or in your hands while you walk around.

Powered by [libretro](https://www.libretro.com) cores: [mGBA](https://mgba.io) and others, bundled, plus any libretro core you add yourself. **No games or BIOS files are included; you bring your own.**

![Build](https://github.com/An0mz/RetroHandhelds/actions/workflows/build.yml/badge.svg)

What changed in each version: [CHANGELOG.md](CHANGELOG.md).

## Features

- **Retro Console** block: insert a cartridge, power it on, and the game shows live on the block's screen, with sound that fades with distance.
- **Retro Handheld** item: carry a game with you. Held in your hands it shows like a map, with the game playing on its screen.
- **Retro TV** block: place it next to a running console to show the game on it. TVs in a rectangle join into one big screen.
- **Retro Remote** item: link it to a console and play from across the room.
- Play full screen, or switch to the in-world view and watch the game on the console, TV or handheld while you play, with the HUD hidden.
- **Cartridges** remember which ROM they play and keep their own saves, so a game continues across consoles and handhelds.
- In-game saves (battery save RAM) are written automatically, plus a save-state slot per cartridge.
- Games continue where you left off: close Minecraft, walk away or put the handheld down, and the next time you play that cartridge you're right back where you were.
- Sound through Minecraft's audio, with its own volume and mute keys, and separate ones for other players' games.
- Keyboard and gamepad (Xbox, PlayStation and other controllers GLFW recognises), all rebindable keys.
- Fast-forward (2x to 4x), optional GBA color correction and LCD ghosting.
- Runs Game Boy, Game Boy Color, GBA, NES, SNES, N64, PlayStation, Nintendo DS, Genesis / Mega Drive, 32X, Master System, Game Gear, SG-1000, PC Engine, Neo Geo Pocket, WonderSwan, Atari Lynx, Atari 2600 / 7800 and Pokemon Mini games.
- Add your own [libretro](https://www.libretro.com) cores (Genesis Plus GX, Snes9x, ...) to use them instead of the bundled ones.
- Multiplayer: players near a console see and hear the game being played on it, on its TVs too.
- Server-friendly: all emulation runs on each player's own computer; the server only forwards compressed pictures, within limits the owner sets.

## Requirements

- Minecraft 1.21.1 with NeoForge 21.1.x (Java 21)
- Install on **both** the client and the server (the mod adds blocks and items).
- Windows x64, Linux x64, or macOS (Apple Silicon or Intel). Linux and macOS are checked automatically in CI; Windows is also tested in game.

## Getting started

1. Start Minecraft once with the mod installed. It creates `retro_handhelds/roms/` in your Minecraft folder (next to `mods/`).
2. Put your own games in `retro_handhelds/roms/` (see [Systems and cores](#systems-and-cores) for the file types).
3. Craft a **Cartridge**, hold it and right-click: pick a ROM from the list.
4. Play it:
   - **Console:** place a Retro Console, right-click it with the cartridge to insert, then right-click with an empty hand to power on. Sneak + right-click with an empty hand ejects the cartridge.
   - **Handheld:** in your inventory, right-click the cartridge onto the Retro Handheld (right-click the handheld with an empty cursor to take it out), then right-click with the handheld in hand.
5. Press **V** to switch between full screen and the in-world view, where you watch the game on the console or the handheld while your controls still go to the game.
6. Press **Esc** to go back to Minecraft. The game keeps running on the console or the handheld; right-click again to pick it back up. The **Power Off** button in the corner shuts it down.

### Retro TV

Place a Retro TV so it touches a console. When that console runs a game, the TV shows it too. Place TVs facing the same way in a filled rectangle (up to 16x16) and they act as one big, seamless screen; one of them has to touch the console. TVs touching each other in any other shape (an L, a cross, a gap) aren't a screen and show nothing. The sound comes from the middle of the screen. Right-click the screen with an empty hand to play, just like the console. While a TV shows the game, the console's own screen just says "Playing on TV".

### Retro Remote

Sneak + right-click a console, or a TV screen connected to one, to link the remote. Then right-click with the remote anywhere within 48 blocks of the console to play it.

ROMs never leave your computer: a cartridge only stores the ROM's file name. In multiplayer, each player needs the ROM in their own `roms` folder.

### Recipes

| Item | Recipe |
|---|---|
| Retro Console | 7 iron ingots around a glass pane, redstone at the bottom center (`III / IGI / IRI`) |
| Retro Handheld | `IGI / RIR` (iron ingot, glass pane, redstone) |
| Cartridge | Shapeless: iron ingot + redstone + paper |
| Retro TV (x2) | `IGI / GRG / IGI` (iron ingot, glass pane, redstone) |
| Retro Remote | ` T / IBI / I ` (redstone torch, iron ingot, any button) |

## Multiplayer

When you play on a console, players within 32 blocks see your game live on that console and on any TVs touching it, and hear it coming from the console: full volume within 4 blocks, fading out by 32. They don't need the ROM. Pictures are sent at 10 per second, compressed, and only the parts that changed; sound is sent as compressed mono (about 11 KB/s). Nothing is sent when nobody is near.

Handhelds are shared too: while you hold one in your hand, nearby players see you holding it in front of you with the game on its screen, and hear the sound following you around.

Too loud? Other players' games have their own keys: **N** mutes them and **[** / **]** lower or raise their volume, without touching your own game (which keeps **M**, **-** and **=**). You can also set the volume in the config, or turn shared screens off entirely with `showOtherScreens`.

Each player who wants to *play* a cartridge needs that ROM in their own `roms` folder.

## Controls

Every console has its own keys, set up for that console's controller and named the way it names its buttons (the N64's A, B, Z and C-buttons; the PlayStation's Cross, Circle, Square, Triangle). Click **Controls** at the top of the game screen to see and change them: click a control, then press a key or mouse button (Esc unbinds). A key shown in red does something else too. Your changes are kept in `retro_handhelds/controls.json`; **Reset to preset** puts a console back to its defaults. A hint with the current keys shows for a few seconds when a game opens.

Some of the presets:

| Console | Keys |
|---|---|
| GBA, Game Boy | D-pad arrows, A / B = X / Z, L / R = A / S, Start / Select = Enter / Backspace |
| SNES, DS | D-pad arrows, A / B / X / Y = X / Z / S / A, L / R = Q / W |
| Genesis | D-pad arrows, A / B / C = Z / X / C, X / Y / Z = A / S / D |
| N64 | Stick = W A S D, A = Space, B = E, Z = Left Shift, L / R = Q / R, C-buttons = mouse and arrow keys, D-pad = I J K L |
| PlayStation | D-pad arrows, Cross / Circle / Square / Triangle = Z / X / A / S, L1 / R1 = Q / W, L2 / R2 = 1 / 3, left stick = I J K L, right stick = mouse and T F G H |

**Mouse look:** on the PlayStation the mouse moves the right stick like an analog stick, with the cursor hidden. On the N64 the right stick is the C-buttons, which games press like buttons (Super Mario 64 turns its camera one step per press, and C-Up is first-person view), so the arrow keys press them and mouse look is off. Turn it on in Controls if you like: a flick of the mouse then taps C-Left or C-Right once, and **Mouse up/down** adds C-Up and C-Down. Left Alt frees the cursor to click the screen's buttons and back. Mouse look and its sensitivity can be changed per console in Controls. On the DS the mouse is the touchscreen instead: hold the left button on the picture in the full-screen view.

**Gamepads** (Xbox, PlayStation and other controllers GLFW recognises) follow each console's layout through the standard libretro pad: face buttons, bumpers, triggers (L2 / R2 on the N64 and PlayStation, otherwise L / R too), sticks (analog on the N64 and PlayStation, otherwise the left stick also works as the D-pad) and stick clicks.

These keys work on every console, under **Retro Handhelds** in *Options → Controls → Key Binds*:

| Action | Key |
|---|---|
| Fast-forward (hold) | Tab |
| Full screen / in-world view | V |
| Save / load state | F5 / F9 |
| Free the cursor (mouse look) | Left Alt |
| Mute, volume down/up (your game) | M, -, = (also in the world) |
| Mute, volume down/up (other players' games) | N, [, ] (also in the world); never touches your own game |
| Back to Minecraft | Esc |

The game pauses when Minecraft pauses (singleplayer). On servers and LAN worlds Minecraft keeps running, and so does the game.

## Saves

Everything is stored in your Minecraft folder under `retro_handhelds/`:

| Folder | Contents |
|---|---|
| `roms/` | Your ROMs |
| `saves/<cartridge id>/game.sav` | In-game save, written every few seconds when it changes and when the game stops |
| `saves/<cartridge id>/state.st` | Save state (F5 / F9). Cores other than mGBA use their own file, e.g. `state.snes9x.st`, since states only load in the core that made them |
| `saves/<cartridge id>/resume.st` | Where you left off: saved automatically whenever the game stops (and every minute while playing), restored the next time you play that cartridge |
| `cores/` | Optional libretro cores you add yourself; see below |
| `system/` | Optional BIOS files. Not needed by the bundled cores |

## Systems and cores

The system is picked from the ROM's file extension:

| System | Extensions | Bundled core | Cores you can add (first found wins) |
|---|---|---|---|
| GBA, Game Boy, Game Boy Color | `.gba` `.gb` `.gbc` | mGBA | `mgba` |
| NES | `.nes` | FCEUmm | `nestopia`, `fceumm`, `mesen` |
| SNES | `.sfc` `.smc` | Mesen-S | `snes9x`, `mesen-s` |
| Genesis / Mega Drive | `.md` `.gen` `.smd` | BlastEm | `genesis_plus_gx`, `picodrive`, `blastem` |
| 32X | `.32x` | BlastEm | `picodrive`, `blastem` |
| Master System, Game Gear, SG-1000 | `.sms` `.gg` `.sg` | Gearsystem | `genesis_plus_gx`, `picodrive`, `gearsystem` |
| Nintendo 64 | `.n64` `.v64` `.z64` | none: download ParaLLEl-N64, see [Setting up the N64](#setting-up-the-n64) | `parallel_n64` |
| PlayStation | `.cue` `.chd` `.m3u` `.pbp` | PCSX ReARMed | `pcsx_rearmed`, `mednafen_psx` |
| Nintendo DS | `.nds` | DeSmuME | `desmume` |
| PC Engine / TurboGrafx-16 | `.pce` | Beetle PCE Fast | `mednafen_pce`, `mednafen_pce_fast` |
| Neo Geo Pocket / Color | `.ngp` `.ngc` | Beetle NeoPop | `mednafen_ngp` |
| WonderSwan / Color | `.ws` `.wsc` | Beetle WonderSwan | `mednafen_wswan` |
| Atari Lynx | `.lnx` | Handy | `handy` |
| Atari 2600 | `.a26` | Stella 2014 | `stella`, `stella2014` |
| Atari 7800 | `.a78` | ProSystem | `prosystem` |
| Pokemon Mini | `.min` | PokeMini | `pokemini` |

To use another core, download it for your platform (e.g. from the [libretro buildbot](https://buildbot.libretro.com/nightly/)) and put the file, named like `snes9x_libretro.dll` (`.so` on Linux, `.dylib` on macOS), in `retro_handhelds/cores/`. It is used instead of the bundled core the next time you start a game for that system. Cores built on libco (bsnes and its forks, Mupen64Plus-Next, melonDS) and cores that need OpenGL or Vulkan (Flycast, PPSSPP) don't work in the mod. `.bin` files aren't accepted because many systems use that extension; rename a Genesis `.bin` to `.md`.

### Setting up the N64

The N64 core isn't included in the mod: its graphics code is licensed for non-commercial use only, so it can't be shipped with it. It takes a minute to add yourself:

1. Download ParaLLEl-N64 for your computer from the libretro buildbot:
   - Windows: [parallel_n64_libretro.dll.zip](https://buildbot.libretro.com/nightly/windows/x86_64/latest/parallel_n64_libretro.dll.zip)
   - Linux: [parallel_n64_libretro.so.zip](https://buildbot.libretro.com/nightly/linux/x86_64/latest/parallel_n64_libretro.so.zip)
   - macOS, Apple Silicon: [parallel_n64_libretro.dylib.zip](https://buildbot.libretro.com/nightly/apple/osx/arm64/latest/parallel_n64_libretro.dylib.zip)
   - macOS, Intel: [parallel_n64_libretro.dylib.zip](https://buildbot.libretro.com/nightly/apple/osx/x86_64/latest/parallel_n64_libretro.dylib.zip)
2. Unzip it and put `parallel_n64_libretro.dll` (`.so` on Linux, `.dylib` on macOS) in `retro_handhelds/cores/` in your Minecraft folder.
3. Play an N64 cartridge. The mod sets the core up itself (software rendering, no OpenGL needed); there is nothing to configure.

On macOS, if the game says the core could not be loaded, macOS may be blocking the downloaded file: run `xattr -d com.apple.quarantine` on it in Terminal and try again. Until the core is there, starting an N64 game shows a message saying which file to get and where it goes.

A PlayStation game is a `.cue` (with its `.bin` tracks next to it), a `.chd`, an `.m3u` playlist for multi-disc games, or a `.pbp`. No BIOS is needed: PCSX ReARMed has a built-in one, though a real `scph5501.bin` in `system/` is more compatible. The N64 draws in software, which is heavy: it needs a fast CPU to reach full speed while Minecraft runs.

Other players see a shared game at up to 240x160, scaled down from larger systems.

Saves belong to the cartridge, not the console: give a cartridge to someone and it starts their own save on their computer.

## Configuration

**Player settings** (`config/retrohandhelds-client.toml`, also in *Mods → Retro Handhelds → Config*):

| Option | Default | |
|---|---|---|
| `volume` | 10 | 0-10, on top of the Jukebox/Note Blocks slider |
| `otherPlayersVolume` | 10 | 0-10, volume of other players' consoles and handhelds |
| `muted` | false | Mutes your own game |
| `otherPlayersMuted` | false | Mutes other players' games |
| `colorCorrection` | false | Colors like a real GBA screen |
| `lcdGhosting` | false | Frame blending like the original LCD |
| `inWorldView` | false | Open games in the in-world view |
| `fastForwardSpeed` | 2 | 2-4x |
| `gamepadEnabled` | true | |
| `gamepadDeadzone` | 35 | Left stick deadzone, in % |
| `gamepadPositionalAB` | false | Use Nintendo button positions instead of pad labels |
| `shareMyScreen` | true | Let nearby players see your game |
| `showOtherScreens` | true | Show games other players share |
| `resumeGames` | true | Continue games where you left off |

**Server settings** (`config/retrohandhelds-server.toml`):

| Option | Default | |
|---|---|---|
| `enableConsoles` | true | When off, consoles can't start games; cartridges can always be ejected |
| `enableHandhelds` | true | |
| `screenSharing` | true | Let nearby players see games played on consoles |
| `screenShareFps` | 10 | Pictures per second (1-20) |
| `screenShareRadius` | 32 | Blocks around the console |
| `screenShareKBps` | 96 | Bandwidth limit per sharing player; extra is dropped |

## Commands

- `/retrohandhelds play <rom>`: play a ROM directly, without a console (saves per ROM file).
- `/retrohandhelds test <rom>`: runs a ROM for 5 seconds without a screen and writes the last frame to `retro_handhelds/debug_frame.png`. Useful for bug reports.

## Building from source

```bash
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runClient` starts a development client, `./gradlew runServer` a dedicated server, and `./gradlew runClient2` a second player (Player2, own folder) for testing multiplayer. Requires JDK 21.

## Legal

Retro Handhelds does not include, download or link to any games or BIOS files. Only play games you own. This project is not affiliated with or endorsed by Nintendo.

## License and credits

- Retro Handhelds is licensed under the [MIT License](LICENSE).
- Emulation by [mGBA](https://github.com/mgba-emu/mgba) (MPL 2.0, [LICENSE-mGBA.txt](LICENSE-mGBA.txt)), [FCEUmm](https://github.com/libretro/libretro-fceumm) (GPL 2.0, [LICENSE-FCEUmm.txt](LICENSE-FCEUmm.txt)), [Mesen-S](https://github.com/libretro/Mesen-S) (GPL 3.0, [LICENSE-Mesen-S.txt](LICENSE-Mesen-S.txt)), [BlastEm](https://github.com/libretro/blastem) (GPL 3.0, [LICENSE-BlastEm.txt](LICENSE-BlastEm.txt)) and [Gearsystem](https://github.com/drhelius/Gearsystem) (GPL 3.0, [LICENSE-Gearsystem.txt](LICENSE-Gearsystem.txt)), each by their authors and contributors. Exact versions and source links: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). The bundled cores are the libretro builds from the [libretro buildbot](https://buildbot.libretro.com/nightly/).
- Build files come from the [NeoForge MDK](https://github.com/NeoForged/MDK) template (MIT, notice included in [LICENSE](LICENSE)).
