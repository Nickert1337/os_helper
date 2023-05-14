package com.hunllef.mappers;

import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public enum PrayerMap
{
    THICK_SKIN("Thick Skin", WidgetMap.PRAYER_THICK_SKIN),
    BURST_OF_STRENGTH("Burst of Strength", WidgetMap.PRAYER_BURST_OF_STRENGTH),
    CLARITY_OF_THOUGHT("Clarity of Thought", WidgetMap.PRAYER_CLARITY_OF_THOUGHT),
    SHARP_EYE("Sharp Eye", WidgetMap.PRAYER_SHARP_EYE),
    MYSTIC_WILL("Mystic Will", WidgetMap.PRAYER_MYSTIC_WILL),
    ROCK_SKIN("Rock Skin", WidgetMap.PRAYER_ROCK_SKIN),
    SUPERHUMAN_STRENGTH("Superhuman Strength", WidgetMap.PRAYER_SUPERHUMAN_STRENGTH),
    IMPROVED_REFLEXES("Improved Reflexes", WidgetMap.PRAYER_IMPROVED_REFLEXES),
    RAPID_RESTORE("Rapid Restore", WidgetMap.PRAYER_RAPID_RESTORE),
    RAPID_HEAL("Rapid Heal", WidgetMap.PRAYER_RAPID_HEAL),
    PROTECT_ITEM("Protect Item", WidgetMap.PRAYER_PROTECT_ITEM),
    HAWK_EYE("Hawk Eye", WidgetMap.PRAYER_HAWK_EYE),
    MYSTIC_LORE("Mystic Lore", WidgetMap.PRAYER_MYSTIC_LORE),
    STEEL_SKIN("Steel Skin", WidgetMap.PRAYER_STEEL_SKIN),
    ULTIMATE_STRENGTH("Ultimate Strength", WidgetMap.PRAYER_ULTIMATE_STRENGTH),
    INCREDIBLE_REFLEXES("Incredible Reflexes", WidgetMap.PRAYER_INCREDIBLE_REFLEXES),
    PROTECT_FROM_MAGIC("Protect from Magic", WidgetMap.PRAYER_PROTECT_FROM_MAGIC),
    PROTECT_FROM_MISSILES("Protect from Missiles", WidgetMap.PRAYER_PROTECT_FROM_MISSILES),
    PROTECT_FROM_MELEE("Protect from Melee", WidgetMap.PRAYER_PROTECT_FROM_MELEE),
    EAGLE_EYE("Eagle Eye", WidgetMap.PRAYER_EAGLE_EYE),
    MYSTIC_MIGHT("Mystic Might", WidgetMap.PRAYER_MYSTIC_MIGHT),
    RETRIBUTION("Retribution", WidgetMap.PRAYER_RETRIBUTION),
    REDEMPTION("Redemption", WidgetMap.PRAYER_REDEMPTION),
    SMITE("Smite", WidgetMap.PRAYER_SMITE),
    CHIVALRY("Chivalry", WidgetMap.PRAYER_CHIVALRY),
    PIETY("Piety", WidgetMap.PRAYER_PIETY),
    PRESERVE("Preserve", WidgetMap.PRAYER_PRESERVE),
    RIGOUR("Rigour", WidgetMap.PRAYER_RIGOUR),
    AUGURY("Augury", WidgetMap.PRAYER_AUGURY);

    private final String name;
    private final WidgetMap info;
    private static final Map<String, WidgetMap> map;

    static
    {
        ImmutableMap.Builder<String, WidgetMap> builder = ImmutableMap.builder();

        for (PrayerMap spells : values())
        {
            builder.put(spells.getName(), spells.getInfo());
        }

        map = builder.build();
    }

    @Nullable
    public static WidgetMap getWidget(String prayer)
    {
        return map.getOrDefault(prayer, null);
    }
}
