package com.visualticks;

import com.google.inject.Provides;
import com.visualticks.config.HotkeyMode;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.Arrays;

@PluginDescriptor(
    name = "Visual Ticks"
)
public class VisualTicksPlugin extends Plugin implements KeyListener {
    @Inject
    private VisualTicksConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private VisualTicksOverlayOne overlayOne;
    @Inject
    private VisualTicksOverlayTwo overlayTwo;
    @Inject
    private VisualTicksOverlayThree overlayThree;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ConfigManager configManager;
    @Inject
    private ClientThread clientThread;

    public final int[] ticks = new int[3];

    /**
     * Matches the {@code @Range} on every {@code numberOfTicksN}, which the hotkeys have
     * to enforce themselves — {@code setConfiguration} stores whatever it is handed.
     */
    private static final int MIN_TICKS = 2;
    private static final int MAX_TICKS = 30;

    private static final String[] SUFFIXES = {"One", "Two", "Three"};

    /**
     * Which sets the decrease hotkey hid. Without it a global increase would switch on
     * sets the user deliberately left disabled, instead of only undoing its own work.
     */
    private final boolean[] hiddenByHotkey = new boolean[3];

    @Override
    protected void startUp() throws Exception {
        updateOverlays();
        keyManager.registerKeyListener(this);
        migrate();
    }

    @Override
    protected void shutDown() throws Exception {
        for (BaseVisualTicksOverlay overlay : overlays()) {
            overlayManager.remove(overlay);
        }
        keyManager.unregisterKeyListener(this);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        // @Range only constrains the settings slider; ConfigManager hands back a stored
        // value unvalidated, so a hand-edited or synced profile can carry 0 here and
        // divide by zero on every tick. Clamp at the divide. See issue #7.
        if (config.isEnabledOne()) {
            ticks[0] = (ticks[0] + 1) % Math.max(1, config.numberOfTicksOne());
        }
        if (config.isEnabledTwo()) {
            ticks[1] = (ticks[1] + 1) % Math.max(1, config.numberOfTicksTwo());
        }
        if (config.isEnabledThree()) {
            ticks[2] = (ticks[2] + 1) % Math.max(1, config.numberOfTicksThree());
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(VisualTicksConfig.GROUP_NAME)) {
            return;
        }

        updateOverlays();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged profileChanged) {
        migrate();
    }

    private BaseVisualTicksOverlay[] overlays() {
        return new BaseVisualTicksOverlay[]{overlayOne, overlayTwo, overlayThree};
    }

    private void updateOverlays() {
        BaseVisualTicksOverlay[] overlays = overlays();
        boolean[] enabled = {config.isEnabledOne(), config.isEnabledTwo(), config.isEnabledThree()};

        for (int i = 0; i < overlays.length; i++) {
            overlayManager.remove(overlays[i]);
            if (enabled[i]) {
                overlayManager.add(overlays[i]);
            }
            // A set that is visible again is no longer the decrease hotkey's to restore,
            // however it got there.
            hiddenByHotkey[i] &= !enabled[i];
            overlays[i].onConfigChanged();
        }
    }

    @Provides
    VisualTicksConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VisualTicksConfig.class);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (pressed(config.tickResetHotkey(), e)) {
            // keyPressed runs on the AWT thread; ticks is otherwise only touched
            // by onGameTick and overlay rendering, both on the client thread.
            clientThread.invoke(() -> Arrays.fill(ticks, 0));
            return;
        }

        if (config.tickAdjustHotkeyMode() == HotkeyMode.GLOBAL) {
            if (pressed(config.tickIncreaseHotkey(), e)) {
                adjustAll(1);
            } else if (pressed(config.tickDecreaseHotkey(), e)) {
                adjustAll(-1);
            }
            return;
        }

        for (int i = 0; i < SUFFIXES.length; i++) {
            if (pressed(increaseHotkey(i), e)) {
                adjust(i, 1, true);
            } else if (pressed(decreaseHotkey(i), e)) {
                adjust(i, -1, true);
            }
        }
    }

    /**
     * An unset keybind carries {@code VK_UNDEFINED}, so leave it to no key rather than
     * trusting every keybind to reject a key event on its own.
     */
    private static boolean pressed(Keybind keybind, KeyEvent e) {
        return keybind != null && !Keybind.NOT_SET.equals(keybind) && keybind.matches(e);
    }

    private void adjustAll(int delta) {
        for (int i = 0; i < SUFFIXES.length; i++) {
            adjust(i, delta, false);
        }
    }

    /**
     * Moves one tick set by {@code delta}, hiding it below {@link #MIN_TICKS} and bringing
     * it back at {@link #MIN_TICKS}. Config is the source of truth, so every change is
     * written back through {@link ConfigManager} and reaches the overlays as a
     * {@link ConfigChanged}.
     *
     * @param targeted whether a per-set hotkey aimed at this set specifically, which
     *                 revives it however it came to be disabled. A global increase revives
     *                 only what the global decrease hid.
     */
    private void adjust(int index, int delta, boolean targeted) {
        String suffix = SUFFIXES[index];

        if (!isEnabled(index)) {
            if (delta > 0 && (targeted || hiddenByHotkey[index])) {
                hiddenByHotkey[index] = false;
                // Count first: enabling first would show the set at its stale count for a frame.
                setConfiguration("numberOfTicks" + suffix, MIN_TICKS);
                setConfiguration("isEnabled" + suffix, true);
            }
            return;
        }

        int count = numberOfTicks(index);
        if (delta < 0 && count <= MIN_TICKS) {
            // Set before the write: setConfiguration posts ConfigChanged, and
            // updateOverlays reads this flag back.
            hiddenByHotkey[index] = true;
            setConfiguration("isEnabled" + suffix, false);
            return;
        }

        // A stored count can sit outside the slider's range, so clamp the result rather
        // than the input: the hotkey then walks an out-of-range set back into range.
        setConfiguration("numberOfTicks" + suffix, Math.max(MIN_TICKS, Math.min(MAX_TICKS, count + delta)));
    }

    private void setConfiguration(String key, Object value) {
        configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, key, value);
    }

    // Read through the config proxy, never ConfigManager: only the proxy resolves
    // @ConfigItem defaults for keys the user has never touched.
    private boolean isEnabled(int index) {
        switch (index) {
            case 0:
                return config.isEnabledOne();
            case 1:
                return config.isEnabledTwo();
            default:
                return config.isEnabledThree();
        }
    }

    private int numberOfTicks(int index) {
        switch (index) {
            case 0:
                return config.numberOfTicksOne();
            case 1:
                return config.numberOfTicksTwo();
            default:
                return config.numberOfTicksThree();
        }
    }

    private Keybind increaseHotkey(int index) {
        switch (index) {
            case 0:
                return config.increaseHotkeyOne();
            case 1:
                return config.increaseHotkeyTwo();
            default:
                return config.increaseHotkeyThree();
        }
    }

    private Keybind decreaseHotkey(int index) {
        switch (index) {
            case 0:
                return config.decreaseHotkeyOne();
            case 1:
                return config.decreaseHotkeyTwo();
            default:
                return config.decreaseHotkeyThree();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void migrate() {
        String[][] moves = {
                {"paddingBetweenTicksOne", "horizontalSpacingOne", "verticalSpacingOne"},
                {"tickPaddingTwo", "horizontalSpacingTwo", "verticalSpacingTwo"},
                {"tickPaddingThree", "horizontalSpacingThree", "verticalSpacingThree"}
        };

        for (String[] move : moves) {
            String value = configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, move[0]);
            if (value != null) {
                configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, move[1], value);
                configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, move[2], value);
            }
            configManager.unsetConfiguration(VisualTicksConfig.GROUP_NAME, move[0]);
        }
    }
}
