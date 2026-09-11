package com.example.voting;

import org.bukkit.World;

public enum VotingUnit {
    DAY("Day", Category.DAY_LIGHT) {
        @Override public void apply(World world) { world.setTime(1000L); world.setStorm(false); world.setThundering(false); }
    },
    NIGHT("Night", Category.DAY_LIGHT) {
        @Override public void apply(World world) { world.setTime(13000L); }
    },
    SUN("Sun", Category.CLIMATE) {
        @Override public void apply(World world) { world.setStorm(false); world.setThundering(false); }
    },
    RAIN("Rain", Category.CLIMATE) {
        @Override public void apply(World world) { world.setStorm(true); world.setThundering(false); }
    },
    STORM("Storm", Category.CLIMATE) {
        @Override public void apply(World world) { world.setStorm(true); world.setThundering(true); }
    };

    public enum Category { DAY_LIGHT, CLIMATE }
    private final String displayName;
    private final Category category;

    VotingUnit(String displayName, Category category) { this.displayName = displayName; this.category = category; }
    public String getDisplayName() { return displayName; }
    public Category getCategory() { return category; }
    public abstract void apply(World world);

    public static VotingUnit fromString(String name) {
        for (VotingUnit unit : values()) {
            if (unit.name().equalsIgnoreCase(name) || unit.getDisplayName().equalsIgnoreCase(name)) return unit;
        }
        return null;
    }
}
