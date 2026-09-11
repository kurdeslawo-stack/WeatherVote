package com.example.voting;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> OPTIONS = Arrays.asList("Day", "Night", "Sun", "Rain", "Storm", "Yes", "No");

    private final VotingPlugin plugin;
    public VotCommand(VotingPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUzyj: /vot <Day|Night|Sun|Rain|Storm|Yes|No>"));
            return true;
        }

        String arg = args[0].toLowerCase();

        // 1. OBSŁUGA GŁOSÓW (Yes / No)
        if (arg.equals("yes") || arg.equals("no")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Tylko gracze moga glosowac.");
                return true;
            }
            if (!sender.hasPermission("voting.vote.place")) {
                sender.sendMessage(plugin.msg("NoPermission"));
                return true;
            }

            boolean agree = arg.equals("yes");
            VoteManager.VoteResult res = plugin.getVoteManager().castVote((Player) sender, agree);

            switch (res) {
                case NO_ACTIVE_VOTE: sender.sendMessage(plugin.msg("NoActiveVote")); break;
                case ALREADY_VOTED: sender.sendMessage(plugin.msg("AlreadyVoted")); break;
                case OK:
                    sender.sendMessage(plugin.msg("VoteCast", 
                        "{choice}", (agree ? "Yes" : "No"), 
                        "{agree}", String.valueOf(plugin.getVoteManager().getAgreeCount()),
                        "{disagree}", String.valueOf(plugin.getVoteManager().getDisagreeCount())));
                    break;
            }
            return true;
        }

        // 2. OBSŁUGA ROZPOCZYNANIA GŁOSOWANIA
        if (!sender.hasPermission("voting.vote.start")) {
            sender.sendMessage(plugin.msg("NoPermission"));
            return true;
        }

        VotingUnit unit = VotingUnit.fromString(arg);
        if (unit == null) {
            sender.sendMessage(plugin.msg("UnknownUnit"));
            return true;
        }

        VoteManager.StartResult result = plugin.getVoteManager().startVote(sender, unit);
        switch (result) {
            case ALREADY_ACTIVE: sender.sendMessage(plugin.msg("AlreadyActive")); break;
            case NOT_ENOUGH_PLAYERS: sender.sendMessage(plugin.msg("NotEnoughPlayers")); break;
            case ON_COOLDOWN: sender.sendMessage(plugin.msg("OnCooldown")); break;
            case STARTED: break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String s : OPTIONS) if (s.toLowerCase().startsWith(args[0].toLowerCase())) matches.add(s);
            return matches;
        }
        return new ArrayList<>();
    }
}
