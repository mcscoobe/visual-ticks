package com.visualticks;

import com.visualticks.config.TickSettings;
import net.runelite.api.Client;

import javax.inject.Inject;

public class VisualTicksOverlayTwo extends BaseVisualTicksOverlay
{
    @Inject
    public VisualTicksOverlayTwo(VisualTicksPlugin plugin, VisualTicksConfig config, Client client)
    {
        super(plugin, config, client);
    }

    @Override
    protected TickSettings readSettings() {
        return TickSettings.two(config);
    }

    @Override
    protected int getCurrentTick() {
        return plugin.ticks[1];
    }
}
