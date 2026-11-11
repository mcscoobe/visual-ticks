package com.visualticks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Not a test: the dev-client launcher, named and placed per the RuneLite plugin template so
 * {@code ./gradlew runTestClient} can start it. It side-loads the plugin and opens the real
 * game client. {@code loadBuiltin} refuses to run without assertions enabled, which is why
 * the {@code runTestClient} task passes {@code -ea}.
 */
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