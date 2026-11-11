package com.visualticks;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Rules about {@link VisualTicksConfig} that nothing else enforces. Every key name is
 * stored user data, and the plugin and its tests both lean on these conventions, so a
 * slip here resets settings or writes dead keys with every other test still passing.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigConventionsTest
{
	private static final String[] SUFFIXES = {"One", "Two", "Three"};
	private static final String[] SECTIONS = {
		VisualTicksConfig.tickSettings, VisualTicksConfig.tickSettingsTwo, VisualTicksConfig.tickSettingsThree
	};

	@Mock
	private VisualTicksConfig config;
	@Mock
	private ConfigManager configManager;
	@Mock
	private OverlayManager overlayManager;
	@Mock
	private KeyManager keyManager;
	@Mock
	private ClientThread clientThread;
	@Mock
	private Client client;
	@Mock
	private VisualTicksOverlayOne overlayOne;
	@Mock
	private VisualTicksOverlayTwo overlayTwo;
	@Mock
	private VisualTicksOverlayThree overlayThree;
	@Mock
	private Keybind increase;
	@Mock
	private Keybind decrease;
	@Mock
	private Keybind unmatched;

	@InjectMocks
	private VisualTicksPlugin plugin;

	@Test
	public void everyKeyNameMatchesItsMethodName()
	{
		for (Method method : configItems())
		{
			assertEquals("keyName must equal the method name; the plugin builds keys from method names",
				method.getName(), method.getAnnotation(ConfigItem.class).keyName());
		}
	}

	@Test
	public void everyTickSetDeclaresTheSameSettings()
	{
		Map<String, Set<String>> baseNames = new TreeMap<>();
		for (int i = 0; i < SECTIONS.length; i++)
		{
			baseNames.put(SUFFIXES[i], new TreeSet<>());
		}
		for (Method method : configItems())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			for (int i = 0; i < SECTIONS.length; i++)
			{
				if (item.section().equals(SECTIONS[i]))
				{
					assertTrue(item.keyName() + " sits in section " + SECTIONS[i] + " without its suffix",
						item.keyName().endsWith(SUFFIXES[i]));
					baseNames.get(SUFFIXES[i]).add(item.keyName().substring(0, item.keyName().length() - SUFFIXES[i].length()));
				}
			}
		}

		assertTrue("tick set One declares no settings", !baseNames.get("One").isEmpty());
		assertEquals("a setting added to set One must be added to set Two too", baseNames.get("One"), baseNames.get("Two"));
		assertEquals("a setting added to set One must be added to set Three too", baseNames.get("One"), baseNames.get("Three"));
	}

	@Test
	public void tickCountRangeIsTheDeclaredMinimumAndMaximum() throws Exception
	{
		for (String suffix : SUFFIXES)
		{
			Range range = VisualTicksConfig.class.getMethod("numberOfTicks" + suffix).getAnnotation(Range.class);
			assertEquals(VisualTicksConfig.MIN_TICKS, range.min());
			assertEquals(VisualTicksConfig.MAX_TICKS, range.max());
		}
		// Pinned so moving the constants can't quietly change the slider users see.
		assertEquals(2, VisualTicksConfig.MIN_TICKS);
		assertEquals(30, VisualTicksConfig.MAX_TICKS);
	}

	/**
	 * Drives every path that writes config by string: hide, revive with a normalised
	 * count, and the legacy padding migration. A typo in {@code "numberOfTicks" + suffix}
	 * would otherwise write a key nothing reads.
	 */
	@Test
	public void everyKeyThePluginWritesIsDeclared() throws Exception
	{
		when(config.tickResetHotkey()).thenReturn(unmatched);
		when(config.tickIncreaseHotkey()).thenReturn(increase);
		when(config.tickDecreaseHotkey()).thenReturn(decrease);
		when(config.tickAdjustHotkeyMode()).thenCallRealMethod();
		when(config.interfaceLayerOne()).thenCallRealMethod();
		when(config.interfaceLayerTwo()).thenCallRealMethod();
		when(config.interfaceLayerThree()).thenCallRealMethod();
		when(configManager.getConfiguration(any(String.class), any(String.class))).thenReturn("5");

		plugin.startUp();

		// Every set enabled at a count below the minimum: decrease hides all three...
		when(config.isEnabledOne()).thenReturn(true);
		when(config.isEnabledTwo()).thenReturn(true);
		when(config.isEnabledThree()).thenReturn(true);
		press(decrease);
		// ...and increase revives them, normalising the count first.
		when(config.isEnabledOne()).thenReturn(false);
		when(config.isEnabledTwo()).thenReturn(false);
		when(config.isEnabledThree()).thenReturn(false);
		press(increase);
		// ...then increase again on enabled sets inside the range, which takes the ordinary
		// step write rather than the revive write: the two build the count key separately.
		when(config.isEnabledOne()).thenReturn(true);
		when(config.isEnabledTwo()).thenReturn(true);
		when(config.isEnabledThree()).thenReturn(true);
		when(config.numberOfTicksOne()).thenReturn(5);
		when(config.numberOfTicksTwo()).thenReturn(5);
		when(config.numberOfTicksThree()).thenReturn(5);
		press(increase);

		Set<String> declared = new TreeSet<>();
		for (Method method : configItems())
		{
			declared.add(method.getAnnotation(ConfigItem.class).keyName());
		}
		Set<String> written = new TreeSet<>();
		for (Invocation invocation : Mockito.mockingDetails(configManager).getInvocations())
		{
			if (invocation.getMethod().getName().equals("setConfiguration"))
			{
				written.add(invocation.getArgument(1));
			}
		}

		assertEquals("expected isEnabled, numberOfTicks and both spacing keys for each set, got " + written,
			12, written.size());
		for (String key : written)
		{
			assertTrue("plugin writes undeclared config key " + key, declared.contains(key));
		}
	}

	private void press(Keybind hotkey)
	{
		when(hotkey.matches(any(KeyEvent.class))).thenReturn(true);
		plugin.keyPressed(new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A, 'a'));
		when(hotkey.matches(any(KeyEvent.class))).thenReturn(false);
	}

	private static Set<Method> configItems()
	{
		Set<Method> items = new HashSet<>();
		for (Method method : VisualTicksConfig.class.getMethods())
		{
			if (method.isAnnotationPresent(ConfigItem.class))
			{
				items.add(method);
			}
		}
		return items;
	}
}
