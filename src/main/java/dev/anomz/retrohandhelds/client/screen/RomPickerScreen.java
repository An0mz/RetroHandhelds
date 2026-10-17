package dev.anomz.retrohandhelds.client.screen;

import java.util.List;

import dev.anomz.retrohandhelds.client.RomLibrary;
import dev.anomz.retrohandhelds.network.AssignRomPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/** Lists the ROMs in the local ROM folder; picking one writes its file name onto the held cartridge. */
public final class RomPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 300;

    private final InteractionHand hand;
    private List<String> roms = List.of();
    private int page;

    public RomPickerScreen(InteractionHand hand) {
        super(Component.translatable("screen.retrohandhelds.rom_picker"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        roms = RomLibrary.list();
        int perPage = Math.max(1, (height - 100) / ROW_HEIGHT);
        int pages = Math.max(1, (roms.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int left = (width - BUTTON_WIDTH) / 2;
        int top = 40;

        int first = page * perPage;
        for (int i = first; i < Math.min(roms.size(), first + perPage); i++) {
            String rom = roms.get(i);
            addRenderableWidget(Button.builder(Component.literal(rom), b -> choose(rom))
                    .bounds(left, top + (i - first) * ROW_HEIGHT, BUTTON_WIDTH, 20).build());
        }

        int bottom = height - 30;
        if (pages > 1) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                    .bounds(left, bottom, 40, 20).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                    .bounds(left + BUTTON_WIDTH - 40, bottom, 40, 20).build());
            next.active = page < pages - 1;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(width / 2 - 50, bottom, 100, 20).build());
    }

    private void turnPage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private void choose(String rom) {
        PacketDistributor.sendToServer(new AssignRomPayload(hand == InteractionHand.OFF_HAND, rom));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        if (roms.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.retrohandhelds.rom_picker.empty"),
                    width / 2, height / 2 - 10, 0xFFFFFFFF);
            graphics.drawCenteredString(font, RomLibrary.folder().toString(), width / 2, height / 2 + 4, 0xFFAAAAAA);
        }
    }
}
