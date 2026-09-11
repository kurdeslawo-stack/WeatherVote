package pl.slawcio.weathervote;

import org.bukkit.World;

public enum VotingUnit {
    DAY("Day", "Day", Category.DAY_LIGHT) {
        @Override
        public void apply(World world) {
            world.setTime(1000L);
            world.setStorm(false);
            world.setThundering(false);
        }
    },
    NIGHT("Night", "Night", Category.DAY_LIGHT) {
        @Override
        public void apply(World world) {
            world.setTime(13000L);
        }
    },
    SUN("Sun", "Sun", Category.CLIMATE) {
        @Override
        public void apply(World world) {
            world.setStorm(false);
            world.setThundering(false);
        }
    },
    RAIN("Rain", "Rain", Category.CLIMATE) {
        @Override
        public void apply(World world) {
            world.setStorm(true);
            world.setThundering(false);
        }
    },
    STORM("Storm", "Storm", Category.CLIMATE) {
        @Override
        public void apply(World world) {
            world.setStorm(true);
            world.setThundering(true);
        }
    };

    public enum Category {
        DAY_LIGHT,
        CLIMATE
    }

    private final String commandName;
    private final String languageKey;
    private final Category category;

    VotingUnit(String commandName, String languageKey, Category category) {
        this.commandName = commandName;
        this.languageKey = languageKey;
        this.category = category;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getLanguageKey() {
        return languageKey;
    }

    public Category getCategory() {
        return category;
    }

    public abstract void apply(World world);

    public static VotingUnit fromString(String name) {
        for (VotingUnit unit : values()) {
            if (unit.name().equalsIgnoreCase(name) || unit.commandName.equalsIgnoreCase(name)) {
                return unit;
            }
        }
        return null;
    }
}
