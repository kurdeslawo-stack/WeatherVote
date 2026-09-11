package pl.slawcio.weathervote;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class VoteCommand implements CommandExecutor, TabCompleter {

    private static final List<String> OPTIONS = Arrays.asList(
            "Day", "Night", "Sun", "Rain", "Storm", "Yes", "No", "Reload");

    private final WeatherVotePlugin plugin;

    public VoteCommand(WeatherVotePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.msg("Usage"));
            return true;
        }

        String arg = args[0].toLowerCase(Locale.ROOT);

        if (arg.equals("reload")) {
            if (!sender.hasPermission("weathervote.admin")) {
                sender.sendMessage(plugin.msg("NoPermission"));
                return true;
            }
            plugin.reloadWeatherVote();
            sender.sendMessage(plugin.msg("Reloaded"));
            return true;
        }

        if (arg.equals("yes") || arg.equals("no")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg("PlayerOnly"));
                return true;
            }
            if (!plugin.hasPlacePermission(sender)) {
                sender.sendMessage(plugin.msg("NoPermission"));
                return true;
            }

            boolean agree = arg.equals("yes");
            VoteManager.VoteResult result = plugin.getVoteManager().castVote((Player) sender, agree);

            switch (result) {
                case NO_ACTIVE_VOTE:
                    sender.sendMessage(plugin.msg("NoActiveVote"));
                    break;
                case ALREADY_VOTED:
                    sender.sendMessage(plugin.msg("AlreadyVoted"));
                    break;
                case OK:
                    sender.sendMessage(plugin.msg(
                            "VoteCast",
                            "{choice}", plugin.raw(agree ? "Choices.Yes" : "Choices.No"),
                            "{agree}", String.valueOf(plugin.getVoteManager().getAgreeCount()),
                            "{disagree}", String.valueOf(plugin.getVoteManager().getDisagreeCount())));
                    break;
                default:
                    break;
            }
            return true;
        }

        if (!plugin.hasStartPermission(sender)) {
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
            case ALREADY_ACTIVE:
                sender.sendMessage(plugin.msg("AlreadyActive"));
                break;
            case NOT_ENOUGH_PLAYERS:
                sender.sendMessage(plugin.msg("NotEnoughPlayers"));
                break;
            case ON_COOLDOWN:
                sender.sendMessage(plugin.msg(
                        "OnCooldown",
                        "{seconds}", String.valueOf(plugin.getVoteManager().getCooldownSeconds(unit))));
                break;
            case STARTED:
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return new ArrayList<String>();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<String>();
        for (String option : OPTIONS) {
            if (option.equalsIgnoreCase("Reload") && !sender.hasPermission("weathervote.admin")) {
                continue;
            }
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
