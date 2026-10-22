# Changelog

All notable changes to Retro Handhelds. Versions follow [semantic versioning](https://semver.org):
new features raise the middle number, fixes alone the last one.

## [1.2.0] - 2026-10-22

### Added
- **More systems:** NES, SNES, Genesis / Mega Drive, Master System and Game Gear games, picked by
  file extension (`.nes`, `.sfc`, `.smc`, `.md`, `.gen`, `.smd`, `.sms`, `.gg`).
- **N64, PlayStation, Nintendo DS and more:** N64 (ParaLLEl-N64, software rendered; downloaded by the
  player into `retro_handhelds/cores/`, since its license forbids bundling it), PlayStation
  (PCSX ReARMed, no BIOS needed; `.cue`, `.chd`, `.m3u`, `.pbp`), Nintendo DS (DeSmuME, touchscreen with
  the mouse), PC Engine, Neo Geo Pocket, WonderSwan, Atari Lynx, 2600 and 7800, Pokemon Mini, 32X and
  SG-1000.
- **Controls per console:** every console has its own keys, preset for its controller and named
  after its own buttons. A new **Controls** button in the game screen shows and changes them (click,
  then press a key or mouse button); changes are kept in `retro_handhelds/controls.json`.
- **Mouse look:** on the PlayStation the mouse moves the right stick; Left Alt frees the cursor. On the
  N64 it is off (the arrow keys are the C-buttons) and can be turned on in Controls, where a flick taps a
  C-button. Mouse look, up/down and sensitivity can be changed per console.
- **Analog sticks:** gamepad sticks reach the game as analog sticks on the N64 and PlayStation, and the
  gamepad triggers are L2/R2 there.
- **Restart button** next to Power Off, with a confirmation: starts the game over without switching
  the console off.
- **Your own cores:** put any libretro core in `retro_handhelds/cores/` (e.g. Genesis Plus GX or
  Snes9x) to use it instead of the bundled one.

### Changed
- The console buttons left *Options → Controls*: they are set per console in the new Controls screen.
  Keys changed there before are not carried over (the GBA preset uses the same defaults as before).
- Games show at up to 640x480, so NES, SNES and Genesis games keep their full picture, at the shape the
  core asks for (Genesis at 4:3). Bigger pictures are scaled down instead of cropped.
- N64 and PlayStation games are drawn smoothed instead of with blocky pixels, as on a TV of the time.
- Save states from cores other than mGBA are kept in their own files, so switching cores can't load
  an incompatible state. Existing GBA saves and states are unchanged.

### Fixed
- Big TV walls no longer slow the game down or run it out of memory: one check now works out the
  screen for every TV in the wall, instead of each TV walking the whole wall twice a second.

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

[1.2.0]: https://github.com/An0mz/RetroHandhelds/compare/f61872c...f711bb9
[1.1.0]: https://github.com/An0mz/RetroHandhelds/compare/3cc638f...f61872c
[1.0.0]: https://github.com/An0mz/RetroHandhelds/tree/3cc638f
