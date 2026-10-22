package dev.anomz.retrohandhelds.client.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.anomz.retrohandhelds.RomNames;
import dev.anomz.retrohandhelds.client.GameNames;
import dev.anomz.retrohandhelds.client.RomLibrary;
import dev.anomz.retrohandhelds.client.screen.ui.Icon;
import dev.anomz.retrohandhelds.client.screen.ui.PanelScreen;
import dev.anomz.retrohandhelds.client.screen.ui.ScrollList;
import dev.anomz.retrohandhelds.client.screen.ui.TextButton;
import dev.anomz.retrohandhelds.client.screen.ui.TextField;
import dev.anomz.retrohandhelds.client.screen.ui.Ui;
import dev.anomz.retrohandhelds.network.AssignRomPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The games in the local ROM folder, searchable; picking one writes its file name onto the held
 * cartridge. With no games yet, it says where they go and opens that folder.
 */
public final class RomPickerScreen extends PanelScreen {
    private static final int ROW_HEIGHT = 20;

    private final InteractionHand hand;
    private List<String> roms = List.of();
    private String query = "";
    private ScrollList<String> list;

    public RomPickerScreen(InteractionHand hand) {
        super(Component.translatable("screen.retrohandhelds.rom_picker"), null);
        this.hand = hand;
    }

    @Override
    protected int panelWidth() {
        return 340;
    }

    @Override
    protected int panelHeight() {
        return 300;
    }

    @Override
    protected void initContent() {
        roms = RomLibrary.list();
        int x = contentLeft();
        int w = contentWidth();
        int y = contentTop();
        int bottom = contentBottom();
        if (roms.isEmpty()) {
            subtitle = Component.empty();
            addRenderableWidget(new TextButton(x + (w - 120) / 2, panelY + panelH / 2 + 22, 120, 20,
                    Component.translatable("screen.retrohandhelds.rom_picker.open_folder"), TextButton.Style.PRIMARY,
                    () -> Util.getPlatform().openFile(RomLibrary.folder().toFile())));
            return;
        }
        TextField search = addRenderableWidget(new TextField(x, y, w, 20,
                Component.translatable("screen.retrohandhelds.rom_picker.search"), Icon.SEARCH));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            refresh();
        });
        setInitialFocus(search);
        list = addRenderableWidget(new ScrollList<>(x, y + 26, w, bottom - 26 - (y + 26), ROW_HEIGHT,
                title, this::renderRow, (rom, button) -> choose(rom)));
        addRenderableWidget(new TextButton(x + w - 80, bottom - 20, 80, 20,
                Component.translatable("gui.cancel"), TextButton.Style.QUIET, this::onClose));
        refresh();
    }

    private void refresh() {
        List<String> shown = GameNames.filter(roms, query);
        list.setItems(shown);
        subtitle = Component.translatable(shown.size() == roms.size() ? "screen.retrohandhelds.rom_picker.count"
                : "screen.retrohandhelds.rom_picker.matching", shown.size(), roms.size());
    }

    private void renderRow(GuiGraphics g, String rom, int x, int y, int w, int h, boolean hovered) {
        if (hovered) {
            Ui.round(g, x, y + 1, w, h - 2, Ui.SURFACE_HOVER);
        }
        String tag = GameNames.tag(rom);
        int tagW = 30;
        Ui.round(g, x + 4, y + 4, tagW, h - 8, hovered ? Ui.ACCENT : Ui.SURFACE);
        g.drawCenteredString(font, tag, x + 4 + tagW / 2, y + (h - font.lineHeight) / 2 + 1, hovered ? 0xFFFFFFFF : Ui.TEXT_DIM);
        int textX = x + 4 + tagW + 8;
        String name = font.plainSubstrByWidth(RomNames.displayName(rom), w - (textX - x) - 6);
        g.drawString(font, name, textX, y + (h - font.lineHeight) / 2 + 1, Ui.TEXT, false);
    }

    private void choose(String rom) {
        PacketDistributor.sendToServer(new AssignRomPayload(hand == InteractionHand.OFF_HAND, rom));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter picks the game when the search leaves only one.
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && list != null && list.items().size() == 1) {
            choose(list.items().getFirst());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (roms.isEmpty()) {
            int cx = panelX + panelW / 2;
            int top = panelY + panelH / 2 - 44;
            Ui.icon(g, Icon.FOLDER, cx - 12, top, 24, Ui.TEXT_FAINT);
            g.drawCenteredString(font, Component.translatable("screen.retrohandhelds.rom_picker.empty"), cx, top + 32, Ui.TEXT);
            String folder = font.plainSubstrByWidth(RomLibrary.folder().toString(), contentWidth());
            g.drawCenteredString(font, folder, cx, top + 46, Ui.TEXT_DIM);
        } else if (list.items().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.retrohandhelds.rom_picker.no_match"),
                    panelX + panelW / 2, list.getY() + 20, Ui.TEXT_DIM);
        }
    }
}
