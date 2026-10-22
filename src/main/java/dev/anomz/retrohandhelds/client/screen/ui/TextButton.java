package dev.anomz.retrohandhelds.client.screen.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A rounded text button: accent-filled for the main action, red for a destructive one, quiet otherwise. */
public class TextButton extends AbstractButton {
    public enum Style { PRIMARY, QUIET, DANGER }

    private final Style style;
    private final Runnable action;

    public TextButton(int x, int y, int width, int height, Component label, Style style, Runnable action) {
        super(x, y, width, height, label);
        this.style = style;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && isHoveredOrFocused();
        int fill;
        int text;
        if (!active) {
            fill = Ui.SURFACE;
            text = Ui.TEXT_FAINT;
        } else if (style == Style.PRIMARY) {
            fill = hot ? Ui.ACCENT_HOVER : Ui.ACCENT;
            text = 0xFFFFFFFF;
        } else if (style == Style.DANGER) {
            fill = hot ? 0xFFF47A7C : Ui.DANGER;
            text = 0xFFFFFFFF;
        } else {
            fill = hot ? Ui.SURFACE_HOVER : Ui.SURFACE;
            text = Ui.TEXT;
        }
        Ui.round(g, getX(), getY(), width, height, fill);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                getY() + (height - font.lineHeight) / 2 + 1, text, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
