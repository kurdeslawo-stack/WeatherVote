package pl.slawcio.weathervote;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class LanguageManager {

    private final WeatherVotePlugin plugin;
    private FileConfiguration language;
    private String languageCode;

    public LanguageManager(WeatherVotePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String configured = plugin.getConfig().getString("language", "pl");
        String normalized = configured == null ? "pl" : configured.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("en")) {
            normalized = "eng";
        }
        if (!normalized.equals("pl") && !normalized.equals("eng")) {
            plugin.getLogger().warning("Unsupported language '" + configured + "'. Falling back to 'eng'.");
            normalized = "eng";
        }

        this.languageCode = normalized;
        String resourceName = "lang_" + normalized + ".yml";
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream defaultsStream = plugin.getResource(resourceName);
        if (defaultsStream != null) {
            InputStreamReader reader = new InputStreamReader(defaultsStream, StandardCharsets.UTF_8);
            loaded.setDefaults(YamlConfiguration.loadConfiguration(reader));
        }
        this.language = loaded;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String message(String key, String... placeholders) {
        return color(raw("Messages.Prefix") + raw("Messages." + key, placeholders));
    }

    public String raw(String path, String... placeholders) {
        String value = language.getString(path, path);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace(placeholders[i], placeholders[i + 1]);
        }
        return color(value);
    }

    public String unitName(VotingUnit unit) {
        return raw("Units." + unit.getLanguageKey());
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
