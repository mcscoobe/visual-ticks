package com.visualticks;

import com.visualticks.config.InterfaceTab;
import com.visualticks.config.TickShape;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import javax.inject.Inject;
import java.awt.*;

@Slf4j
public class VisualTicksOverlayHotkeys extends BaseVisualTicksOverlay
{
    @Inject
    public VisualTicksOverlayHotkeys(VisualTicksPlugin plugin, VisualTicksConfig config, Client client)
    {
        super(plugin, config, client);
    }

    @Override
    protected boolean shouldShowText() {
        return config.shouldShowTextHotkeys();
    }

    @Override
    protected boolean shouldShowTickShape() {
        return config.shouldShowTickShapeHotkeys();
    }

    @Override
    protected int getTickTextSize() {
        return config.tickTextSizeHotkeys();
    }

    @Override
    protected int getNumberOfTicks() {
        return config.numberOfTicksHotkeys();
    }

    @Override
    protected Color getTickColour() {
        return config.tickColourHotkeys();
    }

    @Override
    protected Color getCurrentTickColour() {
        return config.currentTickColourHotkeys();
    }

    @Override
    protected int getAmountPerRow() {
        return config.amountPerRowHotkeys();
    }

    @Override
    protected int getSizeOfTickShapes() {
        return config.sizeOfTickShapesHotkeys();
    }

    @Override
    protected int getHorizontalSpacing() {
        return config.horizontalSpacingHotkeys();
    }

    @Override
    protected int getVerticalSpacing() {
        return config.verticalSpacingHotkeys();
    }

    @Override
    protected int getCurrentTick() {
        return plugin.tickHotkeys;
    }

    @Override
    protected InterfaceTab getExclusiveTab() {
        return config.exclusiveTabHotkeys();
    }

    @Override
    protected Color getTickTextColour() {
        return config.tickTextColourHotkeys();
    }

    @Override
    protected Color getCurrentTickTextColour() {
        return config.currentTickTextColourHotkeys();
    }

    @Override
    protected TickShape getTickShape() {
        return config.tickShapeHotkeys();
    }

    @Override
    protected int getTickArc() {
        return config.tickArcHotkeys();
    }
}