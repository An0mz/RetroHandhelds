package dev.anomz.retrohandhelds.client.screen;

import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.anomz.retrohandhelds.client.controls.Binding;
import dev.anomz.retrohandhelds.client.controls.ControlAction;
import dev.anomz.retrohandhelds.client.controls.ControlPreset;
import dev.anomz.retrohandhelds.client.controls.ControlProfile;
import dev.anomz.retrohandhelds.client.controls.ControlProfiles;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * One console's keys: click a control, then press a key or mouse button (Esc unbinds). A key shown
 * in red does something else too: another control of this console, or an emulator key such as save
 * state, which wins. Saved to {@code controls.json} when the screen closes.
 */
public final class ControlsScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int KEY_BUTTON_WIDTH = 110;
    /** Emulator keys checked before any console control in the emulator screen. */
    private static final List<KeyMapping> EMULATOR_KEYS = List.of(EmulatorKeys.SAVE_STATE, EmulatorKeys.LOAD_STATE,
            EmulatorKeys.FAST_FORWARD, EmulatorKeys.TOGGLE_VIEW, EmulatorKeys.FREE_MOUSE, EmulatorKeys.MUTE,
            EmulatorKeys.VOLUME_DOWN, EmulatorKeys.VOLUME_UP, EmulatorKeys.MUTE_OTHERS, EmulatorKeys.OTHERS_VOLUME_DOWN,
            EmulatorKeys.OTHERS_VOLUME_UP);

    private final Screen parent;
    private final ControlProfile profile;
    /** The control waiting for its new key, or null. */
    private ControlAction waitingFor;

    ControlsScreen(Screen parent, ControlProfile profile) {
        super(Component.translatable("screen.retrohandhelds.controls.title", profile.system().displayName()));
        this.parent = parent;
        this.profile = profile;
    }

    /** How a binding reads on screen: the key's own name, a mouse button, or "-" when unbound. */
    public static Component keyName(Binding binding) {
        if (!binding.isBound()) {
            return Component.literal("-");
        }
        InputConstants.Type type = binding.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        return type.getOrCreate(binding.code()).getDisplayName();
    }

    @Override
    protected void init() {
        boolean analog = profile.system().controls().analog;
        int footerTop = height - (analog ? 78 : 30);
        ControlList list = new ControlList(minecraft, width, footerTop - 36, 30);
        for (ControlPreset.Entry entry : profile.preset().entries()) {
            list.add(new Row(entry));
        }
        addRenderableWidget(list);

        int left = width / 2 - 155;
        int right = width / 2 + 5;
        if (analog) {
            addRenderableWidget(CycleButton.onOffBuilder(profile.mouseLook())
                    .create(left, footerTop, 150, 20, Component.translatable("screen.retrohandhelds.controls.mouse_look"),
                            (button, on) -> profile.setMouseLook(on)));
            addRenderableWidget(CycleButton.onOffBuilder(profile.mouseVertical())
                    .create(right, footerTop, 150, 20, Component.translatable("screen.retrohandhelds.controls.mouse_vertical"),
                            (button, on) -> profile.setMouseVertical(on)));
            addRenderableWidget(new SensitivitySlider(left, footerTop + 24));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.retrohandhelds.controls.reset"), b -> {
            profile.reset();
            waitingFor = null;
            rebuildWidgets();
        }).bounds(left, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(right, height - 26, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.retrohandhelds.controls.help"), width / 2, 18, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingFor != null) {
            bind(keyCode == GLFW.GLFW_KEY_ESCAPE ? Binding.NONE : Binding.key(keyCode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingFor != null) {
            bind(Binding.mouseButton(button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void bind(Binding binding) {
        profile.set(waitingFor, binding);
        waitingFor = null;
    }

    @Override
    public void onClose() {
        ControlProfiles.save();
        minecraft.setScreen(parent);
    }

    /** Whether an emulator key (checked first) uses the same key, so this control would never get it. */
    private static boolean takenByEmulatorKey(Binding binding) {
        if (!binding.isBound()) {
            return false;
        }
        InputConstants.Type type = binding.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        for (KeyMapping key : EMULATOR_KEYS) {
            if (key.getKey().getType() == type && key.getKey().getValue() == binding.code()) {
                return true;
            }
        }
        return false;
    }

    private final class ControlList extends ContainerObjectSelectionList<Row> {
        ControlList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }

        void add(Row row) {
            addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final ControlPreset.Entry entry;
        private final Button keyButton;

        Row(ControlPreset.Entry entry) {
            this.entry = entry;
            this.keyButton = Button.builder(Component.empty(), b -> waitingFor = entry.action())
                    .size(KEY_BUTTON_WIDTH, 20).build();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovering, float partialTick) {
            graphics.drawString(font, entry.label(), left + 4, top + 6, 0xFFFFFFFF);
            keyButton.setPosition(left + width - KEY_BUTTON_WIDTH - 4, top);
            keyButton.setMessage(message());
            keyButton.render(graphics, mouseX, mouseY, partialTick);
        }

        private Component message() {
            Binding binding = profile.binding(entry.action());
            MutableComponent name = keyName(binding).copy();
            if (waitingFor == entry.action()) {
                return Component.literal("> ").append(name.withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                        .append(" <").withStyle(ChatFormatting.YELLOW);
            }
            if (profile.sharesKey(entry.action()) || takenByEmulatorKey(binding)) {
                return name.withStyle(ChatFormatting.RED);
            }
            return name;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(keyButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(keyButton);
        }
    }

    private final class SensitivitySlider extends AbstractSliderButton {
        SensitivitySlider(int x, int y) {
            super(x, y, 150, 20, Component.empty(), toSlider(profile.sensitivity()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.retrohandhelds.controls.sensitivity",
                    String.format(Locale.ROOT, "%.1f", fromSlider(value))));
        }

        @Override
        protected void applyValue() {
            profile.setSensitivity(fromSlider(value));
        }

        private static double toSlider(float sensitivity) {
            return (sensitivity - ControlProfile.MIN_SENSITIVITY) / (ControlProfile.MAX_SENSITIVITY - ControlProfile.MIN_SENSITIVITY);
        }

        private static float fromSlider(double value) {
            return (float) (ControlProfile.MIN_SENSITIVITY + value * (ControlProfile.MAX_SENSITIVITY - ControlProfile.MIN_SENSITIVITY));
        }
    }
}
