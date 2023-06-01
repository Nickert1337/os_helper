package com.hunllef;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hunllef")
public interface HunllefConfig extends Config
{
	@ConfigItem(
		position = 1,
		keyName = "autoHide",
		name = "Automatically hide",
		description = "If checked, only show the plugin panel inside The Gauntlet"
	)
	default boolean autoHide()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "audioMode",
		name = "Audio Mode",
		description = ""
	)
	default AudioMode audioMode()
	{
		return AudioMode.Default;
	}

	@ConfigItem(
			position = 3,
			keyName = "prayer",
			name = "Auto prayer",
			description = ""
	)
	default boolean autoPray()
	{
		return true;
	}

	@ConfigItem(
			keyName = "target",
			name = "Delay Target",
			description = "",
			position = 4
	)
	default int getTarget()
	{
		return 180;
	}

	@ConfigItem(
			keyName = "deviation",
			name = "Delay Deviation",
			description = "",
			position = 5
	)
	default int getDeviation()
	{
		return 10;
	}

	@ConfigItem(
			position = 6,
			keyName = "prayer",
			name = "Auto dmg prayers",
			description = ""
	)
	default boolean autoDmgPrays()
	{
		return true;
	}

	@ConfigItem(
			position = 7,
			keyName = "prayer",
			name = "Auto switch weapons",
			description = ""
	)
	default boolean autoSwitchWeapons()
	{
		return true;
	}
}
