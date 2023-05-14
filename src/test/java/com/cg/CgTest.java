package com.cg;

import com.hunllef.HunllefPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CgTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HunllefPlugin.class);
		RuneLite.main(args);
	}
}