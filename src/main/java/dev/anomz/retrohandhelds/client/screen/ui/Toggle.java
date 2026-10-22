package dev.anomz.retrohandhelds.client.screen.ui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** A label with an on/off switch on the right; the whole row toggles it. */
public class Toggle extends AbstractButton {
    private static final int TRACK_W = 22;
    private static final int TRACK_H = 12;

    private boolean on;
    private final BooleanConsumer onChange;

    public Toggle(int x, int y, int width, int height, Component label, boolean on, BooleanConsumer onChange) {
        super(x, y, width, height, label);
        this.on = on;
        this.onChange = onChange;
    }

    @Override
    public void onPress() {
        on = !on;
        onChange.accept(on);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && isHoveredOrFocused();
        if (hot) {
            Ui.round(g, getX(), getY(), width, height, Ui.SURFACE);
        }
        Font font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), getX() + 4, getY() + (height - font.lineHeight) / 2 + 1, active ? Ui.TEXT : Ui.TEXT_FAINT, false);
        int trackX = getX() + width - TRACK_W - 4;
        int trackY = getY() + (height - TRACK_H) / 2;
        Ui.round(g, trackX, trackY, TRACK_W, TRACK_H, on ? (hot ? Ui.ACCENT_HOVER : Ui.ACCENT) : Ui.TRACK);
        int knobX = on ? trackX + TRACK_W - TRACK_H + 2 : trackX + 2;
        Ui.round(g, knobX, trackY + 2, TRACK_H - 4, TRACK_H - 4, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, CommonComponents.optionStatus(getMessage(), on));
    }
}
