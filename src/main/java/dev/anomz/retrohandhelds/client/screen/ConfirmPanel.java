package dev.anomz.retrohandhelds.client.screen;

import java.util.List;

import dev.anomz.retrohandhelds.client.screen.ui.Icon;
import dev.anomz.retrohandhelds.client.screen.ui.PanelScreen;
import dev.anomz.retrohandhelds.client.screen.ui.TextButton;
import dev.anomz.retrohandhelds.client.screen.ui.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Asks before something that can't be undone: a warning, the message, Cancel and a red confirm button. */
public final class ConfirmPanel extends PanelScreen {
    private static final int WIDTH = 300;
    private static final int TEXT_LEFT = 34;

    private final Component message;
    private final Component confirm;
    private final Runnable onConfirm;
    private List<FormattedCharSequence> lines = List.of();

    ConfirmPanel(Screen parent, Component title, Component message, Component confirm, Runnable onConfirm) {
        super(title, parent);
        this.message = message;
        this.confirm = confirm;
        this.onConfirm = onConfirm;
    }

    @Override
    protected int panelWidth() {
        return WIDTH;
    }

    @Override
    protected int panelHeight() {
        int textWidth = WIDTH - 2 * (Ui.PAD + 2) - TEXT_LEFT;
        int lineCount = Math.max(2, minecraft.font.split(message, textWidth).size());
        return Ui.HEADER + Ui.PAD + lineCount * (minecraft.font.lineHeight + 2) + 16 + 20 + Ui.PAD;
    }

    @Override
    protected void initContent() {
        lines = font.split(message, contentWidth() - TEXT_LEFT);
        int x = contentLeft();
        int w = contentWidth();
        int y = contentBottom() - 20;
        addRenderableWidget(new TextButton(x + w - 90, y, 90, 20, confirm, TextButton.Style.DANGER, () -> {
            onClose();
            onConfirm.run();
        }));
        TextButton cancel = addRenderableWidget(new TextButton(x + w - 90 - 6 - 80, y, 80, 20, CommonComponents.GUI_CANCEL,
                TextButton.Style.QUIET, this::onClose));
        setInitialFocus(cancel);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = contentLeft();
        int y = contentTop();
        Ui.icon(g, Icon.WARNING, x + 2, y, 22, Ui.WARNING);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + TEXT_LEFT, y + 2 + i * (font.lineHeight + 2), Ui.TEXT_DIM, false);
        }
    }
}
