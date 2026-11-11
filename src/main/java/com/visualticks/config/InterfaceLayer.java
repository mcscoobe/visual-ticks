package com.visualticks.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.overlay.OverlayLayer;

@RequiredArgsConstructor
@Getter
public enum InterfaceLayer
{
    ABOVE("Above interfaces", OverlayLayer.ABOVE_WIDGETS),
    // Deliberately not UNDER_WIDGETS: OverlayManager promotes an UNDER_WIDGETS overlay to
    // ABOVE_WIDGETS once it has a preferred location and no preferred position, which is
    // every overlay the user has dragged off a snap corner. "Behind" would then do nothing
    // for exactly the players who asked for it. The cost is drawing under overheads.
    BEHIND("Behind interfaces", OverlayLayer.ABOVE_SCENE);

    private final String name;
    private final OverlayLayer overlayLayer;

    @Override
    public String toString()
    {
        return name;
    }
}
