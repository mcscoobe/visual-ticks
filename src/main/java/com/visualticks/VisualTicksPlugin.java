package com.visualticks;

import com.google.inject.Provides;
import com.visualticks.config.HotkeyMode;
import com.visualticks.config.InterfaceLayer;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
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

import static com.visualticks.VisualTicksConfig.MAX_TICKS;
import static com.visualticks.VisualTicksConfig.MIN_TICKS;

/**
 * Owns the per-set tick counters and the overlays that draw them, and handles the reset
 * and adjust hotkeys.
 *
 * <p>Thread map, so new state is guarded for the threads that actually touch it:
 * <ul>
 *   <li>client thread: {@code onGameTick} and overlay rendering;</li>
 *   <li>Swing EDT: {@code startUp}/{@code shutDown}, key events and config panel edits;</li>
 *   <li>any thread: {@code onConfigChanged} and {@code onProfileChanged} run on whichever
 *       thread wrote the config or switched profile. Hotkey and panel writes both post
 *       {@code ConfigChanged} from the Swing EDT.</li>
 * </ul>
 * Only the first group runs on the client thread, so anything the others share with
 * {@code onGameTick} or rendering must hop through {@link ClientThread} or be safe to
 * publish across threads.
 */
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
    @Inject
    private Client client;

    /**
     * Current tick per tick set: the index is the tick set, the value is its 0-based current
     * tick. Drawn labels are 1-based, so counter 0 shows as "1".
     */
    public final int[] counters = new int[3];

    private static final String[] TICK_SET_SUFFIXES = {"One", "Two", "Three"};

    /**
     * Bit per tick set: which sets the decrease hotkey hid. Without it a global increase
     * would switch on sets the user deliberately left disabled, instead of only undoing
     * its own work.
     *
     * <p>Written from {@code keyPressed} on the Swing EDT, and cleared in
     * {@code startUp} (Swing EDT), {@code onProfileChanged} (whichever thread switched profile)
     * and {@code updateOverlays} (whichever thread posted the {@code ConfigChanged}) —
     * never on the client thread, but on more than one thread, hence volatile. The bit
     * updates are read-modify-write rather than atomic; the cost of losing that race is one
     * revive that does not happen, so a lock would buy nothing.
     *
     * <p>Deliberately not persisted: it lives for the session only, and is cleared on
     * startup and on profile change so a claim from one profile can never revive a set in
     * another. A set hidden by hotkey and left that way across a restart has to be
     * re-enabled from the config panel.
     */
    private volatile int hiddenByHotkey;

    @Override
    protected void startUp() throws Exception {
        hiddenByHotkey = 0;
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
        for (int i = 0; i < counters.length; i++) {
            if (isEnabled(i)) {
                counters[i] = (counters[i] + 1) % Math.max(1, numberOfTicks(i));
            }
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
        // The new profile has its own enabled flags, so a claim carried over from the old
        // one would revive a set the user disabled here.
        hiddenByHotkey = 0;
        migrate();
    }

    private BaseVisualTicksOverlay[] overlays() {
        return new BaseVisualTicksOverlay[]{overlayOne, overlayTwo, overlayThree};
    }

    private void updateOverlays() {
        BaseVisualTicksOverlay[] overlays = overlays();

        int enabledMask = 0;

        for (int i = 0; i < overlays.length; i++) {
            overlayManager.remove(overlays[i]);
            // OverlayManager applies a layer only when add() rebuilds its layer map, so the
            // remove/add cycle here is what makes a layer change take effect. Set on every
            // set, enabled or not, so one enabled later is added on the right layer.
            overlays[i].setLayer(interfaceLayer(i).getOverlayLayer());
            if (isEnabled(i)) {
                overlayManager.add(overlays[i]);
                enabledMask |= 1 << i;
            }
            overlays[i].onConfigChanged();
        }

        // A set that is visible again is no longer the decrease hotkey's to restore,
        // however it got there.
        hiddenByHotkey &= ~enabledMask;
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
        if (config.tickResetHotkey().matches(e)) {
            // keyPressed runs on the Swing EDT; counters is otherwise only touched
            // by onGameTick and overlay rendering, both on the client thread.
            clientThread.invoke(() -> Arrays.fill(counters, 0));
            return;
        }

        // KeyManager only withholds key events on the login screen, so a printable key
        // bound here reaches the plugin while the player is typing. The reset hotkey can
        // afford that — it clears a counter — but an adjustment rewrites stored settings.
        if (isTyping()) {
            return;
        }

        if (config.tickAdjustHotkeyMode() == HotkeyMode.GLOBAL) {
            if (config.tickIncreaseHotkey().matches(e)) {
                adjustAll(1);
                e.consume();
            } else if (config.tickDecreaseHotkey().matches(e)) {
                adjustAll(-1);
                e.consume();
            }
            return;
        }

        for (int i = 0; i < TICK_SET_SUFFIXES.length; i++) {
            if (increaseHotkey(i).matches(e)) {
                adjust(i, 1, true);
                e.consume();
            } else if (decreaseHotkey(i).matches(e)) {
                adjust(i, -1, true);
                e.consume();
            }
        }
    }

    /**
     * Whether the player is entering text. Covers an open input dialog and a chat message
     * already under way; the first printable key into an empty chatbox is indistinguishable
     * from a hotkey press, which is why a matched adjustment consumes its key event rather
     * than letting it through to the game as well. Bind a modifier combination to keep a
     * key usable for both.
     */
    private boolean isTyping() {
        if (client.getVarcIntValue(VarClientID.MESLAYERMODE) != InputType.NONE.getType()) {
            return true;
        }

        String typed = client.getVarcStrValue(VarClientID.CHATINPUT);
        return typed != null && !typed.isEmpty();
    }

    private void adjustAll(int delta) {
        for (int i = 0; i < TICK_SET_SUFFIXES.length; i++) {
            adjust(i, delta, false);
        }
    }

    /**
     * Moves one tick set by {@code delta}, hiding it when a decrease would take it below
     * {@link VisualTicksConfig#MIN_TICKS}. An increase on a hidden set shows it again at its stored count,
     * raised to {@link VisualTicksConfig#MIN_TICKS} only if it is stored below that. Config is the source of
     * truth, so every change is written back through {@link ConfigManager} and reaches
     * the overlays as a {@link ConfigChanged}.
     *
     * @param targeted whether a per-set hotkey aimed at this set specifically, which
     *                 revives it however it came to be disabled. A global increase revives
     *                 only what the global decrease hid.
     */
    private void adjust(int index, int delta, boolean targeted) {
        String suffix = TICK_SET_SUFFIXES[index];
        int bit = 1 << index;

        if (!isEnabled(index)) {
            if (delta > 0 && (targeted || (hiddenByHotkey & bit) != 0)) {
                hiddenByHotkey &= ~bit;
                // Hiding a set leaves its count alone, so a set hidden at the minimum is
                // still stored there and needs no write. Only a count below the minimum
                // has to be normalised: overwriting unconditionally would throw away the
                // count of a set the user sized and then switched off by hand.
                //
                // Count first: enabling first would show the set at its stale count for a frame.
                if (numberOfTicks(index) < MIN_TICKS) {
                    setConfiguration("numberOfTicks" + suffix, MIN_TICKS);
                }
                setConfiguration("isEnabled" + suffix, true);
            }
            return;
        }

        int count = numberOfTicks(index);
        if (delta < 0 && count <= MIN_TICKS) {
            // Set before the write: setConfiguration posts ConfigChanged, and
            // updateOverlays reads this flag back.
            hiddenByHotkey |= bit;
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

    // Per-set lookups. Read through the typed proxy getters rather than
    // ConfigManager.getConfiguration by string key: the compiler checks every getter name,
    // and nothing has to parse a stored string back into an int, enum or Keybind. The proxy
    // also resolves the @ConfigItem default for a key not yet in the store.
    //
    // Each switch names all three sets and throws on anything else, so a fourth set fails
    // the first time it is used instead of silently reading set Three's config.
    private boolean isEnabled(int index) {
        switch (index) {
            case 0:
                return config.isEnabledOne();
            case 1:
                return config.isEnabledTwo();
            case 2:
                return config.isEnabledThree();
            default:
                throw new IllegalArgumentException("no tick set " + index);
        }
    }

    private int numberOfTicks(int index) {
        switch (index) {
            case 0:
                return config.numberOfTicksOne();
            case 1:
                return config.numberOfTicksTwo();
            case 2:
                return config.numberOfTicksThree();
            default:
                throw new IllegalArgumentException("no tick set " + index);
        }
    }

    private InterfaceLayer interfaceLayer(int index) {
        switch (index) {
            case 0:
                return config.interfaceLayerOne();
            case 1:
                return config.interfaceLayerTwo();
            case 2:
                return config.interfaceLayerThree();
            default:
                throw new IllegalArgumentException("no tick set " + index);
        }
    }

    private Keybind increaseHotkey(int index) {
        switch (index) {
            case 0:
                return config.increaseHotkeyOne();
            case 1:
                return config.increaseHotkeyTwo();
            case 2:
                return config.increaseHotkeyThree();
            default:
                throw new IllegalArgumentException("no tick set " + index);
        }
    }

    private Keybind decreaseHotkey(int index) {
        switch (index) {
            case 0:
                return config.decreaseHotkeyOne();
            case 1:
                return config.decreaseHotkeyTwo();
            case 2:
                return config.decreaseHotkeyThree();
            default:
                throw new IllegalArgumentException("no tick set " + index);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Splits each legacy single-padding key into the horizontal and vertical spacing pair
     * that replaced it, then unsets it. The legacy keys were written by builds before commit
     * {@code 3083e66}, which split padding into two axes. Set One's legacy key is
     * {@code paddingBetweenTicksOne} rather than {@code tickPaddingOne} because set One
     * predates multi-set support; the names are history and must match what was stored.
     *
     * <p>Runs on {@code startUp} and again on {@code ProfileChanged}, because a profile
     * switch loads a different set of stored keys that may still hold a legacy one.
     *
     * <p>Can be removed once no supported install can still hold a pre-{@code 3083e66}
     * profile; a user who skips past that removal silently falls back to default spacing.
     */
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
