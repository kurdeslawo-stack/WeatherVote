package pl.slawcio.weathervote;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class VoteManager implements Listener {

    private final WeatherVotePlugin plugin;
    private final TransitionEffects transitionEffects;
    private VotingUnit activeUnit;
    private final Set<UUID> agreeVoters = new HashSet<UUID>();
    private final Set<UUID> disagreeVoters = new HashSet<UUID>();
    private final Map<VotingUnit.Category, Long> cooldownUntil = new EnumMap<VotingUnit.Category, Long>(VotingUnit.Category.class);

    private int remainingSeconds;
    private BukkitTask task;
    private BossBar bossBar;

    public VoteManager(WeatherVotePlugin plugin) {
        this.plugin = plugin;
        this.transitionEffects = new TransitionEffects(plugin);
    }

    public boolean isActive() {
        return activeUnit != null;
    }

    public StartResult startVote(CommandSender initiator, VotingUnit unit) {
        if (isActive()) {
            return StartResult.ALREADY_ACTIVE;
        }

        if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("Round.MinPlayers", 1)) {
            return StartResult.NOT_ENOUGH_PLAYERS;
        }

        if (plugin.getConfig().getBoolean("Round.Pause.Active", true) && getCooldownSeconds(unit) > 0L) {
            return StartResult.ON_COOLDOWN;
        }

        activeUnit = unit;
        agreeVoters.clear();
        disagreeVoters.clear();
        remainingSeconds = Math.max(1, plugin.getConfig().getInt("Round.Duration", 30));

        if (initiator instanceof Player) {
            agreeVoters.add(((Player) initiator).getUniqueId());
        }

        if (plugin.getConfig().getBoolean("Visual.BossBar.Active", true)) {
            setupBossBar();
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L);

        announceVoteStart(initiator, unit);
        return StartResult.STARTED;
    }

    public VoteResult castVote(Player player, boolean agree) {
        if (!isActive()) {
            return VoteResult.NO_ACTIVE_VOTE;
        }

        UUID id = player.getUniqueId();
        if (agreeVoters.contains(id) || disagreeVoters.contains(id)) {
            return VoteResult.ALREADY_VOTED;
        }

        if (agree) {
            agreeVoters.add(id);
        } else {
            disagreeVoters.add(id);
        }

        updateBossBar();
        return VoteResult.OK;
    }

    public long getCooldownSeconds(VotingUnit unit) {
        Long until = cooldownUntil.get(unit.getCategory());
        if (until == null) {
            return 0L;
        }
        long remaining = until.longValue() - System.currentTimeMillis();
        if (remaining <= 0L) {
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    public int getAgreeCount() {
        return agreeVoters.size();
    }

    public int getDisagreeCount() {
        return disagreeVoters.size();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        activeUnit = null;
        agreeVoters.clear();
        disagreeVoters.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null && isActive()) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    private void announceVoteStart(CommandSender initiator, VotingUnit unit) {
        String consoleMessage = plugin.msg(
                "VoteStarted",
                "{player}", initiator.getName(),
                "{unit}", plugin.unitName(unit));

        if (!plugin.getConfig().getBoolean("Visual.InteractiveChat.Active", true)) {
            Bukkit.broadcastMessage(consoleMessage);
            return;
        }

        Bukkit.getConsoleSender().sendMessage(consoleMessage);

        String divider = plugin.raw("VisualMessages.ChatDivider");
        String title = plugin.raw("VisualMessages.ChatTitle");
        String proposal = plugin.raw(
                "VisualMessages.ChatProposal",
                "{player}", initiator.getName(),
                "{unit}", plugin.unitName(unit));
        BaseComponent[] buttons = buildClickableVoteLine();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(divider);
            player.sendMessage(title);
            player.sendMessage(proposal);
            player.spigot().sendMessage(buttons);
            player.sendMessage(divider);
        }
    }

    private BaseComponent[] buildClickableVoteLine() {
        List<BaseComponent> components = new ArrayList<BaseComponent>();
        appendLegacy(components, plugin.raw("VisualMessages.ClickPrompt"), null);
        appendLegacy(components, plugin.raw("VisualMessages.ClickYes"), "/vot yes");
        appendLegacy(components, plugin.raw("VisualMessages.ClickSeparator"), null);
        appendLegacy(components, plugin.raw("VisualMessages.ClickNo"), "/vot no");
        return components.toArray(new BaseComponent[components.size()]);
    }

    private void appendLegacy(List<BaseComponent> target, String text, String command) {
        BaseComponent[] parts = TextComponent.fromLegacyText(text);
        for (BaseComponent part : parts) {
            if (command != null) {
                part.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            }
            target.add(part);
        }
    }

    private void tick() {
        remainingSeconds--;
        updateBossBar();

        if (plugin.getConfig().getBoolean("Visual.ActionBar.Active", true) && activeUnit != null) {
            String message = plugin.raw(
                    "VisualMessages.ActionBar",
                    "{unit}", plugin.unitName(activeUnit),
                    "{seconds}", String.valueOf(Math.max(remainingSeconds, 0)),
                    "{agree}", String.valueOf(agreeVoters.size()),
                    "{disagree}", String.valueOf(disagreeVoters.size()));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            }
        }

        if (remainingSeconds <= 0) {
            endVote();
        }
    }

    private void endVote() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        boolean success = agreeVoters.size() > disagreeVoters.size();
        VotingUnit finishedUnit = activeUnit;

        if (success) {
            for (World world : getTargetWorlds()) {
                transitionEffects.play(world, finishedUnit);
            }
            Bukkit.broadcastMessage(plugin.msg(
                    "VoteSuccess",
                    "{unit}", plugin.unitName(finishedUnit),
                    "{agree}", String.valueOf(agreeVoters.size()),
                    "{disagree}", String.valueOf(disagreeVoters.size())));
        } else {
            Bukkit.broadcastMessage(plugin.msg(
                    "VoteFailed",
                    "{unit}", plugin.unitName(finishedUnit),
                    "{agree}", String.valueOf(agreeVoters.size()),
                    "{disagree}", String.valueOf(disagreeVoters.size())));
        }

        if (plugin.getConfig().getBoolean("Round.Pause.Active", true)) {
            int pause = Math.max(0, plugin.getConfig().getInt(
                    "Round.Pause.CategoryList." + finishedUnit.getCategory().name() + ".Duration", 0));
            cooldownUntil.put(finishedUnit.getCategory(), System.currentTimeMillis() + pause * 1000L);
        }

        activeUnit = null;
        agreeVoters.clear();
        disagreeVoters.clear();
    }

    private void setupBossBar() {
        BarColor color = parseBarColor(plugin.getConfig().getString("Visual.BossBar.Color", "BLUE"));
        BarStyle style = parseBarStyle(plugin.getConfig().getString("Visual.BossBar.Style", "SEGMENTED_20"));
        bossBar = Bukkit.createBossBar(buildBossBarTitle(), color, style);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void updateBossBar() {
        if (bossBar == null || activeUnit == null) {
            return;
        }
        bossBar.setTitle(buildBossBarTitle());
        int duration = Math.max(1, plugin.getConfig().getInt("Round.Duration", 30));
        double progress = (double) remainingSeconds / (double) duration;
        bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, progress)));
    }

    private String buildBossBarTitle() {
        return plugin.raw(
                "VisualMessages.BossBarTitle",
                "{unit}", plugin.unitName(activeUnit),
                "{seconds}", String.valueOf(Math.max(remainingSeconds, 0)),
                "{agree}", String.valueOf(agreeVoters.size()),
                "{disagree}", String.valueOf(disagreeVoters.size()));
    }

    private List<World> getTargetWorlds() {
        List<String> names = plugin.getConfig().getStringList("Worlds");
        List<World> worlds = new ArrayList<World>();

        if (names.isEmpty()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    worlds.add(world);
                }
            }
            return worlds;
        }

        for (String name : names) {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                worlds.add(world);
            }
        }
        return worlds;
    }

    private BarColor parseBarColor(String value) {
        try {
            return BarColor.valueOf(Objects.toString(value, "BLUE").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid BossBar color '" + value + "'. Using BLUE.");
            return BarColor.BLUE;
        }
    }

    private BarStyle parseBarStyle(String value) {
        try {
            return BarStyle.valueOf(Objects.toString(value, "SEGMENTED_20").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid BossBar style '" + value + "'. Using SEGMENTED_20.");
            return BarStyle.SEGMENTED_20;
        }
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        NOT_ENOUGH_PLAYERS,
        ON_COOLDOWN
    }

    public enum VoteResult {
        OK,
        NO_ACTIVE_VOTE,
        ALREADY_VOTED
    }
}
