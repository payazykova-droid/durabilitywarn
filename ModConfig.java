package com.example.durabilitywarn;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "durabilitywarn")
public class ModConfig implements ConfigData {
    public boolean enabled = true;
    public boolean showText = true;
    public boolean playSound = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    public int thresholdPercent = 10;

    @ConfigEntry.BoundedDiscrete(min = 5, max = 120)
    public int repeatSeconds = 30;
}
