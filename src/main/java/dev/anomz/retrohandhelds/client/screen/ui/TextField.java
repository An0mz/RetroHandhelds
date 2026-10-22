package dev.anomz.retrohandhelds.client.screen.ui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** A text field in a rounded box, with an optional icon; the box lights up with the accent while typing. */
public class TextField extends EditBox {
    private static final int INSET_X = 7;
    private static final int ICON = 10;

    @Nullable
    private final Icon icon;
    private final int insetLeft;

    /** {@code x, y, width, height}: the whole box, 20 high is usual. */
    public TextField(int x, int y, int width, int height, Component name, @Nullable Icon icon) {
        super(Minecraft.getInstance().font, x + inset(icon), y + (height - 8) / 2, width - inset(icon) - INSET_X, 8, name);
        this.icon = icon;
        this.insetLeft = inset(icon);
        setBordered(false);
        setTextColor(Ui.TEXT);
        setHint(name.copy().withColor(Ui.TEXT_FAINT));
    }

    private static int inset(@Nullable Icon icon) {
        return icon == null ? INSET_X : INSET_X + ICON + 4;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int boxX = getX() - insetLeft;
        int boxY = getY() - 6;
        Ui.box(g, boxX, boxY, width + insetLeft + INSET_X, height + 12, Ui.SURFACE, isFocused() ? Ui.ACCENT : Ui.PANEL_BORDER);
        if (icon != null) {
            Ui.icon(g, icon, boxX + INSET_X, getY() - 1, ICON, Ui.TEXT_DIM);
        }
        super.renderWidget(g, mouseX, mouseY, partialTick);
    }

    /** The whole box takes clicks, not just the line of text. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= getX() - insetLeft && mouseX < getX() + width + INSET_X
                && mouseY >= getY() - 6 && mouseY < getY() + height + 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && !super.isMouseOver(mouseX, mouseY)) {
            // Clicked the box around the text: focus, with the cursor where it is.
            setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
