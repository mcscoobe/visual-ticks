package com.visualticks;

import com.visualticks.config.InterfaceTab;
import com.visualticks.config.Tick;
import com.visualticks.config.TickSettings;
import com.visualticks.config.TickShape;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseVisualTicksOverlayTest
{
	private static final int TEXT_WIDTH = 8;
	private static final int TEXT_ASCENT = 9;

	@Mock
	private Client client;
	@Mock
	private Graphics2D graphics;
	@Mock
	private FontMetrics fontMetrics;

	@Before
	public void setUp()
	{
		when(graphics.getFont()).thenReturn(new Font("Dialog", Font.PLAIN, 12));
		when(graphics.getFontMetrics()).thenReturn(fontMetrics);
		when(fontMetrics.stringWidth(anyString())).thenReturn(TEXT_WIDTH);
		when(fontMetrics.getAscent()).thenReturn(TEXT_ASCENT);
	}

	/** Shapes only: cell size is the shape size, so the grid maths is exact. */
	@Test
	public void laysTicksOutInRowsOfAmountPerRow()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 5;
		s.amountPerRow = 3;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;
		TestOverlay overlay = overlay(s);

		overlay.render(graphics);

		assertEquals(5, overlay.ticks.size());
		assertPosition(overlay.ticks.get(0), 0, 0);
		assertPosition(overlay.ticks.get(1), 15, 0);
		assertPosition(overlay.ticks.get(2), 30, 0);
		assertPosition(overlay.ticks.get(3), 0, 15);
		assertPosition(overlay.ticks.get(4), 15, 15);
	}

	@Test
	public void reportedSizeExcludesTrailingSpacing()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 5;
		s.amountPerRow = 3;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;

		Dimension size = overlay(s).render(graphics);

		// 3 columns * 15 - 5 trailing, 2 rows * 15 - 5 trailing
		assertEquals(new Dimension(40, 25), size);
	}

	@Test
	public void singleRowHasNoVerticalExtent()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 4;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;

		Dimension size = overlay(s).render(graphics);

		assertEquals(new Dimension(4 * 15 - 5, 10), size);
	}

	@Test
	public void textIsCentredInsideTheShapeCell()
	{
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 2;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		TestOverlay overlay = overlay(s);

		overlay.render(graphics);

		// cell is max(shapeSize 10, textWidth 8, ascent 9) = 10
		Tick first = overlay.ticks.get(0);
		assertEquals((10 - TEXT_WIDTH) / 2, first.fontX);
		assertEquals((10 + TEXT_ASCENT) / 2, first.fontY);
		assertEquals(15 + (10 - TEXT_WIDTH) / 2, overlay.ticks.get(1).fontX);
	}

	@Test
	public void textLargerThanShapeGrowsTheCell()
	{
		when(fontMetrics.stringWidth(anyString())).thenReturn(20);
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 2;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;

		Dimension size = overlay(s).render(graphics);

		// cell grows to the 20px text width rather than clipping it
		assertEquals(2 * (20 + 5) - 5, size.width);
	}

	/**
	 * Real labels are not all the same width: "10" is wider than "9". The column pitch
	 * comes from each tick's own cell while the reported width comes from the widest
	 * cell, so a two-digit label shifts only itself and leaves a gap. This pins the
	 * current behaviour; see issue #5. Update the expectations when that is fixed.
	 */
	@Test
	public void twoDigitLabelsShiftOnlyTheirOwnColumn()
	{
		when(fontMetrics.stringWidth(anyString()))
			.thenAnswer(invocation -> TEXT_WIDTH * invocation.getArgument(0, String.class).length());
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 10;
		s.amountPerRow = 10;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		TestOverlay overlay = overlay(s);

		Dimension size = overlay.render(graphics);

		// "1".."9" measure 8, so they get 10px cells at a 15px pitch
		assertEquals(8 * 15, overlay.ticks.get(8).shapeX);
		// "10" measures 16, so this one tick alone uses a 21px pitch
		assertEquals(9 * 21, overlay.ticks.get(9).shapeX);
		// yet the reported width assumes every column used the widest cell
		assertEquals(10 * (16 + 5) - 5, size.width);
	}

	@Test
	public void shapeChoiceSelectsTheDrawingCall()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 2;
		s.shape = TickShape.CIRCLE;

		overlay(s).render(graphics);

		verify(graphics, times(2)).fillOval(anyInt(), anyInt(), eq(s.shapeSize), eq(s.shapeSize));
		verify(graphics, never()).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
	}

	@Test
	public void roundedSquareUsesTheConfiguredArc()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 1;
		s.shape = TickShape.ROUNDED_SQUARE;
		s.arc = 7;

		overlay(s).render(graphics);

		verify(graphics).fillRoundRect(0, 0, s.shapeSize, s.shapeSize, 7, 7);
	}

	@Test
	public void onlyTheCurrentTickUsesTheHighlightColour()
	{
		TickSettings s = shapeSettings();
		s.numberOfTicks = 3;
		s.tickColour = Color.GRAY;
		s.currentTickColour = Color.GREEN;
		TestOverlay overlay = overlay(s);
		overlay.current = 1;

		overlay.render(graphics);

		verify(graphics, times(1)).setColor(Color.GREEN);
		verify(graphics, times(2)).setColor(Color.GRAY);
	}

	@Test
	public void textIsSkippedWhenDisabled()
	{
		TickSettings s = shapeSettings();
		s.showText = false;
		s.numberOfTicks = 2;

		overlay(s).render(graphics);

		verify(graphics, never()).drawString(anyString(), anyInt(), anyInt());
	}

	@Test
	public void hiddenOnOtherTabsWhenTabIsExclusive()
	{
		TickSettings s = shapeSettings();
		s.exclusiveTab = InterfaceTab.INVENTORY;
		when(client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL)).thenReturn(InterfaceTab.PRAYER.getIndex());

		assertNull(overlay(s).render(graphics));
	}

	@Test
	public void shownOnTheMatchingTab()
	{
		TickSettings s = shapeSettings();
		s.exclusiveTab = InterfaceTab.INVENTORY;
		when(client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL)).thenReturn(InterfaceTab.INVENTORY.getIndex());

		assertTrue(overlay(s).render(graphics).width > 0);
	}

	@Test
	public void allTabNeverConsultsTheOpenTab()
	{
		TickSettings s = shapeSettings();
		s.exclusiveTab = InterfaceTab.ALL;

		assertTrue(overlay(s).render(graphics).width > 0);
		verify(client, never()).getVarcIntValue(VarClientID.TOPLEVEL_PANEL);
	}

	@Test
	public void settingsAreCachedUntilConfigChanges()
	{
		TestOverlay overlay = overlay(shapeSettings());

		overlay.render(graphics);
		overlay.render(graphics);
		assertEquals(1, overlay.reads);

		overlay.onConfigChanged();
		overlay.render(graphics);
		assertEquals(2, overlay.reads);
	}

	/** A failed read must not latch - the next frame has to try again. */
	@Test
	public void failedSettingsReadIsRetriedOnTheNextFrame()
	{
		TestOverlay overlay = overlay(shapeSettings());
		overlay.failNextRead = true;

		try
		{
			overlay.render(graphics);
			fail("expected the read failure to propagate");
		}
		catch (IllegalStateException expected)
		{
			// expected
		}

		assertTrue(overlay.render(graphics).width > 0);
		assertEquals(2, overlay.reads);
	}

	private static void assertPosition(Tick tick, int x, int y)
	{
		assertEquals(x, tick.shapeX);
		assertEquals(y, tick.shapeY);
	}

	private TestOverlay overlay(TickSettings settings)
	{
		return new TestOverlay(client, settings);
	}

	private static TickSettings shapeSettings()
	{
		TickSettings s = new TickSettings();
		s.showShape = true;
		s.showText = false;
		s.shape = TickShape.SQUARE;
		s.exclusiveTab = InterfaceTab.ALL;
		s.numberOfTicks = 2;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.arc = 10;
		s.textSize = 12;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;
		s.tickColour = Color.GRAY;
		s.currentTickColour = Color.GREEN;
		s.textColour = Color.WHITE;
		s.currentTextColour = Color.YELLOW;
		return s;
	}

	private static class TestOverlay extends BaseVisualTicksOverlay
	{
		private final TickSettings settings;
		int current;
		int reads;
		boolean failNextRead;

		TestOverlay(Client client, TickSettings settings)
		{
			super(null, null, client);
			this.settings = settings;
		}

		@Override
		protected TickSettings readSettings()
		{
			reads++;
			if (failNextRead)
			{
				failNextRead = false;
				throw new IllegalStateException("config unavailable");
			}
			return settings;
		}

		@Override
		protected int getCurrentTick()
		{
			return current;
		}
	}
}
