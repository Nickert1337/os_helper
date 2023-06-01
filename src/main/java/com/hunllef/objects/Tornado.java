package com.hunllef.objects;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.NPC;

@Getter(AccessLevel.PACKAGE)
public class Tornado
{
    @Getter
    private NPC npc;

    @Getter
    private int timeLeft;

    public Tornado(NPC npc)
    {
        this.npc = npc;
        this.timeLeft = 20;
    }

    public void updateTimeLeft()
    {
        if (timeLeft > 0)
        {
            timeLeft--;
        }
    }
}
