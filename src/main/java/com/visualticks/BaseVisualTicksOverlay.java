package com.visualticks;

import com.google.inject.Inject;
import com.visualticks.config.Tick;
import com.visualticks.config.TickSettings;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
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
    protected volatile boolean configChanged = true;
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
        ticks.clear();

        Font originalFont = g.getFont();
        g.setFont(originalFont.deriveFont((float) s.textSize));
        FontMetrics fm = g.getFontMetrics();

        int textHeight = fm.getAscent();

        // First pass: one cell size shared by every tick. Labels differ in width
        // ("10" is wider than "9"), so sizing each cell on its own label would give
        // each column its own pitch and break the row. See issue #5.
        int cellSize = s.showShape ? s.shapeSize : 0;
        if (s.showText) {
            cellSize = Math.max(cellSize, textHeight);
            for (int i = 0; i < s.numberOfTicks; i++) {
                cellSize = Math.max(cellSize, fm.stringWidth(String.valueOf(i + 1)));
            }
        }

        int maxCol = 0;
        int maxRow = 0;

        for (int i = 0; i < s.numberOfTicks; i++)
        {
            int row = i / s.amountPerRow;
            int col = i % s.amountPerRow;
            int x = col * (cellSize + s.horizontalSpacing);
            int y = row * (cellSize + s.verticalSpacing);

            Tick tick = new Tick(x, y);

            if (s.showText) {
                int textWidth = fm.stringWidth(String.valueOf(i + 1));
                tick.fontX = x + (cellSize - textWidth) / 2;
                tick.fontY = y + (cellSize + textHeight) / 2;
            }
            ticks.add(tick);

            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }

        dimension.width = (maxCol + 1) * (cellSize + s.horizontalSpacing) - s.horizontalSpacing;
        dimension.height = (maxRow + 1) * (cellSize + s.verticalSpacing) - s.verticalSpacing;

        g.setFont(originalFont);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(configChanged) {
            configChanged = false;
            try {
                s = readSettings();
                calculateSizes(graphics);
            } catch (RuntimeException ex) {
                // Retry next frame rather than latching a transient failure forever.
                configChanged = true;
                throw ex;
            }
        }

        if(s.exclusiveTab.getIndex() != -1 && client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) != s.exclusiveTab.getIndex()) return null;
        if(ticks.size() < s.numberOfTicks) return null;

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
