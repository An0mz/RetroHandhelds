package dev.anomz.retrohandhelds.client.session;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.RomLibrary;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.SaveFiles;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.screen.EmulatorScreen;
import dev.anomz.retrohandhelds.item.CartridgeData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The one running emulator on this client (libretro allows only one) and the console it belongs to,
 * if any. Render thread only.
 */
public final class ConsoleSessions {
    /** Sound fades out linearly to silence at this distance from the console. */
    private static final double SOUND_RANGE = 16;
    /** Walking further away than this switches the console off. */
    private static final double STOP_RANGE = 48;

    public record Active(EmulatorSession session, SessionDisplay display, @Nullable BlockPos pos,
            @Nullable CartridgeData cartridge, String romFile) {
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
        return start(romFile, null, null, onError);
    }

    /** Right-click on a console: reuse its running session or start one, then open the screen. */
    public static void powerOn(BlockPos pos, CartridgeData cartridge) {
        Minecraft minecraft = Minecraft.getInstance();
        Active current = active;
        Optional<Active> session = current != null && pos.equals(current.pos())
                && cartridge.equals(current.cartridge()) && current.isRunning()
                ? Optional.of(current)
                : start(cartridge.romFile(), pos, cartridge, message -> minecraft.player.displayClientMessage(message, true));
        session.ifPresent(s -> minecraft.setScreen(new EmulatorScreen(s, false)));
    }

    private static Optional<Active> start(String romFile, @Nullable BlockPos pos, @Nullable CartridgeData cartridge,
            Consumer<Component> onError) {
        Optional<Path> rom = RomLibrary.resolve(romFile);
        if (rom.isEmpty()) {
            onError.accept(Component.translatable("message.retrohandhelds.rom_missing", romFile));
            return Optional.empty();
        }
        Optional<MgbaCore> core = NativeLoader.load();
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
            session = EmulatorSession.start(core.get(), rom.get(), RetroPaths.system(), RetroPaths.saves(), saves, true);
        } catch (IllegalStateException e) {
            onError.accept(Component.translatable("message.retrohandhelds.busy"));
            return Optional.empty();
        }
        active = new Active(session, new SessionDisplay(session.frames()), pos, cartridge, romFile);
        return Optional.of(active);
    }

    public static void stop() {
        Active current = active;
        if (current != null) {
            active = null;
            current.session().stop();
            current.display().close();
        }
    }

    /** Stops {@code session} only if it is still the active one. */
    public static void stop(Active session) {
        if (active == session) {
            stop();
        }
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
        float volume = minecraft.options.getSoundSourceVolume(SoundSource.RECORDS);
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
                volume *= (float) Math.max(0, 1 - distance / SOUND_RANGE);
            }
        }
        current.session().setVolume(volume);
    }
}
