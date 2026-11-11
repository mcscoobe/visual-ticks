package com.visualticks.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which adjust hotkeys {@code keyPressed} honours: the global pair, or each tick set's own.
 * Constant names are stored user data; see {@link com.visualticks.VisualTicksConfig}.
 */
@RequiredArgsConstructor
@Getter
public enum HotkeyMode
{
    GLOBAL("Global"),
    INDEPENDENT("Independent");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
