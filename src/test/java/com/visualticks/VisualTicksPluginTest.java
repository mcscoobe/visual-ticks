package com.visualticks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class VisualTicksPluginTest
{
	// loadBuiltin is generic varargs and not @SafeVarargs; this single-element call is type-correct.
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(VisualTicksPlugin.class);
		RuneLite.main(args);
	}
}