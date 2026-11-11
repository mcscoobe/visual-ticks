package com.visualticks.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which adjust hotkeys {@code keyPressed} honours: the global pair, or each tick set's own.
 * RuneLite stores a chosen value by its constant name and reads it back with {@code valueOf},
 * so renaming a constant resets that setting for every user who picked it.
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
