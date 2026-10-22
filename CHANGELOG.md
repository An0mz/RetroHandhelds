# Changelog

All notable changes to Retro Handhelds. Versions follow [semantic versioning](https://semver.org):
new features raise the middle number, fixes alone the last one.

## [1.2.0] - 2026-10-22

### Added
- **Many more systems:** NES, SNES, N64, PlayStation, Nintendo DS, Genesis / Mega Drive, 32X, Master
  System, Game Gear, SG-1000, PC Engine, Neo Geo Pocket, WonderSwan, Atari Lynx, 2600 and 7800, and
  Pokemon Mini, picked by file extension. PlayStation games are `.cue`, `.chd`, `.m3u` or `.pbp` and need
  no BIOS. The N64 needs a core you download yourself; the README explains how.
- **Controls per console:** every console has its own keys, preset for its controller and named
  after its own buttons. A new **Controls** button in the game screen shows and changes them.
- **Mouse:** on the PlayStation the mouse moves the right stick (Left Alt frees the cursor); on the DS it
  is the touchscreen. Mouse look and its sensitivity can be set per console in Controls.
- **Analog sticks:** gamepad sticks reach the game as analog sticks on the N64 and PlayStation, and the
  triggers are L2/R2 there.
- **Restart button** next to Power Off, with a confirmation.
- **Your own cores:** put any libretro core in `retro_handhelds/cores/` (e.g. Genesis Plus GX or
  Snes9x) to use it instead of the bundled one.

### Changed
- The console buttons moved from *Options → Controls* to the new Controls screen. Keys changed there
  before are not carried over; the GBA keys are the same as before.
- Bigger pictures (up to 640x480) at the shape the core asks for, scaled down instead of cropped. N64
  and PlayStation games are drawn smoothed instead of with blocky pixels.
- Save states from cores other than mGBA are kept in their own files. Existing GBA saves and states
  are unchanged.

### Fixed
- Big TV walls no longer slow the game down or run it out of memory.

## [1.1.0] - 2026-10-18

### Added
- **Continue where you left off:** when a game stops (you close Minecraft, leave the world, walk away,
  put the handheld down or power off) its exact state is saved, and the next time you play that
  cartridge it picks up right there. It's also refreshed every minute while you play, so a crash loses
  little. Separate from your own save-state slot; turn it off with `resumeGames` in the client config.
- **Separate volume for other players' games:** N mutes them and [ / ] lower or raise their volume,
  without touching your own game. New client config option `otherPlayersMuted`.

### Changed
- M, - and = now only affect your own game (M used to mute other players' games too).

### Fixed
- The Retro Remote is held like a remote, pointing forward, instead of diagonally like a sword.

## [1.0.0] - 2026-10-17

First public release, for Minecraft 1.21.1 with NeoForge.

### Added
- **Retro Console** block: insert a cartridge and the game plays on the block's screen, with sound that
  fades with distance.
- **Retro Handheld** item: carry a game and see it in your hands, map-style, while you walk around.
- **Retro TV** block: TVs touching a console show its game; TVs facing the same way in a filled
  rectangle (up to 16x16) join into one seamless screen. Right-click the screen to play.
- **Retro Remote** item: link it to a console or its TV and play from up to 48 blocks away.
- **Cartridges** remember their game and keep their own saves across consoles and handhelds.
- GBA, Game Boy and Game Boy Color games, using your own ROM files. No games or BIOS are included.
- Full-screen play or the in-world view (V), which hides the HUD so nothing covers the game.
- Automatic in-game saves and a save-state slot per cartridge (F5 / F9).
- Keyboard and gamepad support (Xbox, PlayStation and other controllers), all rebindable.
- Fast-forward up to 4x (hold Tab), optional GBA color correction and LCD ghosting.
- Volume and mute keys, remembered between sessions.
- **Multiplayer:** players nearby see and hear the game being played, on consoles, TVs and handhelds
  held by other players. Games run on each player's own computer; the server only relays a
  compressed picture and sound within limits the server owner sets.
- Server settings to turn consoles, handhelds or screen sharing off, and to limit sharing bandwidth.

[1.2.0]: https://github.com/An0mz/RetroHandhelds/compare/f61872c...v1.2.0
[1.1.0]: https://github.com/An0mz/RetroHandhelds/compare/3cc638f...f61872c
[1.0.0]: https://github.com/An0mz/RetroHandhelds/tree/3cc638f
