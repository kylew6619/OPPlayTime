package io.mewb.playtimePlugin.manager;


import io.mewb.playtimePlugin.PlaytimePlugin;
import io.mewb.playtimePlugin.data.PlaytimePlayer;
import io.mewb.playtimePlugin.data.Reward;
import io.mewb.playtimePlugin.data.TitleData;
import io.mewb.playtimePlugin.util.PlaytimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlaytimeManager {

    private final PlaytimePlugin plugin;
    private final Map<UUID, PlaytimePlayer> players;
    private BukkitTask playtimeTask;

    public PlaytimeManager(PlaytimePlugin plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentHashMap<>();
    }

    public PlaytimePlayer getPlaytimePlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, PlaytimePlayer::new);
    }

    public void loadPlayerData(UUID uuid) {
        File playerFile = new File(plugin.getDataFolder(), "data" + File.separator + uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            players.put(uuid, new PlaytimePlayer(uuid));
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        long activeTime = config.getLong("activeTime", 0);
        long afkTime = config.getLong("afkTime", 0);
        long lastActivityTime = config.getLong("lastActivityTime", System.currentTimeMillis());
        Set<Long> claimedRewards = config.getLongList("claimedRewards").stream()
                .boxed()
                .collect(Collectors.toSet());

        PlaytimePlayer playtimePlayer = new PlaytimePlayer(uuid, activeTime, afkTime, lastActivityTime, false, claimedRewards);
        players.put(uuid, playtimePlayer);
    }

    public void savePlayerData(UUID uuid) {
        PlaytimePlayer playtimePlayer = players.get(uuid);
        if (playtimePlayer == null) return;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("activeTime", playtimePlayer.getActiveTime());
        config.set("afkTime", playtimePlayer.getAfkTime());
        config.set("lastActivityTime", playtimePlayer.getLastActivityTime());
        config.set("claimedRewards", new ArrayList<>(playtimePlayer.getClaimedRewards()));

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data for player " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAllPlayers() {
        players.keySet().forEach(this::savePlayerData);
    }

    public void startPlaytimeTask() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
        }
        // Run every 20 ticks (1 second)
        playtimeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlaytimePlayer pp = getPlaytimePlayer(player.getUniqueId());

                long timeSinceLastActivity = (currentTime - pp.getLastActivityTime()) / 1000; // in seconds

                if (plugin.getPlaytimeConfig().isAfkEnabled() && timeSinceLastActivity >= plugin.getPlaytimeConfig().getAfkTimeout()) {
                    // Player is AFK
                    if (!pp.isAFK()) {
                        pp.setAFK(true);
                        sendAfkMessage(player, true);
                    }
                    pp.addAfkTime(1); // Add 1 second to AFK time
                } else {
                    // Player is active
                    if (pp.isAFK()) {
                        pp.setAFK(false);
                        sendAfkMessage(player, false);
                    }
                    pp.addActiveTime(1); // Add 1 second to active time
                }

                // Check for rewards
                checkRewards(player, pp);
            }
        }, 20L, 20L);
    }

    private void sendAfkMessage(Player player, boolean isAfk) {
        if (isAfk) {
            if (plugin.getPlaytimeConfig().isAfkMessageEnabled()) {
                player.sendMessage(plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getAfkMessage()));
            }
            if (plugin.getPlaytimeConfig().isAfkTitleEnabled() && plugin.getPlaytimeConfig().getAfkTitle() != null) {
                TitleData title = plugin.getPlaytimeConfig().getAfkTitle();
                player.sendTitle(
                        plugin.getPlaytimeConfig().color(title.getMain()),
                        plugin.getPlaytimeConfig().color(title.getSubtitle()),
                        title.getFadeIn(), title.getStay(), title.getFadeOut()
                );
            }
        } else {
            if (plugin.getPlaytimeConfig().isBackMessageEnabled()) {
                player.sendMessage(plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getBackMessage()));
            }
            if (plugin.getPlaytimeConfig().isBackTitleEnabled() && plugin.getPlaytimeConfig().getBackTitle() != null) {
                TitleData title = plugin.getPlaytimeConfig().getBackTitle();
                player.sendTitle(
                        plugin.getPlaytimeConfig().color(title.getMain()),
                        plugin.getPlaytimeConfig().color(title.getSubtitle()),
                        title.getFadeIn(), title.getStay(), title.getFadeOut()
                );
            }
        }
    }

    private void checkRewards(Player player, PlaytimePlayer pp) {
        long totalPlaytime = pp.getTotalTime();
        for (Map.Entry<Long, Reward> entry : plugin.getPlaytimeConfig().getRewards().entrySet()) {
            long requiredPlaytime = entry.getKey();
            Reward reward = entry.getValue();

            if (totalPlaytime >= requiredPlaytime && !pp.getClaimedRewards().contains(requiredPlaytime)) {
                // Reward unlocked!
                pp.getClaimedRewards().add(requiredPlaytime);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Send title
                if (reward.getTitle() != null) {
                    TitleData title = reward.getTitle();
                    player.sendTitle(
                            plugin.getPlaytimeConfig().color(title.getMain()),
                            plugin.getPlaytimeConfig().color(title.getSubtitle()),
                            title.getFadeIn(), title.getStay(), title.getFadeOut()
                    );
                }

                // Run commands
                for (String cmd : reward.getCommands()) {
                    String formattedCmd = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                }
            }
        }
    }

    public void resetPlayerData(UUID uuid) {
        File playerFile = new File(plugin.getDataFolder(), "data" + File.separator + uuid.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
        players.remove(uuid); // Remove from cached players
        // If player is online, re-add with default values
        if (Bukkit.getPlayer(uuid) != null) {
            players.put(uuid, new PlaytimePlayer(uuid));
        }
    }

    public void resetAllPlayerData() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        players.clear(); // Clear all cached data
        // Re-add online players with default values
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player.getUniqueId(), new PlaytimePlayer(player.getUniqueId()));
        }
    }

    public List<PlaytimePlayer> getTopPlaytimes(int limit) {
        return players.values().stream()
                .sorted(Comparator.comparingLong(PlaytimePlayer::getTotalTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public String formatTime(long seconds) {
        return PlaytimeFormatter.formatTime(seconds);
    }

    public String replacePlaceholders(String text, Player player, PlaytimePlayer pp) {
        String formattedText = text.replace("%player%", player.getName())
                .replace("%active%", formatTime(pp.getActiveTime()))
                .replace("%afk%", formatTime(pp.getAfkTime()))
                .replace("%total%", formatTime(pp.getTotalTime()));
        return plugin.getPlaytimeConfig().color(formattedText);
    }

    public List<String> replacePlaceholders(List<String> lore, Player player, PlaytimePlayer pp) {
        return lore.stream()
                .map(line -> replacePlaceholders(line, player, pp))
                .collect(Collectors.toList());
    }
}