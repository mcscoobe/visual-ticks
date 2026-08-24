package com.visualticks;

import com.google.inject.Provides;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
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

    public final int[] ticks = new int[3];

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
        if (config.isEnabledOne()) {
            ticks[0] = (ticks[0] + 1) % config.numberOfTicksOne();
        }
        if (config.isEnabledTwo()) {
            ticks[1] = (ticks[1] + 1) % config.numberOfTicksTwo();
        }
        if (config.isEnabledThree()) {
            ticks[2] = (ticks[2] + 1) % config.numberOfTicksThree();
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
        if (config.tickResetHotkey().matches(e)) {
            Arrays.fill(ticks, 0);
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
