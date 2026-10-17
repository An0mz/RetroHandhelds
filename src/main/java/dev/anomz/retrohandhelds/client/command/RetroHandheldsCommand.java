package dev.anomz.retrohandhelds.client.command;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.RomLibrary;
import dev.anomz.retrohandhelds.client.emu.CoreOptions;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.libretro.CoreKind;
import dev.anomz.retrohandhelds.client.libretro.LibretroCore;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.screen.EmulatorScreen;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Client commands:
 * {@code /retrohandhelds play <rom>} opens the emulator screen without a console block;
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
        Optional<ConsoleSessions.Active> session = ConsoleSessions.startStandalone(name, ctx.getSource()::sendFailure);
        session.ifPresent(s -> Minecraft.getInstance().setScreen(new EmulatorScreen(s, true)));
        return session.isPresent() ? 1 : 0;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "rom");
        Optional<Path> rom = RomLibrary.resolve(name);
        if (rom.isEmpty()) {
            source.sendFailure(Component.translatable("message.retrohandhelds.rom_missing", name));
            return 0;
        }
        CoreKind kind = CoreKind.forRom(rom.get());
        CoreOptions options = ConsoleSessions.optionsFor(kind, false, false);
        if (options == null) {
            source.sendFailure(Component.literal("No PS2 BIOS found in retro_handhelds/system/pcsx2/bios"));
            return 0;
        }
        Optional<LibretroCore> core = NativeLoader.load(kind);
        if (core.isEmpty()) {
            source.sendFailure(Component.translatable("message.retrohandhelds.core_missing"));
            return 0;
        }
        EmulatorSession session;
        try {
            session = EmulatorSession.start(core.get(), kind, rom.get(), RetroPaths.system(), RetroPaths.saves(), null, false,
                    options);
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("message.retrohandhelds.busy"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Running " + name + " for " + DebugFrameTest.RUN_SECONDS + " seconds..."), false);
        DebugFrameTest.scheduleFinish(session);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestRoms(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(RomLibrary.list(), builder);
    }
}
