package com.visualticks;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import java.awt.event.KeyEvent;

@Slf4j
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
    private VisualTicksOverlayHotkeys overlayHotkeys;
    @Inject
    private KeyManager keyManager;
    @Inject
    private ConfigManager configManager;
    public int tickOne = 0;
    public int tickTwo = 0;
    public int tickThree = 0;
    public int tickHotkeys = 0;

    @Override
    protected void startUp() throws Exception {
        updateOverlays();
        keyManager.registerKeyListener(this);
        migrate();
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlayOne);
        overlayManager.remove(overlayTwo);
        overlayManager.remove(overlayThree);
        overlayManager.remove(overlayHotkeys);
        keyManager.unregisterKeyListener(this);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (!config.isEnabledOne() && !config.isEnabledTwo() && !config.isEnabledThree() && !config.isEnabledHotkeys()) {
            return;
        }

        if(config.isEnabledOne()) {
            tickOne = (tickOne + 1) % config.numberOfTicksOne();
        }

        if(config.isEnabledTwo()) {
            tickTwo = (tickTwo + 1) % config.numberOfTicksTwo();
        }

        if(config.isEnabledThree()) {
            tickThree = (tickThree + 1) % config.numberOfTicksThree();
        }

        if(config.isEnabledHotkeys()) {
            tickHotkeys = (tickHotkeys + 1) % config.numberOfTicksHotkeys();
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(VisualTicksConfig.GROUP_NAME)) {
            return;
        }

        // Handle hotkey configuration changes
        String key = event.getKey();
        if (isHotkeyConfigurationKey(key)) {
            // Re-register hotkeys with KeyManager when hotkey configs change
            keyManager.unregisterKeyListener(this);
            keyManager.registerKeyListener(this);
        }
        
        // Handle numberOfTicks changes with proper tick value adjustment
        if (isNumberOfTicksConfigurationKey(key)) {
            adjustTickValuesForConfigurationChange(key);
        }

        updateOverlays();
    }

    private void updateOverlays() {
        overlayManager.remove(overlayOne);
        overlayManager.remove(overlayTwo);
        overlayManager.remove(overlayThree);
        overlayManager.remove(overlayHotkeys);

        if (config.isEnabledOne()) {
            overlayManager.add(overlayOne);
        }
        if (config.isEnabledTwo()) {
            overlayManager.add(overlayTwo);
        }
        if (config.isEnabledThree()) {
            overlayManager.add(overlayThree);
        }
        if (config.isEnabledHotkeys()) {
            overlayManager.add(overlayHotkeys);
        }

        overlayOne.onConfigChanged();
        overlayTwo.onConfigChanged();
        overlayThree.onConfigChanged();
        overlayHotkeys.onConfigChanged();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged profileChanged) {
        migrate();
    }

    @Provides
    VisualTicksConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VisualTicksConfig.class);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No implementation needed
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Existing tick reset functionality
        if (config.tickResetHotkey().matches(e)) {
            resetTicks();
            return;
        }
        
        // Handle increment/decrement hotkeys for hotkey configuration only
        handleIncrementDecrementKeys(e);
    }
    
    /**
     * Handles increment and decrement hotkey presses for the hotkey configuration.
     * Increments/decrements the numberOfTicksHotkeys configuration value.
     * 
     * @param e The KeyEvent to process
     */
    private void handleIncrementDecrementKeys(KeyEvent e) {
        // Hotkey configuration increment/decrement for numberOfTicks
        if (config.tickIncrementHotkey().matches(e)) {
            incrementNumberOfTicks();
            return;
        }
        if (config.tickDecrementHotkey().matches(e)) {
            decrementNumberOfTicks();
            return;
        }
    }
    
    /**
     * Increments the numberOfTicksHotkeys configuration value.
     * Maximum value is capped at 30 (as defined in the @Range annotation).
     */
    private void incrementNumberOfTicks() {
        int currentValue = config.numberOfTicksHotkeys();
        if (currentValue < 30) { // Max value from @Range annotation
            configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksHotkeys", currentValue + 1);
        }
    }
    
    /**
     * Decrements the numberOfTicksHotkeys configuration value.
     * Minimum value is capped at 2 (as defined in the @Range annotation).
     */
    private void decrementNumberOfTicks() {
        int currentValue = config.numberOfTicksHotkeys();
        if (currentValue > 2) { // Min value from @Range annotation
            configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksHotkeys", currentValue - 1);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No implementation needed
    }

    private void migrate() {
        String[][] migrationMap = {
                {"paddingBetweenTicksOne", "horizontalSpacingOne,verticalSpacingOne"},
                {"tickPaddingTwo", "horizontalSpacingTwo,verticalSpacingTwo"},
                {"tickPaddingThree", "horizontalSpacingThree,verticalSpacingThree"}
        };

        for (String[] migration : migrationMap) {
            String oldKey = migration[0];
            String[] newKeys = migration[1].split(",");

            for (String newKey : newKeys) {
                if (configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, oldKey) != null) {
                    String value = configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, oldKey);
                    configManager.setConfiguration(VisualTicksConfig.GROUP_NAME, newKey, value);
                }
            }

            configManager.unsetConfiguration(VisualTicksConfig.GROUP_NAME, oldKey);
        }
    }

    private void resetTicks() {
        tickOne = 0;
        tickTwo = 0;
        tickThree = 0;
        tickHotkeys = 0;
    }

    /**
     * Increments the tick count for the specified configuration.
     * Uses modulo arithmetic to wrap around when reaching the maximum tick count.
     * Only operates on enabled configurations.
     * 
     * @param configurationNumber The configuration to increment (1, 2, 3, or 4)
     */
    private void incrementTick(int configurationNumber) {
        if (!isConfigurationEnabled(configurationNumber)) {
            return;
        }

        switch (configurationNumber) {
            case 1:
                tickOne = (tickOne + 1) % config.numberOfTicksOne();
                break;
            case 2:
                tickTwo = (tickTwo + 1) % config.numberOfTicksTwo();
                break;
            case 3:
                tickThree = (tickThree + 1) % config.numberOfTicksThree();
                break;
            case 4:
                tickHotkeys = (tickHotkeys + 1) % config.numberOfTicksHotkeys();
                break;
        }
    }

    /**
     * Decrements the tick count for the specified configuration.
     * Uses modulo arithmetic to wrap around when going below zero.
     * Only operates on enabled configurations.
     * 
     * @param configurationNumber The configuration to decrement (1, 2, 3, or 4)
     */
    private void decrementTick(int configurationNumber) {
        if (!isConfigurationEnabled(configurationNumber)) {
            return;
        }

        switch (configurationNumber) {
            case 1:
                tickOne = (tickOne - 1 + config.numberOfTicksOne()) % config.numberOfTicksOne();
                break;
            case 2:
                tickTwo = (tickTwo - 1 + config.numberOfTicksTwo()) % config.numberOfTicksTwo();
                break;
            case 3:
                tickThree = (tickThree - 1 + config.numberOfTicksThree()) % config.numberOfTicksThree();
                break;
            case 4:
                tickHotkeys = (tickHotkeys - 1 + config.numberOfTicksHotkeys()) % config.numberOfTicksHotkeys();
                break;
        }
    }

    /**
     * Helper method to check if a configuration is enabled before manipulation.
     * 
     * @param configurationNumber The configuration to check (1, 2, 3, or 4)
     * @return true if the configuration is enabled, false otherwise
     */
    private boolean isConfigurationEnabled(int configurationNumber) {
        switch (configurationNumber) {
            case 1:
                return config.isEnabledOne();
            case 2:
                return config.isEnabledTwo();
            case 3:
                return config.isEnabledThree();
            case 4:
                return config.isEnabledHotkeys();
            default:
                return false;
        }
    }

    /**
     * Checks if the given configuration key is a hotkey configuration.
     * 
     * @param key The configuration key to check
     * @return true if the key is a hotkey configuration, false otherwise
     */
    private boolean isHotkeyConfigurationKey(String key) {
        return "tickResetHotkey".equals(key) || 
               "tickIncrementHotkey".equals(key) || 
               "tickDecrementHotkey".equals(key);
    }

    /**
     * Checks if the given configuration key is a numberOfTicks configuration.
     * 
     * @param key The configuration key to check
     * @return true if the key is a numberOfTicks configuration, false otherwise
     */
    private boolean isNumberOfTicksConfigurationKey(String key) {
        return "numberOfTicksOne".equals(key) || 
               "numberOfTicksTwo".equals(key) || 
               "numberOfTicksThree".equals(key) || 
               "numberOfTicksHotkeys".equals(key);
    }

    /**
     * Adjusts tick values when numberOfTicks configuration changes.
     * Uses modulo operation to ensure tick values remain within valid range.
     * 
     * @param key The configuration key that changed
     */
    private void adjustTickValuesForConfigurationChange(String key) {
        switch (key) {
            case "numberOfTicksOne":
                if (config.numberOfTicksOne() > 0) {
                    tickOne = tickOne % config.numberOfTicksOne();
                }
                break;
            case "numberOfTicksTwo":
                if (config.numberOfTicksTwo() > 0) {
                    tickTwo = tickTwo % config.numberOfTicksTwo();
                }
                break;
            case "numberOfTicksThree":
                if (config.numberOfTicksThree() > 0) {
                    tickThree = tickThree % config.numberOfTicksThree();
                }
                break;
            case "numberOfTicksHotkeys":
                if (config.numberOfTicksHotkeys() > 0) {
                    tickHotkeys = tickHotkeys % config.numberOfTicksHotkeys();
                }
                break;
        }
    }
}
