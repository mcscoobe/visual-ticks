/*
 * Copyright (c) 2022, Damen <gh: damencs>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.visualticks.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Side-panel tab a tick set can be restricted to. RuneLite stores a chosen value by its constant name and reads it back with {@code valueOf},
 * so renaming a constant resets that setting for every user who picked it. That is why
 * {@link #FILIST} keeps its odd name.
 *
 * <p>Declaration order is dropdown order, not index order, which is why {@code FILIST(9)}
 * comes before {@code ACCOUNT(8)}.
 */
@RequiredArgsConstructor
@Getter
public enum InterfaceTab
{
    ALL("All", -1),
    COMBAT_OPTIONS("Combat", 0),
    SKILLS("Skills", 1),
    QUEST("Quest", 2),
    INVENTORY("Inventory", 3),
    EQUIPMENT("Equipment", 4),
    PRAYER("Prayer", 5),
    SPELLBOOK("Spellbook", 6),
    CLAN_CHAT("Clan Chat", 7),
    FILIST("Friends List", 9),
    ACCOUNT("Account", 8),
    LOGOUT("Logout", 10),
    SETTINGS("Settings", 11),
    EMOTES("Emotes", 12),
    MUSIC_PLAYER("Music", 13);

    private final String name;
    /**
     * The value RuneLite's {@code TOPLEVEL_PANEL} varc holds while this tab is open, or
     * {@code -1} for {@link #ALL}, which matches any tab.
     */
    private final int index;

    public boolean matchesAnyTab()
    {
        return index == -1;
    }

    @Override
    public String toString()
    {
        return name;
    }
}