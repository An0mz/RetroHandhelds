package dev.anomz.retrohandhelds.client.screen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.screen.ui.Icon;
import dev.anomz.retrohandhelds.client.screen.ui.PanelScreen;
import dev.anomz.retrohandhelds.client.screen.ui.TextButton;
import dev.anomz.retrohandhelds.client.screen.ui.Ui;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Why a game can't start for want of a core, with what to do about it. For a core the player has to
 * download (the N64): the file, a button to its download for this computer and one to the cores
 * folder. For a bundled core that failed to load: where to look and how to use another core.
 */
public final class CoreMissingPanel extends PanelScreen {
    private static final int WIDTH = 330;
    private static final int TEXT_LEFT = 34;

    private final RetroSystem system;
    private final boolean download;
    private final String fileName;
    @Nullable
    private final String url;
    private List<FormattedCharSequence> lines = List.of();

    public CoreMissingPanel(RetroSystem system) {
        super(Component.translatable(system.bundledCore() == null ? "screen.retrohandhelds.core.download.title"
                : "screen.retrohandhelds.core.failed.title", system.displayName()), null);
        this.system = system;
        this.download = system.bundledCore() == null;
        String core = download ? system.dropInCores().getFirst() : system.bundledCore();
        this.fileName = NativeLoader.libraryFileName(core);
        this.url = download ? NativeLoader.downloadUrl(core) : null;
    }

    private List<FormattedCharSequence> wrap(int width) {
        List<FormattedCharSequence> result = new ArrayList<>();
        if (download) {
            result.addAll(font.split(Component.translatable("screen.retrohandhelds.core.download.why", system.displayName()), width));
            result.add(FormattedCharSequence.EMPTY);
            result.addAll(font.split(Component.translatable("screen.retrohandhelds.core.download.step1", fileName), width));
            result.addAll(font.split(Component.translatable("screen.retrohandhelds.core.download.step2"), width));
            result.addAll(font.split(Component.translatable("screen.retrohandhelds.core.download.step3"), width));
        } else {
            result.addAll(font.split(Component.translatable("screen.retrohandhelds.core.failed.why", system.displayName()), width));
        }
        return result;
    }

    private int textWidth() {
        return WIDTH - 2 * (Ui.PAD + 2) - TEXT_LEFT;
    }

    @Override
    protected int panelWidth() {
        return WIDTH;
    }

    @Override
    protected int panelHeight() {
        int lineCount = wrap(textWidth()).size();
        return Ui.HEADER + Ui.PAD + lineCount * (font.lineHeight + 2) + 12 + 20 + Ui.PAD;
    }

    @Override
    protected void initContent() {
        lines = wrap(contentWidth() - TEXT_LEFT);
        int x = contentLeft();
        int w = contentWidth();
        int y = contentBottom() - 20;
        int right = x + w;
        if (download && url != null) {
            right -= 90;
            addRenderableWidget(new TextButton(right, y, 90, 20, Component.translatable("screen.retrohandhelds.core.download"),
                    TextButton.Style.PRIMARY, () -> ConfirmLinkScreen.confirmLinkNow(this, url)));
            right -= 6;
        }
        addRenderableWidget(new TextButton(right - 120, y, 120, 20, Component.translatable("screen.retrohandhelds.core.open_folder"),
                download ? TextButton.Style.QUIET : TextButton.Style.PRIMARY, () -> Util.getPlatform().openFile(RetroPaths.cores().toFile())));
        addRenderableWidget(new TextButton(x, y, 60, 20, Component.translatable("gui.done"), TextButton.Style.QUIET, this::onClose));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = contentLeft();
        int y = contentTop();
        Ui.icon(g, download ? Icon.FOLDER : Icon.WARNING, x + 2, y, 22, download ? Ui.TEXT_DIM : Ui.WARNING);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + TEXT_LEFT, y + 2 + i * (font.lineHeight + 2), Ui.TEXT_DIM, false);
        }
    }
}
