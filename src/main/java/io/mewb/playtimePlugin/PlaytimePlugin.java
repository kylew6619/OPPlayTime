package io.mewb.playtimePlugin;

import io.mewb.playtimePlugin.commands.PlaytimeCommandExecutor;
import io.mewb.playtimePlugin.config.PlaytimeConfig;
import io.mewb.playtimePlugin.gui.PlaytimeGUI;
import io.mewb.playtimePlugin.listeners.PlaytimeListener;
import io.mewb.playtimePlugin.manager.PlaytimeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimePlugin extends JavaPlugin {

    private PlaytimeConfig playtimeConfig;
    private PlaytimeManager playtimeManager;
    private  PlaytimeGUI playtimeGUI;

    @Override
    public void onEnable() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize configuration
        saveDefaultConfig(); // Saves config.yml from resources if it doesn't exist
        playtimeConfig = new PlaytimeConfig(this);
        playtimeConfig.loadConfig();

        // Initialize manager and GUI
        playtimeManager = new PlaytimeManager(this);
        playtimeGUI = new PlaytimeGUI(this);

        // Register commands
        getCommand("playtime").setExecutor(new PlaytimeCommandExecutor(this));
        getCommand("playtimetop").setExecutor(new PlaytimeCommandExecutor(this));
        getCommand("playtimereset").setExecutor(new PlaytimeCommandExecutor(this));
        getCommand("playtimeresetall").setExecutor(new PlaytimeCommandExecutor(this));

        // Register event listener
        getServer().getPluginManager().registerEvents(new PlaytimeListener(this), this);

        // Start playtime tracking task
        playtimeManager.startPlaytimeTask();

        getLogger().info("Playtime plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player data before disabling
        if (playtimeManager != null) {
            playtimeManager.saveAllPlayers();
        }
        getLogger().info("Playtime plugin disabled!");
    }

    public PlaytimeConfig getPlaytimeConfig() {
        return playtimeConfig;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public PlaytimeGUI getPlaytimeGUI() {
        return playtimeGUI;
    }
}
