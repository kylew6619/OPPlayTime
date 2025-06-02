package io.mewb.playtimePlugin.commands;


import io.mewb.playtimePlugin.PlaytimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlaytimeCommandExecutor implements CommandExecutor {

    private final PlaytimePlugin plugin;

    public PlaytimeCommandExecutor(PlaytimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("playtime")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("playtime.open")) {
                player.sendMessage(plugin.getPlaytimeConfig().color("&cYou don't have permission to do that."));
                return true;
            }
            if (args.length == 0) {
                plugin.getPlaytimeGUI().openMainMenu(player);
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (!player.hasPermission("playtime.admin")) {
                        player.sendMessage(plugin.getPlaytimeConfig().color("&cYou don't have permission to reload the config."));
                        return true;
                    }
                    plugin.getPlaytimeConfig().loadConfig();
                    plugin.getPlaytimeManager().startPlaytimeTask(); // Restart task in case timeout changed
                    player.sendMessage(plugin.getPlaytimeConfig().color("&aPlaytime plugin config reloaded."));
                    return true;
                }
            }
        } else if (command.getName().equalsIgnoreCase("playtimetop")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("playtime.top")) {
                player.sendMessage(plugin.getPlaytimeConfig().color("&cYou don't have permission to do that."));
                return true;
            }
            plugin.getPlaytimeGUI().openLeaderboardGUI(player);
            return true;
        } else if (command.getName().equalsIgnoreCase("playtimereset")) {
            if (!sender.hasPermission("playtime.reset")) {
                sender.sendMessage(plugin.getPlaytimeConfig().color("&cYou don't have permission to do that."));
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(plugin.getPlaytimeConfig().color("&cUsage: /playtimereset <player>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getPlaytimeConfig().color("&cPlayer not found."));
                return true;
            }
            plugin.getPlaytimeManager().resetPlayerData(target.getUniqueId());
            sender.sendMessage(plugin.getPlaytimeConfig().color("&aPlaytime data for &e" + target.getName() + " &ahas been reset."));
            return true;
        } else if (command.getName().equalsIgnoreCase("playtimeresetall")) {
            if (!sender.hasPermission("playtime.resetall")) {
                sender.sendMessage(plugin.getPlaytimeConfig().color("&cYou don't have permission to do that."));
                return true;
            }
            // Simple confirmation for reset all
            if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
                sender.sendMessage(plugin.getPlaytimeConfig().color("&cAre you sure you want to reset all playtime data? This cannot be undone. Type /playtimeresetall confirm to proceed."));
                return true;
            }
            plugin.getPlaytimeManager().resetAllPlayerData();
            sender.sendMessage(plugin.getPlaytimeConfig().color("&aAll playtime data has been reset."));
            return true;
        }
        return false;
    }
}