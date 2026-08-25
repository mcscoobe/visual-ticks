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
	 * Real labels are not all the same width: "10" is wider than "9". Every column
	 * still has to sit on one pitch - the widest cell - so the row stays aligned and
	 * the reported width matches what is drawn. Regression test for issue #5.
	 */
	@Test
	public void allColumnsShareThePitchOfTheWidestLabel()
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

		// "10" measures 16, the widest label, so every cell is 16 at a 21px pitch,
		// with the 10px shape centred in it (inset 3)
		for (int i = 0; i < 10; i++)
		{
			assertEquals(i * 21 + 3, overlay.ticks.get(i).shapeX);
		}
		// and the reported width is that same pitch, so it matches the drawn content
		assertEquals(10 * 21 - 5, size.width);
	}

	/** Each label is centred in the shared cell, so narrow labels get a wider inset. */
	@Test
	public void narrowerLabelsAreCentredInTheSharedCell()
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

		overlay.render(graphics);

		// cell is 16 wide: "1" (8 wide) insets by 4, "10" (16 wide) fills it
		assertEquals((16 - 8) / 2, overlay.ticks.get(0).fontX);
		assertEquals(9 * 21, overlay.ticks.get(9).fontX);
		// but only 10 tall - the baseline follows the height, not the label width
		assertEquals((10 + TEXT_ASCENT) / 2, overlay.ticks.get(0).fontY);
	}

	/**
	 * The uniform pitch has to hold down the y-axis too: a wide label in a later row
	 * must not push its row out. Rows and columns share one cell, so both pitches and
	 * the reported height agree. Regression test for issue #5.
	 */
	@Test
	public void rowsShareOnePitchWhenLabelsDiffer()
	{
		when(fontMetrics.stringWidth(anyString()))
			.thenAnswer(invocation -> TEXT_WIDTH * invocation.getArgument(0, String.class).length());
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 12;
		s.amountPerRow = 5;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;
		TestOverlay overlay = overlay(s);

		Dimension size = overlay.render(graphics);

		// widest label is "10".."12" at 16, so cells are 16 wide (21px column pitch,
		// shape inset 3) and 10 tall (15px row pitch)
		assertPosition(overlay.ticks.get(0), 3, 0);
		assertPosition(overlay.ticks.get(4), 4 * 21 + 3, 0);
		// row 2 starts back at the same x even though it holds the two-digit labels
		assertPosition(overlay.ticks.get(5), 3, 15);
		assertPosition(overlay.ticks.get(9), 4 * 21 + 3, 15);
		assertPosition(overlay.ticks.get(11), 21 + 3, 2 * 15);
		assertEquals(new Dimension(5 * 21 - 5, 3 * 15 - 5), size);
	}

	/** Text-only: no shape to seed the cell, so the labels alone set the pitch. */
	@Test
	public void textOnlyCellsAreSizedByTheLabels()
	{
		when(fontMetrics.stringWidth(anyString()))
			.thenAnswer(invocation -> TEXT_WIDTH * invocation.getArgument(0, String.class).length());
		TickSettings s = shapeSettings();
		s.showShape = false;
		s.showText = true;
		s.numberOfTicks = 10;
		s.amountPerRow = 10;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		TestOverlay overlay = overlay(s);

		Dimension size = overlay.render(graphics);

		// cell is the widest label (16), not the shape size, and still uniform
		assertEquals((16 - 8) / 2, overlay.ticks.get(0).fontX);
		assertEquals(9 * 21, overlay.ticks.get(9).fontX);
		assertEquals(10 * 21 - 5, size.width);
		// nothing but the ascent sets the height when there is no shape
		assertEquals(TEXT_ASCENT, size.height);
		verify(graphics, never()).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
		verify(graphics, never()).fillOval(anyInt(), anyInt(), anyInt(), anyInt());
	}

	/** A tall font drives the cell when it out-measures both the shape and the labels. */
	@Test
	public void ascentDrivesTheCellWhenItIsTallest()
	{
		when(fontMetrics.getAscent()).thenReturn(30);
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 2;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		TestOverlay overlay = overlay(s);

		Dimension size = overlay.render(graphics);

		// height is max(shape 10, ascent 30) = 30; width stays max(shape 10, label 8) = 10
		assertEquals(new Dimension(2 * 15 - 5, 30), size);
		// the tall cell centres the shape vertically rather than stranding it at the top
		assertPosition(overlay.ticks.get(1), 15, (30 - 10) / 2);
		assertEquals((10 - TEXT_WIDTH) / 2, overlay.ticks.get(0).fontX);
		assertEquals((30 + 30) / 2, overlay.ticks.get(0).fontY);
	}

	/**
	 * The first pass has to be a true max over every label, not a peek at the last one -
	 * digit count usually makes the last label widest, which would hide the shortcut.
	 */
	@Test
	public void widestLabelCountsWhereverItFalls()
	{
		when(fontMetrics.stringWidth(anyString()))
			.thenAnswer(invocation -> "2".equals(invocation.getArgument(0, String.class)) ? 40 : TEXT_WIDTH);
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = 3;
		s.amountPerRow = 8;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		TestOverlay overlay = overlay(s);

		Dimension size = overlay.render(graphics);

		// the 40px "2" sits in the middle, yet every cell is 40 wide at a 45px pitch,
		// with the 10px shape centred in it (inset 15)
		assertEquals(45 + 15, overlay.ticks.get(1).shapeX);
		assertEquals(2 * 45 + 15, overlay.ticks.get(2).shapeX);
		assertEquals(3 * 45 - 5, size.width);
	}

	/**
	 * The cell is sized for the widest label, so a corner-anchored shape drifts away
	 * from the number it carries - the wider the widest label, the further. Shape and
	 * label must stay concentric.
	 */
	@Test
	public void shapeAndLabelShareTheSameCentre()
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

		overlay.render(graphics);

		for (int i = 0; i < 10; i++)
		{
			Tick tick = overlay.ticks.get(i);
			int labelWidth = TEXT_WIDTH * String.valueOf(i + 1).length();
			assertEquals(
				"tick " + (i + 1) + " label is off-centre from its shape",
				tick.shapeX + s.shapeSize / 2,
				tick.fontX + labelWidth / 2);
			// and the label sits within the shape vertically rather than hanging below it
			assertTrue(tick.fontY - TEXT_ASCENT >= tick.shapeY);
			assertTrue(tick.fontY <= tick.shapeY + s.shapeSize);
		}
	}

	/**
	 * Label width is a horizontal measurement: it may widen the columns, but it must
	 * not push the rows apart or grow the reported height.
	 */
	@Test
	public void labelWidthDoesNotAffectRowPitch()
	{
		when(fontMetrics.stringWidth(anyString()))
			.thenAnswer(invocation -> TEXT_WIDTH * invocation.getArgument(0, String.class).length());

		// nine ticks: every label is one digit
		Dimension singleDigit = overlay(varyingLabelSettings(9)).render(graphics);
		// ten ticks: "10" is twice as wide as any label before it
		TestOverlay wide = overlay(varyingLabelSettings(10));
		Dimension twoDigit = wide.render(graphics);

		// the extra width lands on the columns only
		assertEquals(5 * 15 - 5, singleDigit.width);
		assertEquals(5 * 21 - 5, twoDigit.width);
		// while the row pitch and the height are untouched
		assertEquals(singleDigit.height, twoDigit.height);
		assertEquals(2 * 15 - 5, twoDigit.height);
		assertEquals(15, wide.ticks.get(5).shapeY);
	}

	private static TickSettings varyingLabelSettings(int numberOfTicks)
	{
		TickSettings s = shapeSettings();
		s.showText = true;
		s.numberOfTicks = numberOfTicks;
		s.amountPerRow = 5;
		s.shapeSize = 10;
		s.horizontalSpacing = 5;
		s.verticalSpacing = 5;
		return s;
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
