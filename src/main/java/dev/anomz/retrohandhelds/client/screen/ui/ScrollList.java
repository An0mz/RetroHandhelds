package dev.anomz.retrohandhelds.client.screen.ui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A scrolling list of rows drawn by the screen: rows are clipped to the list, the mouse wheel or the
 * slim scrollbar scrolls it, and a click on a row reports that row.
 */
public class ScrollList<T> extends AbstractWidget {
    /** Draws one row at {@code x, y}, {@code width x height}. */
    @FunctionalInterface
    public interface RowRenderer<T> {
        void render(GuiGraphics g, T item, int x, int y, int width, int height, boolean hovered);
    }

    /** A click on a row, with the GLFW mouse button. */
    @FunctionalInterface
    public interface RowClick<T> {
        void clicked(T item, int button);
    }

    private static final int BAR = 3;

    private final int rowHeight;
    private final RowRenderer<T> renderer;
    private final RowClick<T> onClick;
    private List<T> items = List.of();
    private double scroll;
    private boolean draggingBar;

    public ScrollList(int x, int y, int width, int height, int rowHeight, Component name, RowRenderer<T> renderer, RowClick<T> onClick) {
        super(x, y, width, height, name);
        this.rowHeight = rowHeight;
        this.renderer = renderer;
        this.onClick = onClick;
    }

    public void setItems(List<T> items) {
        this.items = List.copyOf(items);
        scroll = clamp(scroll);
    }

    public List<T> items() {
        return items;
    }

    private int contentHeight() {
        return items.size() * rowHeight;
    }

    private boolean scrolls() {
        return contentHeight() > height;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(Math.max(0, contentHeight() - height), value));
    }

    private int rowWidth() {
        return scrolls() ? width - BAR - 4 : width;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + rowWidth() || mouseY < getY() || mouseY >= getY() + height) {
            return -1;
        }
        int index = (int) ((mouseY - getY() + scroll) / rowHeight);
        return index < items.size() ? index : -1;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int hovered = isHovered() ? rowAt(mouseX, mouseY) : -1;
        g.enableScissor(getX(), getY(), getX() + width, getY() + height);
        int first = (int) (scroll / rowHeight);
        for (int i = first; i < items.size(); i++) {
            int y = getY() + i * rowHeight - (int) scroll;
            if (y >= getY() + height) {
                break;
            }
            renderer.render(g, items.get(i), getX(), y, rowWidth(), rowHeight, i == hovered);
        }
        g.disableScissor();
        if (scrolls()) {
            int barX = getX() + width - BAR;
            Ui.round(g, barX, getY(), BAR, height, Ui.SURFACE);
            int thumb = Math.max(16, height * height / contentHeight());
            int thumbY = getY() + (int) ((height - thumb) * scroll / (contentHeight() - height));
            Ui.round(g, barX, thumbY, BAR, thumb, draggingBar ? Ui.TEXT_DIM : Ui.TRACK);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (scrolls() && mouseX >= getX() + width - BAR - 2) {
            draggingBar = true;
            scrollToBar(mouseY);
            return true;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onClick.clicked(items.get(row), button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            scrollToBar(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return false;
    }

    private void scrollToBar(double mouseY) {
        double fraction = (mouseY - getY()) / height;
        scroll = clamp(fraction * contentHeight() - height / 2.0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!scrolls()) {
            return false;
        }
        scroll = clamp(scroll - scrollY * rowHeight * 1.5);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
