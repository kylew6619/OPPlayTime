package io.mewb.playtimePlugin.config;


import io.mewb.playtimePlugin.PlaytimePlugin;
import io.mewb.playtimePlugin.data.GUIItem;
import io.mewb.playtimePlugin.data.Reward;
import io.mewb.playtimePlugin.data.TitleData;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class PlaytimeConfig {

    private final PlaytimePlugin plugin;
    private FileConfiguration config;

    // AFK Settings
    private boolean afkEnabled;
    private long afkTimeout;
    private boolean afkMessageEnabled;
    private String afkMessage;
    private boolean afkTitleEnabled;
    private TitleData afkTitle;
    private boolean backMessageEnabled;
    private String backMessage;
    private boolean backTitleEnabled;
    private TitleData backTitle;

    // Main GUI Settings
    private String mainGUITitle;
    private int mainGUISize;
    private GUIItem mainGUIFiller;
    private Map<String, GUIItem> mainGUIItems;

    // Leaderboard GUI Settings
    private String leaderboardGUITitle;
    private int leaderboardGUISize;
    private int leaderboardItemsPerPage;
    private GUIItem leaderboardGUIFiller;
    private GUIItem leaderboardNextPageItem;
    private GUIItem leaderboardPrevPageItem;
    private List<String> playerHeadLore;

    // Rewards Settings
    private Map<Long, Reward> rewards;

    public PlaytimeConfig(PlaytimePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadAfkSettings();
        loadMainGUISettings();
        loadLeaderboardGUISettings();
        loadRewards();
    }

    private void loadAfkSettings() {
        ConfigurationSection afkSection = config.getConfigurationSection("afk");
        if (afkSection == null) return;

        afkEnabled = afkSection.getBoolean("enabled", true);
        afkTimeout = afkSection.getLong("timeout", 300);

        ConfigurationSection afkMessagesSection = afkSection.getConfigurationSection("messages");
        if (afkMessagesSection == null) return;

        // AFK Message
        ConfigurationSection afkMsgSection = afkMessagesSection.getConfigurationSection("afk");
        if (afkMsgSection != null) {
            afkMessageEnabled = afkMsgSection.getBoolean("enabled", true);
            afkMessage = afkMsgSection.getString("message", "&eYou are now AFK.");
            ConfigurationSection afkTitleSection = afkMsgSection.getConfigurationSection("title");
            if (afkTitleSection != null) {
                afkTitleEnabled = afkTitleSection.getBoolean("enabled", true);
                afkTitle = new TitleData(
                        afkTitleSection.getString("main", "&7AFK Mode"),
                        afkTitleSection.getString("subtitle", "&fYou're no longer earning active playtime."),
                        afkTitleSection.getInt("fadein", 10),
                        afkTitleSection.getInt("stay", 40),
                        afkTitleSection.getInt("fadeout", 10)
                );
            }
        }

        // Back Message
        ConfigurationSection backMsgSection = afkMessagesSection.getConfigurationSection("back");
        if (backMsgSection != null) {
            backMessageEnabled = backMsgSection.getBoolean("enabled", true);
            backMessage = backMsgSection.getString("message", "&aWelcome back! You're no longer AFK.");
            ConfigurationSection backTitleSection = backMsgSection.getConfigurationSection("title");
            if (backTitleSection != null) {
                backTitleEnabled = backTitleSection.getBoolean("enabled", true);
                backTitle = new TitleData(
                        backTitleSection.getString("main", "&a&lBack to Active!"),
                        backTitleSection.getString("subtitle", "&fPlaytime tracking resumed."),
                        backTitleSection.getInt("fadein", 10),
                        backTitleSection.getInt("stay", 40),
                        backTitleSection.getInt("fadeout", 10)
                );
            }
        }
    }

    private void loadMainGUISettings() {
        ConfigurationSection guiSection = config.getConfigurationSection("playtime_gui");
        if (guiSection == null) return;

        mainGUITitle = guiSection.getString("title", "&bYour Playtime Menu");
        mainGUISize = guiSection.getInt("size", 27);
        mainGUIFiller = loadGUIItem(guiSection.getConfigurationSection("filler"));

        mainGUIItems = new HashMap<>();
        ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                mainGUIItems.put(key, loadGUIItem(itemsSection.getConfigurationSection(key)));
            }
        }
    }

    private void loadLeaderboardGUISettings() {
        ConfigurationSection guiSection = config.getConfigurationSection("playtimetop_gui");
        if (guiSection == null) return;

        leaderboardGUITitle = guiSection.getString("title", "&dPlaytime Leaderboard");
        leaderboardGUISize = guiSection.getInt("size", 54);
        leaderboardItemsPerPage = guiSection.getInt("items_per_page", 28);
        leaderboardGUIFiller = loadGUIItem(guiSection.getConfigurationSection("filler"));
        leaderboardNextPageItem = loadGUIItem(guiSection.getConfigurationSection("nextPageItem"));
        leaderboardPrevPageItem = loadGUIItem(guiSection.getConfigurationSection("prevPageItem"));
        playerHeadLore = guiSection.getStringList("player_head_lore").stream()
                .map(s -> s.replace("%player%", "PlayerName")) // Placeholder for example
                .collect(Collectors.toList());
    }

    private void loadRewards() {
        rewards = new HashMap<>();
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards"); // Corrected method call
        if (rewardsSection == null) return;

        for (String key : rewardsSection.getKeys(false)) {
            try {
                long playtime = Long.parseLong(key);
                ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
                if (rewardSection != null) {
                    GUIItem unlocked = loadGUIItem(rewardSection.getConfigurationSection("unlocked"));
                    GUIItem locked = loadGUIItem(rewardSection.getConfigurationSection("locked"));
                    TitleData title = null;
                    ConfigurationSection titleSection = rewardSection.getConfigurationSection("title");
                    if (titleSection != null) {
                        title = new TitleData(
                                titleSection.getString("main", ""),
                                titleSection.getString("subtitle", ""),
                                titleSection.getInt("fadein", 10),
                                titleSection.getInt("stay", 40),
                                titleSection.getInt("fadeout", 10)
                        );
                    }
                    List<String> commands = rewardSection.getStringList("commands");
                    rewards.put(playtime, new Reward(playtime, unlocked, locked, title, commands));
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid reward playtime key: " + key);
            }
        }
    }

    private GUIItem loadGUIItem(ConfigurationSection section) {
        if (section == null) return null;
        boolean enabled = section.getBoolean("enabled", false); // For filler
        if (section.contains("enabled") && !enabled) return null; // If filler is disabled

        Material material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        String name = section.getString("name", "Item");
        List<String> lore = section.getStringList("lore");
        boolean glow = section.getBoolean("glow", false);
        int slot = section.getInt("slot", -1); // Slot is handled separately for main GUI items

        return new GUIItem(material, name, lore, glow, slot);
    }

    public String color(String message) {
        return message.replace("&", "§");
    }

    public List<String> color(List<String> messages) {
        return messages.stream().map(this::color).collect(Collectors.toList());
    }
}
