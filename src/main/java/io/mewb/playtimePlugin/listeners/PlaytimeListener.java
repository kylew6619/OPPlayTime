package io.mewb.playtimePlugin.listeners;


import io.mewb.playtimePlugin.PlaytimePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlaytimeListener implements Listener {

    private final PlaytimePlugin plugin;

    public PlaytimeListener(PlaytimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlaytimeManager().loadPlayerData(player.getUniqueId());
        // Update activity on join to ensure they are not immediately AFK
        plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId()).updateActivity();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlaytimeManager().savePlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only update activity if the player actually moved a block
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            plugin.getPlaytimeManager().getPlaytimePlayer(event.getPlayer().getUniqueId()).updateActivity();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        plugin.getPlaytimeManager().getPlaytimePlayer(event.getPlayer().getUniqueId()).updateActivity();
    }
}