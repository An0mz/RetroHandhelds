package dev.anomz.retrohandhelds.client.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.input.MouseLook;

class ControlsTest {
    @Test
    void everyConsoleHasAPresetWithoutSharedKeysOrRepeatedControls() {
        for (RetroSystem system : RetroSystem.values()) {
            ControlPreset preset = ControlPreset.forSystem(system);
            assertFalse(preset.entries().isEmpty(), system + " has controls");
            Set<Binding> keys = new HashSet<>();
            Set<ControlAction> actions = new HashSet<>();
            for (ControlPreset.Entry entry : preset.entries()) {
                assertTrue(actions.add(entry.action()), system + " lists " + entry.action() + " twice");
                if (entry.defaultBinding().isBound()) {
                    assertTrue(keys.add(entry.defaultBinding()), system + ": " + entry.label() + " shares its key");
                }
            }
        }
    }

    @Test
    void consolesNameTheirOwnButtons() {
        ControlProfile n64 = new ControlProfile(RetroSystem.N64);
        assertEquals(List.of(ControlAction.B), n64.actionsFor(Binding.key(GLFW_KEY_SPACE)), "N64 A is the RetroPad's B");
        assertEquals(List.of(ControlAction.L2), n64.actionsFor(Binding.key(GLFW_KEY_LEFT_SHIFT)), "N64 Z is L2");
        assertEquals(List.of(ControlAction.LEFT_STICK_UP), n64.actionsFor(Binding.key(GLFW_KEY_W)));
        assertFalse(n64.mouseLook(), "the arrow keys are the C-buttons; the mouse is opt-in");
        assertEquals(MouseLook.Mode.TAPS, n64.preset().mouseMode(), "C-buttons are tapped");
        assertFalse(n64.mouseVertical(), "no first-person view by accident");
        assertEquals(MouseLook.Mode.STICK, new ControlProfile(RetroSystem.PLAYSTATION).preset().mouseMode());
        ControlProfile gba = new ControlProfile(RetroSystem.GAME_BOY_ADVANCE);
        assertEquals(List.of(ControlAction.A), gba.actionsFor(Binding.key(GLFW_KEY_X)), "GBA keeps the old keys");
        assertFalse(gba.mouseLook(), "no sticks, no mouse look");
    }

    @Test
    void rebindingAndResetting() {
        ControlProfile profile = new ControlProfile(RetroSystem.SNES);
        profile.set(ControlAction.A, Binding.mouseButton(GLFW_MOUSE_BUTTON_LEFT));
        assertEquals(List.of(ControlAction.A), profile.actionsFor(Binding.mouseButton(GLFW_MOUSE_BUTTON_LEFT)));
        assertEquals(List.of(), profile.actionsFor(Binding.key(GLFW_KEY_X)), "the old key is free");
        profile.set(ControlAction.B, Binding.mouseButton(GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(profile.sharesKey(ControlAction.A));
        assertTrue(profile.isChanged());
        profile.reset();
        assertFalse(profile.isChanged());
        assertEquals(Binding.key(GLFW_KEY_X), profile.binding(ControlAction.A));
    }

    @Test
    void settingThePresetKeyAgainIsNoChange() {
        ControlProfile profile = new ControlProfile(RetroSystem.NES);
        profile.set(ControlAction.A, Binding.key(GLFW_KEY_Q));
        profile.set(ControlAction.A, Binding.key(GLFW_KEY_X));
        assertFalse(profile.isChanged());
    }

    @Test
    void onlyChangesAreSavedAndTheyComeBack() {
        ControlProfile n64 = new ControlProfile(RetroSystem.N64);
        n64.set(ControlAction.B, Binding.key(GLFW_KEY_J));
        n64.setMouseLook(true);
        n64.setMouseVertical(true);
        n64.setSensitivity(2.5f);
        String json = ControlProfiles.write(List.of(n64, new ControlProfile(RetroSystem.NES)));
        assertFalse(json.contains("\"nes\""), "untouched consoles are not saved");

        Map<RetroSystem, ControlProfile> read = ControlProfiles.read(json);
        ControlProfile back = read.get(RetroSystem.N64);
        assertEquals(Binding.key(GLFW_KEY_J), back.binding(ControlAction.B));
        assertEquals(Binding.key(GLFW_KEY_LEFT_SHIFT), back.binding(ControlAction.L2), "untouched keys follow the preset");
        assertTrue(back.mouseLook());
        assertTrue(back.mouseVertical());
        assertEquals(2.5f, back.sensitivity());
    }

    @Test
    void unreadableEntriesAreSkipped() {
        String json = """
                {"n64": {"bindings": {"B": "key:abc", "NOT_A_BUTTON": "key:65", "A": "mouse:1"}, "mouseSensitivity": 99},
                 "gamecube": {"bindings": {"A": "key:65"}},
                 "snes": "nonsense"}
                """;
        Map<RetroSystem, ControlProfile> read = ControlProfiles.read(json);
        assertEquals(Set.of(RetroSystem.N64), read.keySet());
        ControlProfile n64 = read.get(RetroSystem.N64);
        assertEquals(Binding.key(GLFW_KEY_SPACE), n64.binding(ControlAction.B));
        assertEquals(Binding.mouseButton(1), n64.binding(ControlAction.A));
        assertEquals(ControlProfile.MAX_SENSITIVITY, n64.sensitivity(), "clamped");
    }

    @Test
    void bindingsRoundTripAsText() {
        for (Binding binding : List.of(Binding.key(GLFW_KEY_Z), Binding.mouseButton(2), Binding.NONE)) {
            assertEquals(Optional.of(binding), Binding.parse(binding.serialize()));
        }
        assertEquals(Optional.empty(), Binding.parse("key:-4"));
        assertEquals(Optional.empty(), Binding.parse("joystick:1"));
    }
}
