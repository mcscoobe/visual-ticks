package com.visualticks;

import com.google.inject.Inject;
import com.visualticks.config.Tick;
import com.visualticks.config.TickSettings;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseVisualTicksOverlay extends Overlay
{
    protected VisualTicksPlugin plugin;
    protected VisualTicksConfig config;
    protected Client client;
    protected boolean configChanged = true;
    protected TickSettings s;
    protected final List<Tick> ticks = new ArrayList<>();
    protected final Dimension dimension = new Dimension();

    @Inject
    public BaseVisualTicksOverlay(VisualTicksPlugin plugin, VisualTicksConfig config, Client client)
    {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    public void onConfigChanged() {
        configChanged = true;
    }

    protected abstract TickSettings readSettings();
    protected abstract int getCurrentTick();

    protected void calculateSizes(Graphics2D g) {
        configChanged = false;
        ticks.clear();

        g.setFont(g.getFont().deriveFont((float) s.textSize));
        FontMetrics fm = g.getFontMetrics();

        int maxBoundingSize = 0;
        int maxCol = 0;
        int maxRow = 0;

        for (int i = 0; i < s.numberOfTicks; i++)
        {
            int boundingSize = s.showShape ? s.shapeSize : 0;

            String text = String.valueOf(i + 1);
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();

            if (s.showText) {
                boundingSize = Math.max(boundingSize, textWidth);
                boundingSize = Math.max(boundingSize, textHeight);
            }

            int row = i / s.amountPerRow;
            int col = i % s.amountPerRow;
            int x = col * (boundingSize + s.horizontalSpacing);
            int y = row * (boundingSize + s.verticalSpacing);

            Tick tick = new Tick(x, y);

            if (s.showText) {
                tick.fontX = x + (boundingSize - textWidth) / 2;
                tick.fontY = y + (boundingSize + textHeight) / 2;
            }
            ticks.add(tick);

            maxBoundingSize = Math.max(maxBoundingSize, boundingSize);
            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }

        dimension.width = (maxCol + 1) * (maxBoundingSize + s.horizontalSpacing) - s.horizontalSpacing;
        dimension.height = (maxRow + 1) * (maxBoundingSize + s.verticalSpacing) - s.verticalSpacing;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(configChanged) {
            s = readSettings();
            calculateSizes(graphics);
        }

        if(s.exclusiveTab.getIndex() != -1 && client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != s.exclusiveTab.getIndex()) return null;
        if(ticks.size() < s.numberOfTicks - 1) return null;

        Font originalFont = graphics.getFont();
        graphics.setFont(graphics.getFont().deriveFont((float) s.textSize));

        for (int i = 0; i < s.numberOfTicks; i++)
        {
            Tick tick = ticks.get(i);
            if (s.showShape) {
                graphics.setColor(i == getCurrentTick() ? s.currentTickColour : s.tickColour);
                switch(s.shape) {
                    case SQUARE:
                        graphics.fillRect(tick.shapeX, tick.shapeY, s.shapeSize, s.shapeSize);
                        break;
                    case CIRCLE:
                        graphics.fillOval(tick.shapeX, tick.shapeY, s.shapeSize, s.shapeSize);
                        break;
                    case ROUNDED_SQUARE:
                        graphics.fillRoundRect(tick.shapeX, tick.shapeY, s.shapeSize, s.shapeSize, s.arc, s.arc);
                        break;
                }
            }
            if (s.showText) {
                graphics.setColor(i == getCurrentTick() ? s.currentTextColour : s.textColour);
                graphics.drawString(String.valueOf(i + 1), tick.fontX, tick.fontY);
            }
        }

        graphics.setFont(originalFont);
        return dimension;
    }
}
