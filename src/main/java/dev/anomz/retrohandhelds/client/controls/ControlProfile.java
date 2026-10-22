package dev.anomz.retrohandhelds.client.controls;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.anomz.retrohandhelds.RetroSystem;

/**
 * One console's controls: its preset plus what the player changed. Only changes are kept, so a better
 * preset in a later version still reaches every key the player never touched. Render thread only.
 */
public final class ControlProfile {
    public static final float DEFAULT_SENSITIVITY = 1;
    public static final float MIN_SENSITIVITY = 0.1f;
    public static final float MAX_SENSITIVITY = 4;

    private final RetroSystem system;
    private final ControlPreset preset;
    private final Map<ControlAction, Binding> changed = new EnumMap<>(ControlAction.class);
    private Boolean mouseLook;
    private Boolean mouseVertical;
    private float sensitivity = DEFAULT_SENSITIVITY;
    /** Key to actions, rebuilt whenever a binding changes. */
    private Map<Binding, List<ControlAction>> lookup;

    public ControlProfile(RetroSystem system) {
        this.system = system;
        this.preset = ControlPreset.forSystem(system);
    }

    public RetroSystem system() {
        return system;
    }

    public ControlPreset preset() {
        return preset;
    }

    public Binding binding(ControlAction action) {
        Binding binding = changed.get(action);
        if (binding != null) {
            return binding;
        }
        for (ControlPreset.Entry entry : preset.entries()) {
            if (entry.action() == action) {
                return entry.defaultBinding();
            }
        }
        return Binding.NONE;
    }

    public void set(ControlAction action, Binding binding) {
        if (binding.equals(presetBinding(action))) {
            changed.remove(action);
        } else {
            changed.put(action, binding);
        }
        lookup = null;
    }

    /** Back to the preset: every key, mouse look and sensitivity. */
    public void reset() {
        changed.clear();
        mouseLook = null;
        mouseVertical = null;
        sensitivity = DEFAULT_SENSITIVITY;
        lookup = null;
    }

    /** The actions {@code binding} drives in this console; empty for an unbound or unused key. */
    public List<ControlAction> actionsFor(Binding binding) {
        if (lookup == null) {
            Map<Binding, List<ControlAction>> map = new HashMap<>();
            for (ControlPreset.Entry entry : preset.entries()) {
                Binding bound = binding(entry.action());
                if (bound.isBound()) {
                    map.computeIfAbsent(bound, b -> new ArrayList<>()).add(entry.action());
                }
            }
            lookup = map;
        }
        return lookup.getOrDefault(binding, List.of());
    }

    /** Whether another control of this console uses the same key. */
    public boolean sharesKey(ControlAction action) {
        Binding bound = binding(action);
        return bound.isBound() && actionsFor(bound).size() > 1;
    }

    /** Whether the mouse moves the right stick; only for consoles with analog sticks. */
    public boolean mouseLook() {
        return system.controls().analog && (mouseLook != null ? mouseLook : preset.mouseLook());
    }

    public void setMouseLook(boolean on) {
        mouseLook = on == preset.mouseLook() ? null : on;
    }

    /** Whether up/down mouse movement moves the stick too. */
    public boolean mouseVertical() {
        return mouseVertical != null ? mouseVertical : preset.mouseVertical();
    }

    public void setMouseVertical(boolean on) {
        mouseVertical = on == preset.mouseVertical() ? null : on;
    }

    public float sensitivity() {
        return sensitivity;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.clamp(sensitivity, MIN_SENSITIVITY, MAX_SENSITIVITY);
    }

    /** Whether anything differs from the preset, i.e. whether there is anything to save. */
    public boolean isChanged() {
        return !changed.isEmpty() || mouseLook != null || mouseVertical != null || sensitivity != DEFAULT_SENSITIVITY;
    }

    Map<ControlAction, Binding> changedBindings() {
        return changed;
    }

    Boolean mouseLookOverride() {
        return mouseLook;
    }

    Boolean mouseVerticalOverride() {
        return mouseVertical;
    }

    private Binding presetBinding(ControlAction action) {
        for (ControlPreset.Entry entry : preset.entries()) {
            if (entry.action() == action) {
                return entry.defaultBinding();
            }
        }
        return Binding.NONE;
    }
}
