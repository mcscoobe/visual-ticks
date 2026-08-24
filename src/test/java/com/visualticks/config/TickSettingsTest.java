package com.visualticks.config;

import com.visualticks.VisualTicksConfig;
import org.junit.Test;

import java.awt.Color;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The three factories are hand-copied, so the failure mode is a getter from the
 * wrong tick set or assigned to the wrong field. These tests feed the factories a
 * proxy config whose every answer is derived from the getter name, which makes any
 * such cross-wiring visible.
 */
public class TickSettingsTest
{
	private static final Map<String, Integer> VALUES = new HashMap<>();
	private static final AtomicInteger NEXT_VALUE = new AtomicInteger(1);

	@Test
	public void oneReadsOnlyItsOwnGetters()
	{
		assertSuffix("One", TickSettings::one);
	}

	@Test
	public void twoReadsOnlyItsOwnGetters()
	{
		assertSuffix("Two", TickSettings::two);
	}

	@Test
	public void threeReadsOnlyItsOwnGetters()
	{
		assertSuffix("Three", TickSettings::three);
	}

	@Test
	public void allThreeFactoriesReadTheSameSettings()
	{
		assertEquals(baseNames("One", TickSettings::one), baseNames("Two", TickSettings::two));
		assertEquals(baseNames("One", TickSettings::one), baseNames("Three", TickSettings::three));
	}

	@Test
	public void oneMapsEveryGetterToItsMatchingField()
	{
		assertFields(TickSettings.one(recordingConfig(new ArrayList<>())), "One");
	}

	@Test
	public void twoMapsEveryGetterToItsMatchingField()
	{
		assertFields(TickSettings.two(recordingConfig(new ArrayList<>())), "Two");
	}

	@Test
	public void threeMapsEveryGetterToItsMatchingField()
	{
		assertFields(TickSettings.three(recordingConfig(new ArrayList<>())), "Three");
	}

	private static void assertFields(TickSettings s, String suffix)
	{
		assertEquals(intFor("numberOfTicks" + suffix), s.numberOfTicks);
		assertEquals(intFor("amountPerRow" + suffix), s.amountPerRow);
		assertEquals(intFor("sizeOfTickShapes" + suffix), s.shapeSize);
		assertEquals(intFor("tickArc" + suffix), s.arc);
		assertEquals(intFor("tickTextSize" + suffix), s.textSize);
		assertEquals(intFor("horizontalSpacing" + suffix), s.horizontalSpacing);
		assertEquals(intFor("verticalSpacing" + suffix), s.verticalSpacing);
		assertEquals(colourFor("tickColour" + suffix), s.tickColour);
		assertEquals(colourFor("currentTickColour" + suffix), s.currentTickColour);
		assertEquals(colourFor("tickTextColour" + suffix), s.textColour);
		assertEquals(colourFor("currentTickTextColour" + suffix), s.currentTextColour);
		// booleans answer true only for the text toggle, so a swap flips both
		assertTrue(s.showText);
		assertTrue(!s.showShape);
	}

	private static void assertSuffix(String suffix, Function<VisualTicksConfig, TickSettings> factory)
	{
		List<String> called = new ArrayList<>();
		factory.apply(recordingConfig(called));

		assertEquals("every field should come from one getter", 15, called.size());
		for (String name : called)
		{
			assertTrue(name + " does not belong to tick set " + suffix, name.endsWith(suffix));
		}
	}

	private static List<String> baseNames(String suffix, Function<VisualTicksConfig, TickSettings> factory)
	{
		List<String> called = new ArrayList<>();
		factory.apply(recordingConfig(called));

		List<String> bases = new ArrayList<>();
		for (String name : called)
		{
			bases.add(name.substring(0, name.length() - suffix.length()));
		}
		return bases;
	}

	private static VisualTicksConfig recordingConfig(List<String> called)
	{
		InvocationHandler handler = (proxy, method, args) ->
		{
			String name = method.getName();
			called.add(name);

			Class<?> type = method.getReturnType();
			if (type == int.class)
			{
				return intFor(name);
			}
			if (type == boolean.class)
			{
				return name.startsWith("shouldShowText");
			}
			if (type == Color.class)
			{
				return colourFor(name);
			}
			if (type == TickShape.class)
			{
				return TickShape.CIRCLE;
			}
			if (type == InterfaceTab.class)
			{
				return InterfaceTab.PRAYER;
			}
			throw new AssertionError("unhandled return type " + type + " for " + name);
		};

		return (VisualTicksConfig) Proxy.newProxyInstance(
			TickSettingsTest.class.getClassLoader(),
			new Class<?>[]{VisualTicksConfig.class},
			handler);
	}

	/**
	 * Distinct per getter name, so a mis-assigned field lands on the wrong number.
	 * Assigned sequentially rather than hashed so two getters can never collide.
	 */
	private static synchronized int intFor(String name)
	{
		return VALUES.computeIfAbsent(name, ignored -> NEXT_VALUE.getAndIncrement());
	}

	private static Color colourFor(String name)
	{
		return new Color(intFor(name));
	}
}
