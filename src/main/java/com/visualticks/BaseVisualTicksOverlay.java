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

        // First pass: one cell shared by every tick, so all columns and rows sit on a
        // single pitch. Labels differ in width ("10" is wider than "9"), so sizing each
        // cell on its own label would break the grid. See issue #5.
        //
        // Width and height are tracked apart because only width varies with the label:
        // folding the widest label into the height too would push the rows away from
        // each other for a purely horizontal reason.
        int cellWidth = s.showShape ? s.shapeSize : 0;
        int cellHeight = cellWidth;
        if (s.showText) {
            cellHeight = Math.max(cellHeight, textHeight);
            for (int i = 0; i < s.numberOfTicks; i++) {
                cellWidth = Math.max(cellWidth, fm.stringWidth(String.valueOf(i + 1)));
            }
        }

        // Negative spacing legitimately tightens the layout, but below -cell it inverts the
        // pitch: each tick would sit left of / above the last, marching out of the reported
        // bounds and driving the Dimension negative. Clamp at total overlap. See issue #6.
        int horizontalSpacing = Math.max(s.horizontalSpacing, -cellWidth);
        int verticalSpacing = Math.max(s.verticalSpacing, -cellHeight);

        // The cell is sized for the widest label, so a corner-anchored shape would sit
        // adrift from the number it carries. Centre it in the cell the label centres in.
        int shapeInsetX = s.showShape ? (cellWidth - s.shapeSize) / 2 : 0;
        int shapeInsetY = s.showShape ? (cellHeight - s.shapeSize) / 2 : 0;

        int maxCol = 0;
        int maxRow = 0;

        for (int i = 0; i < s.numberOfTicks; i++)
        {
            int row = i / s.amountPerRow;
            int col = i % s.amountPerRow;
            int x = col * (cellWidth + horizontalSpacing);
            int y = row * (cellHeight + verticalSpacing);

            Tick tick = new Tick(x + shapeInsetX, y + shapeInsetY);

            if (s.showText) {
                int textWidth = fm.stringWidth(String.valueOf(i + 1));
                tick.fontX = x + (cellWidth - textWidth) / 2;
                tick.fontY = y + (cellHeight + textHeight) / 2;
            }
            ticks.add(tick);

            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }

        dimension.width = (maxCol + 1) * (cellWidth + horizontalSpacing) - horizontalSpacing;
        dimension.height = (maxRow + 1) * (cellHeight + verticalSpacing) - verticalSpacing;

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
