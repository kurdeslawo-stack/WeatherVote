package pl.slawcio.weathervote;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeatherVotePlugin extends JavaPlugin {

    private VoteManager voteManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveLanguageDefaults();

        this.languageManager = new LanguageManager(this);
        this.voteManager = new VoteManager(this);
        getServer().getPluginManager().registerEvents(voteManager, this);

        PluginCommand command = getCommand("vot");
        if (command == null) {
            throw new IllegalStateException("Command 'vot' is missing from plugin.yml");
        }

        VoteCommand voteCommand = new VoteCommand(this);
        command.setExecutor(voteCommand);
        command.setTabCompleter(voteCommand);

        getLogger().info("WeatherVote " + getDescription().getVersion()
                + " enabled (language=" + languageManager.getLanguageCode() + ").");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.shutdown();
        }
    }

    public void reloadWeatherVote() {
        reloadConfig();
        languageManager.reload();
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public String msg(String key, String... placeholders) {
        return languageManager.message(key, placeholders);
    }

    public String raw(String path, String... placeholders) {
        return languageManager.raw(path, placeholders);
    }

    public String unitName(VotingUnit unit) {
        return languageManager.unitName(unit);
    }

    public boolean hasStartPermission(org.bukkit.command.CommandSender sender) {
        return sender.hasPermission("weathervote.vote.start") || sender.hasPermission("voting.vote.start");
    }

    public boolean hasPlacePermission(org.bukkit.command.CommandSender sender) {
        return sender.hasPermission("weathervote.vote.place") || sender.hasPermission("voting.vote.place");
    }

    private void saveLanguageDefaults() {
        if (!new java.io.File(getDataFolder(), "lang_pl.yml").exists()) {
            saveResource("lang_pl.yml", false);
        }
        if (!new java.io.File(getDataFolder(), "lang_eng.yml").exists()) {
            saveResource("lang_eng.yml", false);
        }
    }
}
