package com.visualticks;

import com.visualticks.config.HotkeyMode;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Canvas;
import java.awt.event.KeyEvent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class VisualTicksPluginLogicTest
{
	@Mock
	private Keybind resetHotkey;
	@Mock
	private Keybind globalIncrease;
	@Mock
	private Keybind globalDecrease;
	@Mock
	private Keybind increaseOne;
	@Mock
	private Keybind decreaseOne;
	@Mock
	private Keybind increaseTwo;
	@Mock
	private Keybind decreaseTwo;
	@Mock
	private Keybind increaseThree;
	@Mock
	private Keybind decreaseThree;
	@Mock
	private Client client;
	@Mock
	private VisualTicksConfig config;
	@Mock
	private OverlayManager overlayManager;
	@Mock
	private VisualTicksOverlayOne overlayOne;
	@Mock
	private VisualTicksOverlayTwo overlayTwo;
	@Mock
	private VisualTicksOverlayThree overlayThree;
	@Mock
	private KeyManager keyManager;
	@Mock
	private ConfigManager configManager;
	@Mock
	private ClientThread clientThread;

	@InjectMocks
	private VisualTicksPlugin plugin;

	private EventBus eventBus;

	@Before
	public void setUp()
	{
		// The default bus swallows subscriber exceptions, which would make the
		// negative assertions below pass vacuously.
		eventBus = new EventBus(e ->
		{
			throw new AssertionError("subscriber threw", e);
		});
		eventBus.register(plugin);

		when(config.isEnabledOne()).thenReturn(true);
		when(config.isEnabledTwo()).thenReturn(true);
		when(config.isEnabledThree()).thenReturn(true);
		when(config.numberOfTicksOne()).thenReturn(2);
		when(config.numberOfTicksTwo()).thenReturn(3);
		when(config.numberOfTicksThree()).thenReturn(4);
		when(config.tickResetHotkey()).thenReturn(resetHotkey);
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.GLOBAL);
		when(config.tickIncreaseHotkey()).thenReturn(globalIncrease);
		when(config.tickDecreaseHotkey()).thenReturn(globalDecrease);
		when(config.increaseHotkeyOne()).thenReturn(increaseOne);
		when(config.decreaseHotkeyOne()).thenReturn(decreaseOne);
		when(config.increaseHotkeyTwo()).thenReturn(increaseTwo);
		when(config.decreaseHotkeyTwo()).thenReturn(decreaseTwo);
		when(config.increaseHotkeyThree()).thenReturn(increaseThree);
		when(config.decreaseHotkeyThree()).thenReturn(decreaseThree);

		// Run scheduled work inline so hotkey handling is observable.
		doAnswer(invocation ->
		{
			invocation.getArgument(0, Runnable.class).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
	}

	private void gameTicks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			eventBus.post(new GameTick());
		}
	}

	@Test
	public void eachCounterAdvancesOnGameTick()
	{
		gameTicks(1);

		assertArrayEquals(new int[]{1, 1, 1}, plugin.ticks);
	}

	@Test
	public void countersWrapAtTheirOwnLimit()
	{
		gameTicks(4);

		// 4 % 2 == 0, 4 % 3 == 1, 4 % 4 == 0
		assertArrayEquals(new int[]{0, 1, 0}, plugin.ticks);
	}

	/**
	 * A stored tick count of 0 is out of @Range but reachable from an edited or synced
	 * profile, and used to throw ArithmeticException every tick. The bus here rethrows
	 * subscriber exceptions, so the assertions below only run if nothing threw.
	 * Regression test for issue #7.
	 */
	@Test
	public void zeroTickCountDoesNotDivideByZero()
	{
		when(config.numberOfTicksOne()).thenReturn(0);

		gameTicks(3);

		assertArrayEquals(new int[]{0, 0, 3}, plugin.ticks);
	}

	@Test
	public void disabledCountersDoNotAdvance()
	{
		when(config.isEnabledTwo()).thenReturn(false);

		gameTicks(3);

		assertArrayEquals(new int[]{1, 0, 3}, plugin.ticks);
	}

	@Test
	public void startUpAddsOnlyEnabledOverlays() throws Exception
	{
		when(config.isEnabledTwo()).thenReturn(false);

		plugin.startUp();

		verify(overlayManager, never()).add(overlayTwo);
		verify(overlayManager).add(overlayThree);

		// Each overlay is removed before being re-added; the reverse order would
		// add then immediately drop every overlay and render nothing.
		InOrder inOrder = inOrder(overlayManager);
		inOrder.verify(overlayManager).remove(overlayOne);
		inOrder.verify(overlayManager).add(overlayOne);
		verify(overlayManager).remove(overlayTwo);
		verify(overlayOne).onConfigChanged();
		verify(overlayTwo).onConfigChanged();
		verify(overlayThree).onConfigChanged();
		verify(keyManager).registerKeyListener(plugin);
	}

	@Test
	public void configChangeInOwnGroupRefreshesOverlays()
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup(VisualTicksConfig.GROUP_NAME);

		eventBus.post(event);

		verify(overlayManager).add(overlayOne);
		verify(overlayOne).onConfigChanged();
	}

	@Test
	public void configChangeInOtherGroupIsIgnored()
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup("someotherplugin");

		eventBus.post(event);

		verifyNoInteractions(overlayManager);
		verifyNoInteractions(overlayOne);
	}

	@Test
	public void resetHotkeyClearsEveryCounter()
	{
		when(resetHotkey.matches(any(KeyEvent.class))).thenReturn(true);
		gameTicks(1);

		plugin.keyPressed(keyEvent());

		assertArrayEquals(new int[]{0, 0, 0}, plugin.ticks);
	}

	/** The counters are only safe to touch on the client thread. */
	@Test
	public void resetIsHandedToTheClientThread()
	{
		when(resetHotkey.matches(any(KeyEvent.class))).thenReturn(true);

		plugin.keyPressed(keyEvent());

		verify(clientThread).invoke(any(Runnable.class));
	}

	@Test
	public void otherKeysLeaveCountersAlone()
	{
		when(resetHotkey.matches(any(KeyEvent.class))).thenReturn(false);
		gameTicks(1);

		plugin.keyPressed(keyEvent());

		assertArrayEquals(new int[]{1, 1, 1}, plugin.ticks);
		verify(clientThread, never()).invoke(any(Runnable.class));
	}

	@Test
	public void globalIncreaseAddsATickToEverySet()
	{
		press(globalIncrease);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 3);
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksTwo", 4);
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksThree", 5);
	}

	@Test
	public void globalDecreaseRemovesATick()
	{
		press(globalDecrease);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksTwo", 2);
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksThree", 3);
	}

	/** The count never leaves the range the settings slider allows. */
	@Test
	public void adjustmentStaysInsideTheSliderRange()
	{
		when(config.numberOfTicksOne()).thenReturn(30);
		when(config.numberOfTicksTwo()).thenReturn(45);

		press(globalIncrease);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 30);
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksTwo", 30);
	}

	@Test
	public void decreasingASetAtTwoHidesIt()
	{
		press(globalDecrease);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", false);
		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 1);
	}

	@Test
	public void increaseBringsBackWhatTheDecreaseHotkeyHid()
	{
		press(globalDecrease);
		when(config.isEnabledOne()).thenReturn(false);

		press(globalIncrease);

		// Already stored at 2, so enabling it is the whole job.
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	/** Otherwise one global increase switches on sets the user never wanted. */
	@Test
	public void increaseLeavesSetsTheUserDisabledAlone()
	{
		when(config.isEnabledTwo()).thenReturn(false);

		press(globalIncrease);

		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledTwo", true);
		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksTwo", 2);
	}

	@Test
	public void showingAHiddenSetFromTheConfigPanelEndsTheHotkeyClaimOnIt()
	{
		press(globalDecrease);

		// Re-enabled by hand, then disabled by hand: no longer the hotkey's to restore.
		when(config.isEnabledOne()).thenReturn(true);
		ConfigChanged event = new ConfigChanged();
		event.setGroup(VisualTicksConfig.GROUP_NAME);
		eventBus.post(event);
		when(config.isEnabledOne()).thenReturn(false);

		press(globalIncrease);

		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	@Test
	public void independentHotkeysTouchOnlyTheirOwnSet()
	{
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.INDEPENDENT);

		press(increaseTwo);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksTwo", 4);
		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 3);
		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksThree", 5);
	}

	/** A per-set hotkey names its target, so it revives it however it was disabled. */
	@Test
	public void independentIncreaseShowsItsOwnDisabledSet()
	{
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.INDEPENDENT);
		when(config.isEnabledOne()).thenReturn(false);

		press(increaseOne);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	@Test
	public void theGlobalHotkeysAreIgnoredInIndependentMode()
	{
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.INDEPENDENT);

		press(globalIncrease);

		verifyNoInteractions(configManager);
	}

	@Test
	public void independentHotkeysAreIgnoredInGlobalMode()
	{
		press(increaseTwo);

		verifyNoInteractions(configManager);
	}

	/** Keybind.matches already rejects NOT_SET; this pins that the plugin relies on it. */
	@Test
	public void unsetHotkeysChangeNothing()
	{
		when(config.tickIncreaseHotkey()).thenReturn(Keybind.NOT_SET);
		when(config.tickDecreaseHotkey()).thenReturn(Keybind.NOT_SET);

		plugin.keyPressed(new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED,
			System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, KeyEvent.CHAR_UNDEFINED));

		verifyNoInteractions(configManager);
	}

	/**
	 * KeyManager withholds key events only on the login screen, so a printable key bound
	 * to an adjustment would otherwise rewrite stored settings mid-sentence.
	 */
	@Test
	public void adjustmentsAreIgnoredWhileTyping()
	{
		when(client.getVarcStrValue(VarClientID.CHATINPUT)).thenReturn("hi there");

		press(globalIncrease);

		verifyNoInteractions(configManager);
	}

	@Test
	public void adjustmentsAreIgnoredWhileAnInputDialogIsOpen()
	{
		when(client.getVarcIntValue(VarClientID.MESLAYERMODE)).thenReturn(InputType.SEARCH.getType());

		press(globalIncrease);

		verifyNoInteractions(configManager);
	}

	/** Resetting a counter is harmless enough to survive a stray keystroke. */
	@Test
	public void theResetHotkeyStillWorksWhileTyping()
	{
		when(client.getVarcStrValue(VarClientID.CHATINPUT)).thenReturn("hi there");
		when(resetHotkey.matches(any(KeyEvent.class))).thenReturn(true);
		gameTicks(1);

		plugin.keyPressed(keyEvent());

		assertArrayEquals(new int[]{0, 0, 0}, plugin.ticks);
	}

	/** So a bound key doesn't both adjust the counter and type itself into the chatbox. */
	@Test
	public void aMatchedAdjustmentConsumesItsKeyEvent()
	{
		when(globalIncrease.matches(any(KeyEvent.class))).thenReturn(true);
		KeyEvent event = keyEvent();

		plugin.keyPressed(event);

		assertTrue(event.isConsumed());
	}

	@Test
	public void anUnmatchedKeyIsLeftAlone()
	{
		KeyEvent event = keyEvent();

		plugin.keyPressed(event);

		assertFalse(event.isConsumed());
	}

	/** A set sized by the user and switched off by hand keeps its size when it returns. */
	@Test
	public void revivingASetKeepsTheCountItWasConfiguredWith()
	{
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.INDEPENDENT);
		when(config.isEnabledOne()).thenReturn(false);
		when(config.numberOfTicksOne()).thenReturn(8);

		press(increaseOne);

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 2);
	}

	/** A stored count below the minimum is still normalised on the way back in. */
	@Test
	public void revivingASetNormalisesACountBelowTheMinimum()
	{
		when(config.tickAdjustHotkeyMode()).thenReturn(HotkeyMode.INDEPENDENT);
		when(config.isEnabledOne()).thenReturn(false);
		when(config.numberOfTicksOne()).thenReturn(0);

		press(increaseOne);

		// Count first, so the set never reappears at its stale size.
		InOrder inOrder = inOrder(configManager);
		inOrder.verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "numberOfTicksOne", 2);
		inOrder.verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	/** Profiles carry their own enabled flags, so a claim must not cross between them. */
	@Test
	public void switchingProfileDropsTheHotkeyClaim()
	{
		press(globalDecrease);
		when(config.isEnabledOne()).thenReturn(false);

		eventBus.post(new ProfileChanged());
		press(globalIncrease);

		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	@Test
	public void restartingDropsTheHotkeyClaim() throws Exception
	{
		press(globalDecrease);
		when(config.isEnabledOne()).thenReturn(false);

		plugin.startUp();
		press(globalIncrease);

		verify(configManager, never()).setConfiguration(VisualTicksConfig.GROUP_NAME, "isEnabledOne", true);
	}

	@Test
	public void shutDownRemovesEveryOverlayAndTheKeyListener() throws Exception
	{
		plugin.shutDown();

		verify(overlayManager).remove(overlayOne);
		verify(overlayManager).remove(overlayTwo);
		verify(overlayManager).remove(overlayThree);
		verify(keyManager).unregisterKeyListener(plugin);
	}

	@Test
	public void migrationSplitsLegacyPaddingIntoBothAxes() throws Exception
	{
		when(configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, "paddingBetweenTicksOne")).thenReturn("9");

		plugin.startUp();

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "horizontalSpacingOne", "9");
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "verticalSpacingOne", "9");
		verify(configManager).unsetConfiguration(VisualTicksConfig.GROUP_NAME, "paddingBetweenTicksOne");
	}

	@Test
	public void migrationCoversAllThreeLegacyKeys() throws Exception
	{
		when(configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, "tickPaddingTwo")).thenReturn("3");
		when(configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, "tickPaddingThree")).thenReturn("4");

		plugin.startUp();

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "horizontalSpacingTwo", "3");
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "verticalSpacingTwo", "3");
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "horizontalSpacingThree", "4");
		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "verticalSpacingThree", "4");
	}

	@Test
	public void migrationWithoutLegacyValuesWritesNothing() throws Exception
	{
		plugin.startUp();

		verify(configManager, never()).setConfiguration(any(String.class), any(String.class), any(String.class));
		verify(configManager).unsetConfiguration(VisualTicksConfig.GROUP_NAME, "paddingBetweenTicksOne");
		verify(configManager).unsetConfiguration(VisualTicksConfig.GROUP_NAME, "tickPaddingTwo");
		verify(configManager).unsetConfiguration(VisualTicksConfig.GROUP_NAME, "tickPaddingThree");
	}

	@Test
	public void profileChangeRerunsMigration()
	{
		reset(configManager);
		when(configManager.getConfiguration(VisualTicksConfig.GROUP_NAME, "paddingBetweenTicksOne")).thenReturn("7");

		eventBus.post(new ProfileChanged());

		verify(configManager).setConfiguration(VisualTicksConfig.GROUP_NAME, "horizontalSpacingOne", "7");
	}

	private void press(Keybind hotkey)
	{
		when(hotkey.matches(any(KeyEvent.class))).thenReturn(true);
		plugin.keyPressed(keyEvent());
		when(hotkey.matches(any(KeyEvent.class))).thenReturn(false);
	}

	private static KeyEvent keyEvent()
	{
		return new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_R, 'r');
	}
}
