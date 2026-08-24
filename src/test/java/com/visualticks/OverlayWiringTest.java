package com.visualticks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Each overlay subclass hard-codes a tick index and a settings factory. A copy-paste
 * slip there points an overlay at another counter's state, which nothing else catches.
 */
@RunWith(MockitoJUnitRunner.class)
public class OverlayWiringTest
{
	@Mock
	private VisualTicksConfig config;

	private final VisualTicksPlugin plugin = new VisualTicksPlugin();

	@Before
	public void setUp()
	{
		plugin.ticks[0] = 5;
		plugin.ticks[1] = 6;
		plugin.ticks[2] = 7;
	}

	@Test
	public void eachOverlayTracksItsOwnCounter()
	{
		assertEquals(5, new VisualTicksOverlayOne(plugin, config, null).getCurrentTick());
		assertEquals(6, new VisualTicksOverlayTwo(plugin, config, null).getCurrentTick());
		assertEquals(7, new VisualTicksOverlayThree(plugin, config, null).getCurrentTick());
	}

	@Test
	public void eachOverlayReadsItsOwnSettings()
	{
		when(config.numberOfTicksOne()).thenReturn(11);
		when(config.numberOfTicksTwo()).thenReturn(22);
		when(config.numberOfTicksThree()).thenReturn(33);

		assertEquals(11, new VisualTicksOverlayOne(plugin, config, null).readSettings().numberOfTicks);
		assertEquals(22, new VisualTicksOverlayTwo(plugin, config, null).readSettings().numberOfTicks);
		assertEquals(33, new VisualTicksOverlayThree(plugin, config, null).readSettings().numberOfTicks);
	}
}
