package com.visualticks;

import com.google.inject.Inject;
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

/**
 * Lays out and draws one tick set. Subclasses supply only which counter and which
 * settings factory they use.
 *
 * <p>Subclass names are persisted: RuneLite's {@code Overlay.getName()} returns the simple
 * class name, and the overlay manager saves each overlay's dragged screen position under
 * keys prefixed with it. Renaming {@code VisualTicksOverlayOne/Two/Three} silently resets
 * every user's layout. {@code OverlayWiringTest} pins the names.
 */
public abstract class BaseVisualTicksOverlay extends Overlay
{
    protected VisualTicksPlugin plugin;
    protected VisualTicksConfig config;
    protected Client client;
    protected volatile boolean configChanged = true;
    protected TickSettings settings;
    protected final List<TickPosition> positions = new ArrayList<>();
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
        positions.clear();

        // Nothing to draw, so claim no space: the maths below seeds maxCol/maxRow at 0 and
        // would report a phantom one-cell box for OverlayManager to reserve and snap to.
        if (settings.numberOfTicks <= 0) {
            dimension.setSize(0, 0);
            return;
        }

        Font originalFont = g.getFont();
        g.setFont(originalFont.deriveFont((float) settings.textSize));
        FontMetrics fm = g.getFontMetrics();

        int textHeight = fm.getAscent();

        // First pass: one cell shared by every tick, so all columns and rows sit on a
        // single pitch. Labels differ in width ("10" is wider than "9"), so sizing each
        // cell on its own label would break the grid. See issue #5.
        //
        // Width and height are tracked apart because only width varies with the label:
        // folding the widest label into the height too would push the rows away from
        // each other for a purely horizontal reason.
        int cellWidth = settings.showShape ? settings.shapeSize : 0;
        int cellHeight = cellWidth;
        if (settings.showText) {
            cellHeight = Math.max(cellHeight, textHeight);
            for (int i = 0; i < settings.numberOfTicks; i++) {
                cellWidth = Math.max(cellWidth, fm.stringWidth(String.valueOf(i + 1)));
            }
        }

        // Negative spacing legitimately tightens the layout, but below -cell it inverts the
        // pitch: each tick would sit left of / above the last, marching out of the reported
        // bounds and driving the Dimension negative. Clamp at total overlap. See issue #6.
        int horizontalSpacing = Math.max(settings.horizontalSpacing, -cellWidth);
        int verticalSpacing = Math.max(settings.verticalSpacing, -cellHeight);

        // A stored config value can sit outside its @Range — ConfigManager returns it
        // unvalidated — so amountPerRow may be 0 and divide by zero below. See issue #7.
        int amountPerRow = Math.max(1, settings.amountPerRow);

        // The cell is sized for the widest label, so a corner-anchored shape would sit
        // adrift from the number it carries. Centre it in the cell the label centres in.
        int shapeInsetX = settings.showShape ? (cellWidth - settings.shapeSize) / 2 : 0;
        int shapeInsetY = settings.showShape ? (cellHeight - settings.shapeSize) / 2 : 0;

        int maxCol = 0;
        int maxRow = 0;

        for (int i = 0; i < settings.numberOfTicks; i++)
        {
            int row = i / amountPerRow;
            int col = i % amountPerRow;
            int x = col * (cellWidth + horizontalSpacing);
            int y = row * (cellHeight + verticalSpacing);

            TickPosition tick = new TickPosition(x + shapeInsetX, y + shapeInsetY);

            if (settings.showText) {
                int textWidth = fm.stringWidth(String.valueOf(i + 1));
                tick.fontX = x + (cellWidth - textWidth) / 2;
                tick.fontY = y + (cellHeight + textHeight) / 2;
            }
            positions.add(tick);

            maxRow = Math.max(maxRow, row);
            maxCol = Math.max(maxCol, col);
        }

        // n cells have n - 1 gaps between them: drop the spacing after the last column and
        // row so the reported box ends where the drawing does.
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
                settings = readSettings();
                calculateSizes(graphics);
            } catch (RuntimeException ex) {
                // Retry next frame rather than latching a transient failure forever.
                configChanged = true;
                throw ex;
            }
        }

        if(settings.exclusiveTab.getIndex() != -1 && client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) != settings.exclusiveTab.getIndex()) return null;

        Font originalFont = graphics.getFont();
        graphics.setFont(graphics.getFont().deriveFont((float) settings.textSize));

        for (int i = 0; i < settings.numberOfTicks; i++)
        {
            TickPosition tick = positions.get(i);
            if (settings.showShape) {
                graphics.setColor(i == getCurrentTick() ? settings.currentTickColour : settings.tickColour);
                switch(settings.shape) {
                    case SQUARE:
                        graphics.fillRect(tick.shapeX, tick.shapeY, settings.shapeSize, settings.shapeSize);
                        break;
                    case CIRCLE:
                        graphics.fillOval(tick.shapeX, tick.shapeY, settings.shapeSize, settings.shapeSize);
                        break;
                    case ROUNDED_SQUARE:
                        graphics.fillRoundRect(tick.shapeX, tick.shapeY, settings.shapeSize, settings.shapeSize, settings.arc, settings.arc);
                        break;
                }
            }
            if (settings.showText) {
                graphics.setColor(i == getCurrentTick() ? settings.currentTextColour : settings.textColour);
                graphics.drawString(String.valueOf(i + 1), tick.fontX, tick.fontY);
            }
        }

        graphics.setFont(originalFont);
        return dimension;
    }
}
