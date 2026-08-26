package com.visualticks.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
