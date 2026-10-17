package dev.anomz.retrohandhelds.client.session;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RomNames;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.block.TvLayout;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.RomLibrary;
import dev.anomz.retrohandhelds.client.emu.CoreOptions;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.SaveFiles;
import dev.anomz.retrohandhelds.client.libretro.CoreKind;
import dev.anomz.retrohandhelds.client.libretro.Lrps2Bios;
import dev.anomz.retrohandhelds.client.libretro.LibretroCore;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.screen.EmulatorScreen;
import dev.anomz.retrohandhelds.item.CartridgeData;
import dev.anomz.retrohandhelds.item.HandheldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The one running emulator on this client (libretro allows only one) and what it belongs to: a
 * console block, a handheld item, or nothing (the play command). Render thread only.
 */
public final class ConsoleSessions {
    /** Full volume within this distance from the console. */
    private static final double FULL_VOLUME_RANGE = 4;
    /** Beyond full volume range, sound fades out linearly to silence at this distance. */
    private static final double SOUND_RANGE = 32;
    /** Walking further away than this switches the console off; also how far a remote reaches. */
    public static final double STOP_RANGE = 48;

    /**
     * @param pos the console block, or null for a handheld or the play command
     * @param handheld whether it runs on a handheld item in the player's inventory
     */
    public record Active(EmulatorSession session, SessionDisplay display, @Nullable BlockPos pos,
            @Nullable CartridgeData cartridge, boolean handheld, String romFile) {
        boolean isRunning() {
            EmulatorSession.State state = session.state();
            return state == EmulatorSession.State.STARTING || state == EmulatorSession.State.RUNNING;
        }
    }

    @Nullable
    private static Active active;

    private ConsoleSessions() {
    }

    /** For the play command: a session not tied to any block. */
    public static Optional<Active> startStandalone(String romFile, Consumer<Component> onError) {
        return start(romFile, null, null, false, onError);
    }

    /** Right-click on a console: reuse its running session or start one, then open the screen. */
    public static void powerOn(BlockPos pos, CartridgeData cartridge) {
        Minecraft minecraft = Minecraft.getInstance();
        Active current = active;
        Optional<Active> session = current != null && pos.equals(current.pos())
                && cartridge.equals(current.cartridge()) && current.isRunning()
                ? Optional.of(current)
                : start(cartridge.romFile(), pos, cartridge, false, message -> minecraft.player.displayClientMessage(message, true));
        session.ifPresent(s -> minecraft.setScreen(new EmulatorScreen(s, false)));
    }

    /**
     * Right-click with a handheld: resume its running game or start one, then open the screen.
     * Closing the screen keeps it running while the handheld stays in the player's inventory.
     */
    public static void powerOnHandheld(CartridgeData cartridge) {
        Minecraft minecraft = Minecraft.getInstance();
        Active current = active;
        Optional<Active> session = current != null && current.handheld()
                && cartridge.equals(current.cartridge()) && current.isRunning()
                ? Optional.of(current)
                : start(cartridge.romFile(), null, cartridge, true, message -> minecraft.player.displayClientMessage(message, true));
        session.ifPresent(s -> minecraft.setScreen(new EmulatorScreen(s, false)));
    }

    private static Optional<Active> start(String romFile, @Nullable BlockPos pos, @Nullable CartridgeData cartridge,
            boolean handheld, Consumer<Component> onError) {
        Optional<Path> rom = RomLibrary.resolve(romFile);
        if (rom.isEmpty()) {
            onError.accept(Component.translatable("message.retrohandhelds.rom_missing", romFile));
            return Optional.empty();
        }
        CoreKind kind = CoreKind.forRom(rom.get());
        if (kind == CoreKind.PCSX && RomLibrary.isDvdImage(rom.get())) {
            onError.accept(Component.translatable("message.retrohandhelds.dvd_game", RomNames.displayName(romFile)));
            return Optional.empty();
        }
        CoreOptions options = optionsFor(kind, RetroClientConfig.colorCorrection(), RetroClientConfig.lcdGhosting());
        if (options == null) {
            onError.accept(Component.translatable("message.retrohandhelds.bios_missing",
                    RetroPaths.system().relativize(Lrps2Bios.folder(RetroPaths.system())).toString()));
            return Optional.empty();
        }
        Optional<LibretroCore> core = NativeLoader.load(kind);
        if (core.isEmpty()) {
            onError.accept(Component.translatable("message.retrohandhelds.core_missing"));
            return Optional.empty();
        }
        // A cartridge saves under its own ID; the play command saves per ROM file.
        String saveId = cartridge != null ? cartridge.saveId() : SaveFiles.idFromName("standalone_", romFile);
        SaveFiles saves = SaveFiles.forId(RetroPaths.saves(), saveId).orElse(null);
        if (saves == null) {
            RetroHandhelds.LOGGER.warn("Retro Handhelds: invalid save ID {}, playing without saves", saveId);
        }
        stop();
        EmulatorSession session;
        try {
            session = EmulatorSession.start(core.get(), kind, rom.get(), RetroPaths.system(), RetroPaths.saves(), saves, true,
                    options);
        } catch (IllegalStateException e) {
            onError.accept(Component.translatable("message.retrohandhelds.busy"));
            return Optional.empty();
        }
        active = new Active(session, new SessionDisplay(session.frames()), pos, cartridge, handheld, romFile);
        return Optional.of(active);
    }

    /**
     * Core options for a game on {@code kind}, or null if it can't start: PS2 games need the
     * player's own BIOS, and run on the all-CPU renderer so the core never needs a graphics context.
     */
    @Nullable
    public static CoreOptions optionsFor(CoreKind kind, boolean colorCorrection, boolean lcdGhosting) {
        CoreOptions options = new CoreOptions(colorCorrection, lcdGhosting);
        if (kind == CoreKind.LRPS2) {
            Optional<String> bios = Lrps2Bios.find(RetroPaths.system());
            if (bios.isEmpty()) {
                return null;
            }
            // Extra rasterizer threads: 4 holds 60 fps in heavy scenes on a fast CPU, 2 (the core's
            // default) drops to ~45. Scaled to the CPU so Minecraft keeps enough for itself.
            int threads = Math.clamp(Runtime.getRuntime().availableProcessors() / 4, 1, 6);
            options.fixed("pcsx2_renderer", "Software (SW)").fixed("pcsx2_bios", bios.get())
                    .fixed("pcsx2_sw_renderer_threads", Integer.toString(threads));
        }
        return options;
    }

    public static void stop() {
        Active current = active;
        if (current != null) {
            active = null;
            current.session().stop();
            current.display().close();
            // Stopped from elsewhere (console broken, handheld gone): don't leave its screen open.
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof EmulatorScreen screen && screen.shows(current)) {
                minecraft.setScreen(null);
            }
        }
    }

    /** Stops {@code session} only if it is still the active one. */
    public static void stop(Active session) {
        if (active == session) {
            stop();
        }
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static Component toggleMute() {
        RetroClientConfig.setMuted(!RetroClientConfig.muted());
        return volumeMessage();
    }

    /** Changes the volume by whole steps of 10%; also unmutes. Saved to the client config. */
    public static Component changeVolume(int steps) {
        RetroClientConfig.setVolume(RetroClientConfig.volume() + steps);
        RetroClientConfig.setMuted(false);
        return volumeMessage();
    }

    /** Changes other players' volume by whole steps of 10%; also unmutes. Saved to the client config. */
    public static Component changeOtherPlayersVolume(int steps) {
        RetroClientConfig.setOtherPlayersVolume(RetroClientConfig.otherPlayersVolume() + steps);
        RetroClientConfig.setMuted(false);
        return Component.translatable("message.retrohandhelds.other_volume",
                RetroClientConfig.otherPlayersVolume() * 100 / RetroClientConfig.VOLUME_STEPS);
    }

    private static Component volumeMessage() {
        return RetroClientConfig.muted()
                ? Component.translatable("message.retrohandhelds.muted")
                : Component.translatable("message.retrohandhelds.volume",
                        RetroClientConfig.volume() * 100 / RetroClientConfig.VOLUME_STEPS);
    }

    /** The display of the handheld playing {@code cartridge}, if it is the one running. */
    @Nullable
    public static SessionDisplay displayForHandheld(@Nullable CartridgeData cartridge) {
        Active current = active;
        return current != null && current.handheld() && cartridge != null && cartridge.equals(current.cartridge())
                ? current.display() : null;
    }

    /** The session running on a console here, or null (no game, or it runs on a handheld). */
    @Nullable
    public static EmulatorSession activeConsoleSession() {
        Active current = active;
        return current == null || current.pos() == null ? null : current.session();
    }

    /** The game running on a handheld here, or null (no game, or it runs on a console). */
    @Nullable
    public static Active activeHandheld() {
        Active current = active;
        return current != null && current.handheld() ? current : null;
    }

    public static boolean activePaused() {
        Active current = active;
        return current != null && current.session().isPaused();
    }

    /** The console block whose game is running on this client, if the running game is on a console. */
    @Nullable
    public static BlockPos activeConsolePos() {
        Active current = active;
        return current == null ? null : current.pos();
    }

    @Nullable
    public static SessionDisplay displayAt(BlockPos pos) {
        Active current = active;
        return current != null && pos.equals(current.pos()) ? current.display() : null;
    }

    /** Client tick: keeps volume in step with distance and switches off consoles that went away. */
    public static void tick(Minecraft minecraft) {
        Active current = active;
        if (current == null) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }
        // Follow Minecraft: singleplayer pauses, while servers and LAN worlds keep running, so the game does too.
        current.session().setPaused(minecraft.isPaused());
        float volume = minecraft.options.getSoundSourceVolume(SoundSource.RECORDS);
        if (current.handheld() && !minecraft.player.getInventory().contains(
                stack -> stack.is(RetroRegistries.HANDHELD) && Objects.equals(HandheldItem.cartridgeData(stack), current.cartridge()))) {
            stop();
            return;
        }
        BlockPos pos = current.pos();
        if (pos != null) {
            // Block broken, chunk unloaded or cartridge swapped by someone: switch off.
            if (!(minecraft.level.getBlockEntity(pos) instanceof ConsoleBlockEntity console)
                    || !Objects.equals(console.cartridgeData(), current.cartridge())) {
                stop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)));
            if (distance > STOP_RANGE) {
                stop();
                return;
            }
            if (!(minecraft.screen instanceof EmulatorScreen)) {
                // Heard from the middle of a TV screen showing the game, if there is one.
                double heard = Math.sqrt(minecraft.player.distanceToSqr(TvLayout.soundCenter(minecraft.level, pos)));
                volume *= (float) Math.clamp(1 - (heard - FULL_VOLUME_RANGE) / (SOUND_RANGE - FULL_VOLUME_RANGE), 0, 1);
            }
        }
        float playerVolume = RetroClientConfig.muted() ? 0 : RetroClientConfig.volume() / (float) RetroClientConfig.VOLUME_STEPS;
        current.session().setVolume(volume * playerVolume);
        current.session().options().set(RetroClientConfig.colorCorrection(), RetroClientConfig.lcdGhosting());
    }
}
