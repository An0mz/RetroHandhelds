package dev.anomz.retrohandhelds.client.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** {@code /retrohandhelds test <rom>}: runs a ROM headless for a few seconds and dumps the last frame to PNG. */
public final class TestCommand {
    private static final long RUN_SECONDS = 5;

    private TestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RetroHandhelds.MODID)
                .then(Commands.literal("test")
                        .then(Commands.argument("rom", StringArgumentType.greedyString())
                                .suggests(TestCommand::suggestRoms)
                                .executes(TestCommand::run))));
    }

    private static CompletableFuture<Suggestions> suggestRoms(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(listRoms(), builder);
    }

    private static List<String> listRoms() {
        try (Stream<Path> files = Files.list(RetroPaths.roms())) {
            return files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).matches(".*\\.(gba|gbc|gb)$"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "rom");
        Path roms = RetroPaths.roms().toAbsolutePath().normalize();
        Path rom;
        try {
            rom = roms.resolve(name).normalize();
        } catch (InvalidPathException e) {
            rom = null;
        }
        // The startsWith check stops names like "../../something" from leaving the ROM folder.
        if (rom == null || !rom.startsWith(roms) || !Files.isRegularFile(rom)) {
            source.sendFailure(Component.literal("No ROM named \"" + name + "\" in " + roms));
            return 0;
        }

        Optional<MgbaCore> core = NativeLoader.load();
        if (core.isEmpty()) {
            source.sendFailure(Component.literal("The mGBA core could not be loaded. See the game log for details."));
            return 0;
        }

        EmulatorSession session;
        try {
            session = EmulatorSession.start(core.get(), rom, RetroPaths.system(), RetroPaths.saves());
        } catch (IllegalStateException e) {
            source.sendFailure(Component.literal("An emulator session is already running."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Running " + name + " for " + RUN_SECONDS + " seconds..."), false);
        CompletableFuture.delayedExecutor(RUN_SECONDS, TimeUnit.SECONDS).execute(() -> finish(session));
        return 1;
    }

    /** Runs off the render thread so stopping the session never blocks a frame. */
    private static void finish(EmulatorSession session) {
        session.stop();
        int[] pixels = new int[FrameBuffer.PIXELS];
        long frameCount = session.frames().copyLatest(pixels);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> writeFrame(minecraft, session, pixels, frameCount));
    }

    private static void writeFrame(Minecraft minecraft, EmulatorSession session, int[] pixels, long frameCount) {
        if (frameCount == 0) {
            String reason = session.failure() != null ? session.failure() : "the core produced no video";
            chat(minecraft, "Test failed: " + reason);
            return;
        }
        Path out = RetroPaths.debugFrame();
        try (NativeImage image = new NativeImage(FrameBuffer.WIDTH, FrameBuffer.HEIGHT, false)) {
            for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
                for (int x = 0; x < FrameBuffer.WIDTH; x++) {
                    image.setPixelRGBA(x, y, pixels[y * FrameBuffer.WIDTH + x]);
                }
            }
            image.writeToFile(out);
            chat(minecraft, "Ran " + frameCount + " frames. Last frame written to " + out);
        } catch (IOException e) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not write {}", out, e);
            chat(minecraft, "Could not write " + out + ": " + e.getMessage());
        }
    }

    private static void chat(Minecraft minecraft, String message) {
        minecraft.gui.getChat().addMessage(Component.literal(message));
    }
}
