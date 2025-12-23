package com.visualticks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;
import org.junit.Before;
import net.runelite.api.Client;

import static org.junit.Assert.*;

public class VisualTicksPluginTest
{
	private VisualTicksPlugin plugin;
	private TestConfig testConfig;
	
	/**
	 * Simple test configuration implementation for testing overlay behavior.
	 */
	private static class TestConfig implements VisualTicksConfig {
		private int numberOfTicksOne = 5;
		private int numberOfTicksTwo = 3;
		private int numberOfTicksThree = 4;
		private int numberOfTicksHotkeys = 6;
		private boolean enabledOne = true;
		private boolean enabledTwo = true;
		private boolean enabledThree = true;
		private boolean enabledHotkeys = true;
		
		@Override public int numberOfTicksOne() { return numberOfTicksOne; }
		@Override public int numberOfTicksTwo() { return numberOfTicksTwo; }
		@Override public int numberOfTicksThree() { return numberOfTicksThree; }
		@Override public int numberOfTicksHotkeys() { return numberOfTicksHotkeys; }
		@Override public boolean isEnabledOne() { return enabledOne; }
		@Override public boolean isEnabledTwo() { return enabledTwo; }
		@Override public boolean isEnabledThree() { return enabledThree; }
		@Override public boolean isEnabledHotkeys() { return enabledHotkeys; }
		
		public void setEnabledOne(boolean enabled) { this.enabledOne = enabled; }
		public void setEnabledTwo(boolean enabled) { this.enabledTwo = enabled; }
		
		// Default implementations for other required methods
		@Override public net.runelite.client.config.Keybind tickResetHotkey() { return null; }
		@Override public net.runelite.client.config.Keybind tickIncrementHotkey() { return null; }
		@Override public net.runelite.client.config.Keybind tickDecrementHotkey() { return null; }
		@Override public boolean shouldShowTickShapeOne() { return true; }
		@Override public com.visualticks.config.TickShape tickShapeOne() { return com.visualticks.config.TickShape.CIRCLE; }
		@Override public com.visualticks.config.InterfaceTab exclusiveTabOne() { return com.visualticks.config.InterfaceTab.ALL; }
		@Override public int amountPerRowOne() { return 8; }
		@Override public java.awt.Color tickColourOne() { return java.awt.Color.BLUE; }
		@Override public java.awt.Color currentTickColourOne() { return java.awt.Color.WHITE; }
		@Override public int sizeOfTickShapesOne() { return 32; }
		@Override public int tickArcOne() { return 10; }
		@Override public boolean shouldShowTextOne() { return true; }
		@Override public int tickTextSizeOne() { return 15; }
		@Override public java.awt.Color tickTextColourOne() { return java.awt.Color.WHITE; }
		@Override public java.awt.Color currentTickTextColourOne() { return java.awt.Color.BLUE; }
		@Override public int horizontalSpacingOne() { return 5; }
		@Override public int verticalSpacingOne() { return 5; }
		@Override public boolean shouldShowTickShapeTwo() { return true; }
		@Override public com.visualticks.config.TickShape tickShapeTwo() { return com.visualticks.config.TickShape.CIRCLE; }
		@Override public com.visualticks.config.InterfaceTab exclusiveTabTwo() { return com.visualticks.config.InterfaceTab.ALL; }
		@Override public int amountPerRowTwo() { return 8; }
		@Override public java.awt.Color tickColourTwo() { return java.awt.Color.BLUE; }
		@Override public java.awt.Color currentTickColourTwo() { return java.awt.Color.WHITE; }
		@Override public int sizeOfTickShapesTwo() { return 32; }
		@Override public int tickArcTwo() { return 10; }
		@Override public boolean shouldShowTextTwo() { return true; }
		@Override public int tickTextSizeTwo() { return 15; }
		@Override public java.awt.Color tickTextColourTwo() { return java.awt.Color.WHITE; }
		@Override public java.awt.Color currentTickTextColourTwo() { return java.awt.Color.BLUE; }
		@Override public int horizontalSpacingTwo() { return 5; }
		@Override public int verticalSpacingTwo() { return 5; }
		@Override public boolean shouldShowTickShapeThree() { return true; }
		@Override public com.visualticks.config.TickShape tickShapeThree() { return com.visualticks.config.TickShape.CIRCLE; }
		@Override public com.visualticks.config.InterfaceTab exclusiveTabThree() { return com.visualticks.config.InterfaceTab.ALL; }
		@Override public int amountPerRowThree() { return 8; }
		@Override public java.awt.Color tickColourThree() { return java.awt.Color.BLUE; }
		@Override public java.awt.Color currentTickColourThree() { return java.awt.Color.WHITE; }
		@Override public int sizeOfTickShapesThree() { return 32; }
		@Override public int tickArcThree() { return 10; }
		@Override public boolean shouldShowTextThree() { return true; }
		@Override public int tickTextSizeThree() { return 15; }
		@Override public java.awt.Color tickTextColourThree() { return java.awt.Color.WHITE; }
		@Override public java.awt.Color currentTickTextColourThree() { return java.awt.Color.BLUE; }
		@Override public int horizontalSpacingThree() { return 5; }
		@Override public int verticalSpacingThree() { return 5; }
		@Override public boolean shouldShowTickShapeHotkeys() { return true; }
		@Override public com.visualticks.config.TickShape tickShapeHotkeys() { return com.visualticks.config.TickShape.CIRCLE; }
		@Override public com.visualticks.config.InterfaceTab exclusiveTabHotkeys() { return com.visualticks.config.InterfaceTab.ALL; }
		@Override public int amountPerRowHotkeys() { return 8; }
		@Override public java.awt.Color tickColourHotkeys() { return java.awt.Color.BLUE; }
		@Override public java.awt.Color currentTickColourHotkeys() { return java.awt.Color.WHITE; }
		@Override public int sizeOfTickShapesHotkeys() { return 32; }
		@Override public int tickArcHotkeys() { return 10; }
		@Override public boolean shouldShowTextHotkeys() { return true; }
		@Override public int tickTextSizeHotkeys() { return 15; }
		@Override public java.awt.Color tickTextColourHotkeys() { return java.awt.Color.WHITE; }
		@Override public java.awt.Color currentTickTextColourHotkeys() { return java.awt.Color.BLUE; }
		@Override public int horizontalSpacingHotkeys() { return 5; }
		@Override public int verticalSpacingHotkeys() { return 5; }
	}
	
	@Before
	public void setUp() {
		plugin = new VisualTicksPlugin();
		testConfig = new TestConfig();
		
		// Use reflection to inject test configuration
		try {
			java.lang.reflect.Field configField = VisualTicksPlugin.class.getDeclaredField("config");
			configField.setAccessible(true);
			configField.set(plugin, testConfig);
		} catch (Exception e) {
			throw new RuntimeException("Failed to inject test configuration", e);
		}
	}
	
	/**
	 * Test that overlay getCurrentTick() methods return updated values after tick manipulation.
	 * This verifies Requirements 1.5 and 2.5 - that visual overlays reflect tick changes.
	 */
	@Test
	public void testOverlayReflectsTickManipulation() throws Exception {
		// Create real overlay instances to test getCurrentTick() methods
		VisualTicksOverlayOne overlayOne = new VisualTicksOverlayOne(plugin, testConfig, null);
		VisualTicksOverlayTwo overlayTwo = new VisualTicksOverlayTwo(plugin, testConfig, null);
		VisualTicksOverlayThree overlayThree = new VisualTicksOverlayThree(plugin, testConfig, null);
		VisualTicksOverlayHotkeys overlayHotkeys = new VisualTicksOverlayHotkeys(plugin, testConfig, null);
		
		// Test initial state
		assertEquals("Initial tickOne should be 0", 0, plugin.tickOne);
		assertEquals("Initial tickTwo should be 0", 0, plugin.tickTwo);
		assertEquals("Initial tickThree should be 0", 0, plugin.tickThree);
		assertEquals("Initial tickHotkeys should be 0", 0, plugin.tickHotkeys);
		
		// Verify overlays return the same values as plugin fields
		assertEquals("OverlayOne getCurrentTick should match plugin.tickOne", plugin.tickOne, overlayOne.getCurrentTick());
		assertEquals("OverlayTwo getCurrentTick should match plugin.tickTwo", plugin.tickTwo, overlayTwo.getCurrentTick());
		assertEquals("OverlayThree getCurrentTick should match plugin.tickThree", plugin.tickThree, overlayThree.getCurrentTick());
		assertEquals("OverlayHotkeys getCurrentTick should match plugin.tickHotkeys", plugin.tickHotkeys, overlayHotkeys.getCurrentTick());
		
		// Use reflection to call private increment methods to simulate tick manipulation
		java.lang.reflect.Method incrementMethod = VisualTicksPlugin.class.getDeclaredMethod("incrementTick", int.class);
		incrementMethod.setAccessible(true);
		
		// Test increment operations
		incrementMethod.invoke(plugin, 1); // Increment configuration One
		assertEquals("After increment, tickOne should be 1", 1, plugin.tickOne);
		assertEquals("OverlayOne should reflect incremented value", 1, overlayOne.getCurrentTick());
		
		incrementMethod.invoke(plugin, 2); // Increment configuration Two
		assertEquals("After increment, tickTwo should be 1", 1, plugin.tickTwo);
		assertEquals("OverlayTwo should reflect incremented value", 1, overlayTwo.getCurrentTick());
		
		incrementMethod.invoke(plugin, 3); // Increment configuration Three
		assertEquals("After increment, tickThree should be 1", 1, plugin.tickThree);
		assertEquals("OverlayThree should reflect incremented value", 1, overlayThree.getCurrentTick());
		
		incrementMethod.invoke(plugin, 4); // Increment configuration Hotkeys
		assertEquals("After increment, tickHotkeys should be 1", 1, plugin.tickHotkeys);
		assertEquals("OverlayHotkeys should reflect incremented value", 1, overlayHotkeys.getCurrentTick());
		
		// Test decrement operations
		java.lang.reflect.Method decrementMethod = VisualTicksPlugin.class.getDeclaredMethod("decrementTick", int.class);
		decrementMethod.setAccessible(true);
		
		// Decrement from 1 should give us 0 (normal decrement)
		decrementMethod.invoke(plugin, 1); // Decrement configuration One
		assertEquals("After decrement, tickOne should be 0", 0, plugin.tickOne);
		assertEquals("OverlayOne should reflect decremented value", 0, overlayOne.getCurrentTick());
		
		// Test decrement from 0 to see wrapping behavior
		decrementMethod.invoke(plugin, 1); // Decrement configuration One from 0
		assertEquals("After decrement from 0, tickOne should wrap to 4", 4, plugin.tickOne);
		assertEquals("OverlayOne should reflect wrapped value", 4, overlayOne.getCurrentTick());
		
		// For configuration Two (numberOfTicksTwo = 3, valid values 0-2)
		decrementMethod.invoke(plugin, 2); // Decrement configuration Two from 1
		assertEquals("After decrement, tickTwo should be 0", 0, plugin.tickTwo);
		assertEquals("OverlayTwo should reflect decremented value", 0, overlayTwo.getCurrentTick());
		
		// Test wrapping for configuration Two
		decrementMethod.invoke(plugin, 2); // Decrement configuration Two from 0
		assertEquals("After decrement from 0, tickTwo should wrap to 2", 2, plugin.tickTwo);
		assertEquals("OverlayTwo should reflect wrapped value", 2, overlayTwo.getCurrentTick());
		
		// For configuration Three (numberOfTicksThree = 4, valid values 0-3)
		decrementMethod.invoke(plugin, 3); // Decrement configuration Three from 1
		assertEquals("After decrement, tickThree should be 0", 0, plugin.tickThree);
		assertEquals("OverlayThree should reflect decremented value", 0, overlayThree.getCurrentTick());
		
		// Test wrapping for configuration Three
		decrementMethod.invoke(plugin, 3); // Decrement configuration Three from 0
		assertEquals("After decrement from 0, tickThree should wrap to 3", 3, plugin.tickThree);
		assertEquals("OverlayThree should reflect wrapped value", 3, overlayThree.getCurrentTick());
		
		// For configuration Hotkeys (numberOfTicksHotkeys = 6, valid values 0-5)
		decrementMethod.invoke(plugin, 4); // Decrement configuration Hotkeys from 1
		assertEquals("After decrement, tickHotkeys should be 0", 0, plugin.tickHotkeys);
		assertEquals("OverlayHotkeys should reflect decremented value", 0, overlayHotkeys.getCurrentTick());
		
		// Test wrapping for configuration Hotkeys
		decrementMethod.invoke(plugin, 4); // Decrement configuration Hotkeys from 0
		assertEquals("After decrement from 0, tickHotkeys should wrap to 5", 5, plugin.tickHotkeys);
		assertEquals("OverlayHotkeys should reflect wrapped value", 5, overlayHotkeys.getCurrentTick());
	}
	
	/**
	 * Test that overlays ignore tick manipulation when configurations are disabled.
	 * This verifies that disabled configurations maintain their state.
	 */
	@Test
	public void testOverlayIgnoresDisabledConfigurations() throws Exception {
		// Setup configuration with some configurations disabled
		testConfig.setEnabledOne(false); // Disabled
		testConfig.setEnabledTwo(true);  // Enabled
		
		// Create real overlay instances
		VisualTicksOverlayOne overlayOne = new VisualTicksOverlayOne(plugin, testConfig, null);
		VisualTicksOverlayTwo overlayTwo = new VisualTicksOverlayTwo(plugin, testConfig, null);
		
		// Use reflection to call private increment methods
		java.lang.reflect.Method incrementMethod = VisualTicksPlugin.class.getDeclaredMethod("incrementTick", int.class);
		incrementMethod.setAccessible(true);
		
		// Test that disabled configuration is not affected
		incrementMethod.invoke(plugin, 1); // Try to increment disabled configuration One
		assertEquals("Disabled configuration should not change", 0, plugin.tickOne);
		assertEquals("OverlayOne should reflect unchanged value", 0, overlayOne.getCurrentTick());
		
		// Test that enabled configuration is affected
		incrementMethod.invoke(plugin, 2); // Increment enabled configuration Two
		assertEquals("Enabled configuration should change", 1, plugin.tickTwo);
		assertEquals("OverlayTwo should reflect changed value", 1, overlayTwo.getCurrentTick());
	}
	
	/**
	 * Test that direct field changes are immediately reflected in overlay getCurrentTick() methods.
	 * This verifies that no additional update mechanism is needed beyond direct field access.
	 */
	@Test
	public void testOverlayDirectFieldAccess() {
		// Create overlay instances
		VisualTicksOverlayOne overlayOne = new VisualTicksOverlayOne(plugin, testConfig, null);
		VisualTicksOverlayTwo overlayTwo = new VisualTicksOverlayTwo(plugin, testConfig, null);
		VisualTicksOverlayThree overlayThree = new VisualTicksOverlayThree(plugin, testConfig, null);
		VisualTicksOverlayHotkeys overlayHotkeys = new VisualTicksOverlayHotkeys(plugin, testConfig, null);
		
		// Directly modify plugin tick fields
		plugin.tickOne = 3;
		plugin.tickTwo = 1;
		plugin.tickThree = 2;
		plugin.tickHotkeys = 4;
		
		// Verify overlays immediately reflect the changes
		assertEquals("OverlayOne should immediately reflect field change", 3, overlayOne.getCurrentTick());
		assertEquals("OverlayTwo should immediately reflect field change", 1, overlayTwo.getCurrentTick());
		assertEquals("OverlayThree should immediately reflect field change", 2, overlayThree.getCurrentTick());
		assertEquals("OverlayHotkeys should immediately reflect field change", 4, overlayHotkeys.getCurrentTick());
		
		// Change values again
		plugin.tickOne = 0;
		plugin.tickTwo = 2;
		plugin.tickThree = 1;
		plugin.tickHotkeys = 5;
		
		// Verify overlays still reflect the changes
		assertEquals("OverlayOne should reflect second field change", 0, overlayOne.getCurrentTick());
		assertEquals("OverlayTwo should reflect second field change", 2, overlayTwo.getCurrentTick());
		assertEquals("OverlayThree should reflect second field change", 1, overlayThree.getCurrentTick());
		assertEquals("OverlayHotkeys should reflect second field change", 5, overlayHotkeys.getCurrentTick());
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(VisualTicksPlugin.class);
		RuneLite.main(args);
	}
}