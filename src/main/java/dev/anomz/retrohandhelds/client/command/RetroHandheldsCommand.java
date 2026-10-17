package dev.anomz.retrohandhelds.client.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.screen.EmulatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Client commands:
 * {@code /retrohandhelds play <rom>} opens the emulator screen;
 * {@code /retrohandhelds test <rom>} runs headless and dumps the last frame to PNG.
 */
public final class RetroHandheldsCommand {
    private RetroHandheldsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(RetroHandhelds.MODID)
                .then(Commands.literal("play")
                        .then(Commands.argument("rom", StringArgumentType.greedyString())
                                .suggests(RetroHandheldsCommand::suggestRoms)
                                .executes(RetroHandheldsCommand::play)))
                .then(Commands.literal("test")
                        .then(Commands.argument("rom", StringArgumentType.greedyString())
                                .suggests(RetroHandheldsCommand::suggestRoms)
                                .executes(RetroHandheldsCommand::test))));
    }

    private static int play(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "rom");
        Optional<EmulatorSession> session = startSession(ctx.getSource(), name, true);
        session.ifPresent(s -> Minecraft.getInstance().setScreen(new EmulatorScreen(s, name)));
        return session.isPresent() ? 1 : 0;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "rom");
        Optional<EmulatorSession> session = startSession(ctx.getSource(), name, false);
        session.ifPresent(s -> {
            ctx.getSource().sendSuccess(() -> Component.literal("Running " + name + " for " + DebugFrameTest.RUN_SECONDS + " seconds..."), false);
            DebugFrameTest.scheduleFinish(s);
        });
        return session.isPresent() ? 1 : 0;
    }

    private static Optional<EmulatorSession> startSession(CommandSourceStack source, String name, boolean withSound) {
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
            return Optional.empty();
        }

        Optional<MgbaCore> core = NativeLoader.load();
        if (core.isEmpty()) {
            source.sendFailure(Component.literal("The mGBA core could not be loaded. See the game log for details."));
            return Optional.empty();
        }

        try {
            return Optional.of(EmulatorSession.start(core.get(), rom, RetroPaths.system(), RetroPaths.saves(), withSound));
        } catch (IllegalStateException e) {
            source.sendFailure(Component.literal("An emulator session is already running."));
            return Optional.empty();
        }
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
}
