package com.example.voting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class VoteManager {

    private final VotingPlugin plugin;
    private VotingUnit activeUnit;
    private final Set<UUID> agreeVoters = new HashSet<>();
    private final Set<UUID> disagreeVoters = new HashSet<>();
    private int remainingSeconds;
    private BukkitTask task;
    private BossBar bossBar;

    private final Map<VotingUnit.Category, Long> cooldownUntil = new EnumMap<>(VotingUnit.Category.class);

    public VoteManager(VotingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return activeUnit != null;
    }

    public StartResult startVote(CommandSender initiator, VotingUnit unit) {
        if (isActive()) return StartResult.ALREADY_ACTIVE;

        if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("Round.MinPlayers", 1)) {
            return StartResult.NOT_ENOUGH_PLAYERS;
        }

        if (plugin.getConfig().getBoolean("Round.Pause.Active", true)) {
            Long until = cooldownUntil.get(unit.getCategory());
            if (until != null && until > System.currentTimeMillis()) return StartResult.ON_COOLDOWN;
        }

        activeUnit = unit;
        agreeVoters.clear();
        disagreeVoters.clear();
        remainingSeconds = plugin.getConfig().getInt("Round.Duration", 30);

        // POPRAWKA: Automatyczny głos inicjatora na TAK
        if (initiator instanceof Player) {
            agreeVoters.add(((Player) initiator).getUniqueId());
        }

        if (plugin.getConfig().getBoolean("Visual.BossBar.Active", true)) {
            setupBossBar();
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        Bukkit.broadcastMessage(plugin.msg("VoteStarted", "{player}", initiator.getName(), "{unit}", unit.getDisplayName()));

        return StartResult.STARTED;
    }

    public VoteResult castVote(Player player, boolean agree) {
        if (!isActive()) return VoteResult.NO_ACTIVE_VOTE;
        UUID id = player.getUniqueId();
        if (agreeVoters.contains(id) || disagreeVoters.contains(id)) return VoteResult.ALREADY_VOTED;
        
        if (agree) agreeVoters.add(id);
        else disagreeVoters.add(id);
        
        updateBossBar();
        return VoteResult.OK;
    }

    private void tick() {
        remainingSeconds--;
        updateBossBar();

        if (plugin.getConfig().getBoolean("Visual.ActionBar.Active", true)) {
            String msg = ChatColor.translateAlternateColorCodes('&', 
                "&b" + activeUnit.getDisplayName() + " &7- &f" + remainingSeconds + "s &7- &a" + agreeVoters.size() + " &7/ &c" + disagreeVoters.size());
            Bukkit.getOnlinePlayers().forEach(p -> p.sendActionBar(msg));
        }

        // Głosowanie kończy się TYLKO po upływie czasu
        if (remainingSeconds <= 0) {
            endVote();
        }
    }

    private void endVote() {
        if (task != null) { task.cancel(); task = null; }
        if (bossBar != null) { bossBar.removeAll(); bossBar = null; }

        boolean success = agreeVoters.size() > disagreeVoters.size();
        VotingUnit finishedUnit = activeUnit;

        if (success) {
            getTargetWorlds().forEach(finishedUnit::apply);
            Bukkit.broadcastMessage(plugin.msg("VoteSuccess", "{unit}", finishedUnit.getDisplayName(), "{agree}", String.valueOf(agreeVoters.size()), "{disagree}", String.valueOf(disagreeVoters.size())));
        } else {
            Bukkit.broadcastMessage(plugin.msg("VoteFailed", "{unit}", finishedUnit.getDisplayName(), "{agree}", String.valueOf(agreeVoters.size()), "{disagree}", String.valueOf(disagreeVoters.size())));
        }

        if (plugin.getConfig().getBoolean("Round.Pause.Active", true)) {
            int pause = plugin.getConfig().getInt("Round.Pause.CategoryList." + finishedUnit.getCategory().name() + ".Duration", 0);
            cooldownUntil.put(finishedUnit.getCategory(), System.currentTimeMillis() + pause * 1000L);
        }
        activeUnit = null;
    }

    // --- Metody pomocnicze ---
    private void setupBossBar() {
        bossBar = Bukkit.createBossBar(buildBossBarTitle(), BarColor.BLUE, BarStyle.SEGMENTED_20);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setTitle(buildBossBarTitle());
        double progress = (double) remainingSeconds / (double) plugin.getConfig().getInt("Round.Duration", 30);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    private String buildBossBarTitle() {
        return plugin.msg("BossBarTitle", "{unit}", activeUnit.getDisplayName(), "{seconds}", String.valueOf(remainingSeconds), "{agree}", String.valueOf(agreeVoters.size()), "{disagree}", String.valueOf(disagreeVoters.size()));
    }

    private List<World> getTargetWorlds() {
        List<String> names = plugin.getConfig().getStringList("Worlds");
        if (names.isEmpty()) return Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NORMAL).toList();
        return names.stream().map(Bukkit::getWorld).filter(Objects::nonNull).toList();
    }

    public int getAgreeCount() { return agreeVoters.size(); }
    public int getDisagreeCount() { return disagreeVoters.size(); }
    public enum StartResult { STARTED, ALREADY_ACTIVE, NOT_ENOUGH_PLAYERS, ON_COOLDOWN }
    public enum VoteResult { OK, NO_ACTIVE_VOTE, ALREADY_VOTED }
}
