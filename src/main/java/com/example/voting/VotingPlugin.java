package com.example.voting;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class VotingPlugin extends JavaPlugin {

    private VoteManager voteManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.voteManager = new VoteManager(this);

        // Rejestrujemy tylko jedną komendę
        getCommand("vot").setExecutor(new VotCommand(this));

        getLogger().info("Voting plugin enabled - /vot <opcja|yes|no>");
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    // Twoja ulubiona metoda do wiadomości - zostaje bez zmian
    public String msg(String key, String... placeholders) {
        String raw = getConfig().getString("Messages." + key, key);
        String prefix = getConfig().getString("Messages.Prefix", "");
        String result = prefix + raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
