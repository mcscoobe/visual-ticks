# Visual Ticks

This plugin adds a customisable visual tick overlay. 

You can have up to 3 different tick configurations at once.

![Multiple configurations](./readme/many-configs.gif)

Configurations can be toggled to only show whilst on a specific tab.

Each configuration also has an **Interface layer** setting: *Above interfaces* (the default)
keeps the ticks over the bank, raid storage and wave pickers, while *Behind interfaces* lets
those interfaces cover them. Behind also draws under hitsplats, HP bars and overhead text,
and in Fixed mode clips the ticks to the game view.

Tick counts can be changed without opening the settings, using an increase and a decrease
hotkey. Set **Tick Adjust Hotkeys** to *Global* for one pair of keys that moves every
configuration at once, or *Independent* to give each configuration its own pair. Decreasing
a configuration that is already at 2 ticks hides it. A configuration's own increase key
brings it back, even one you turned off in the settings; the global increase brings back only
the configurations the global decrease hid.

The **Tick Reset Hotkey** (backtick by default) restarts every configuration's count.

Adjust hotkeys are ignored while you are typing, and a key that triggers one is consumed
rather than passed on to the game. Binding a plain printable key therefore costs you that
key in an empty chatbox — bind a modifier combination to keep using it for both.

![Tab specific](./readme/tab-only-mode.gif)