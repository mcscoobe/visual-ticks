package com.visualticks;

import com.visualticks.config.TickSettings;
import net.runelite.api.Client;

import javax.inject.Inject;

/**
 * Do not rename: the class name is this overlay's persisted position key. See
 * {@link BaseVisualTicksOverlay}.
 */
public class VisualTicksOverlayOne extends BaseVisualTicksOverlay
{
    @Inject
    public VisualTicksOverlayOne(VisualTicksPlugin plugin, VisualTicksConfig config, Client client)
    {
        super(plugin, config, client);
    }

    @Override
    protected TickSettings readSettings() {
        return TickSettings.one(config);
    }

    @Override
    protected int getCurrentTick() {
        return plugin.counters[0];
    }
}
