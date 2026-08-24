package com.visualticks.config;

import com.visualticks.VisualTicksConfig;

import java.awt.Color;

/**
 * Snapshot of one tick set's config values, taken when the config changes so the
 * render loop reads plain fields instead of proxy calls.
 *
 * <p>The three factories exist because RuneLite resolves {@code @ConfigItem}
 * defaults only through the config proxy — reading keys generically by suffix via
 * {@code ConfigManager.getConfiguration} returns null for anything the user never
 * changed, losing every default.
 */
public class TickSettings
{
    public boolean showText;
    public boolean showShape;
    public TickShape shape;
    public InterfaceTab exclusiveTab;
    public int numberOfTicks;
    public int amountPerRow;
    public int shapeSize;
    public int arc;
    public int textSize;
    public int horizontalSpacing;
    public int verticalSpacing;
    public Color tickColour;
    public Color currentTickColour;
    public Color textColour;
    public Color currentTextColour;

    public static TickSettings one(VisualTicksConfig c)
    {
        TickSettings s = new TickSettings();
        s.showText = c.shouldShowTextOne();
        s.showShape = c.shouldShowTickShapeOne();
        s.shape = c.tickShapeOne();
        s.exclusiveTab = c.exclusiveTabOne();
        s.numberOfTicks = c.numberOfTicksOne();
        s.amountPerRow = c.amountPerRowOne();
        s.shapeSize = c.sizeOfTickShapesOne();
        s.arc = c.tickArcOne();
        s.textSize = c.tickTextSizeOne();
        s.horizontalSpacing = c.horizontalSpacingOne();
        s.verticalSpacing = c.verticalSpacingOne();
        s.tickColour = c.tickColourOne();
        s.currentTickColour = c.currentTickColourOne();
        s.textColour = c.tickTextColourOne();
        s.currentTextColour = c.currentTickTextColourOne();
        return s;
    }

    public static TickSettings two(VisualTicksConfig c)
    {
        TickSettings s = new TickSettings();
        s.showText = c.shouldShowTextTwo();
        s.showShape = c.shouldShowTickShapeTwo();
        s.shape = c.tickShapeTwo();
        s.exclusiveTab = c.exclusiveTabTwo();
        s.numberOfTicks = c.numberOfTicksTwo();
        s.amountPerRow = c.amountPerRowTwo();
        s.shapeSize = c.sizeOfTickShapesTwo();
        s.arc = c.tickArcTwo();
        s.textSize = c.tickTextSizeTwo();
        s.horizontalSpacing = c.horizontalSpacingTwo();
        s.verticalSpacing = c.verticalSpacingTwo();
        s.tickColour = c.tickColourTwo();
        s.currentTickColour = c.currentTickColourTwo();
        s.textColour = c.tickTextColourTwo();
        s.currentTextColour = c.currentTickTextColourTwo();
        return s;
    }

    public static TickSettings three(VisualTicksConfig c)
    {
        TickSettings s = new TickSettings();
        s.showText = c.shouldShowTextThree();
        s.showShape = c.shouldShowTickShapeThree();
        s.shape = c.tickShapeThree();
        s.exclusiveTab = c.exclusiveTabThree();
        s.numberOfTicks = c.numberOfTicksThree();
        s.amountPerRow = c.amountPerRowThree();
        s.shapeSize = c.sizeOfTickShapesThree();
        s.arc = c.tickArcThree();
        s.textSize = c.tickTextSizeThree();
        s.horizontalSpacing = c.horizontalSpacingThree();
        s.verticalSpacing = c.verticalSpacingThree();
        s.tickColour = c.tickColourThree();
        s.currentTickColour = c.currentTickColourThree();
        s.textColour = c.tickTextColourThree();
        s.currentTextColour = c.currentTickTextColourThree();
        return s;
    }
}
