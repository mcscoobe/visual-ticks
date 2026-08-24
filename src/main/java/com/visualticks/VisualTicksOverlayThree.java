package com.visualticks;

import com.visualticks.config.TickSettings;
import net.runelite.api.Client;

import javax.inject.Inject;

public class VisualTicksOverlayThree extends BaseVisualTicksOverlay
{
    @Inject
    public VisualTicksOverlayThree(VisualTicksPlugin plugin, VisualTicksConfig config, Client client)
    {
        super(plugin, config, client);
    }

    @Override
    protected TickSettings readSettings() {
        return TickSettings.three(config);
    }

    @Override
    protected int getCurrentTick() {
        return plugin.ticks[2];
    }
}
