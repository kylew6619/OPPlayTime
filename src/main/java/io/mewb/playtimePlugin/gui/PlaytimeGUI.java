package io.mewb.playtimePlugin.gui;


import io.mewb.playtimePlugin.PlaytimePlugin;
import io.mewb.playtimePlugin.data.GUIItem;
import io.mewb.playtimePlugin.data.PlaytimePlayer;
import io.mewb.playtimePlugin.data.Reward;
import io.mewb.playtimePlugin.util.ItemBuilder;
import io.mewb.playtimePlugin.util.SkullCreator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlaytimeGUI implements Listener {

    private final PlaytimePlugin plugin;

    public PlaytimeGUI(PlaytimePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        String title = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUITitle());
        int size = plugin.getPlaytimeConfig().getMainGUISize();
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with filler item
        GUIItem filler = plugin.getPlaytimeConfig().getMainGUIFiller();
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        // Add main menu items
        PlaytimePlayer pp = plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId());
        for (Map.Entry<String, GUIItem> entry : plugin.getPlaytimeConfig().getMainGUIItems().entrySet()) {
            String key = entry.getKey();
            GUIItem guiItem = entry.getValue();
            if (guiItem != null) {
                ItemStack item;
                if (key.equals("your_playtime")) {
                    item = new ItemBuilder(guiItem.getMaterial())
                            .setName(plugin.getPlaytimeConfig().color(guiItem.getName()))
                            .setLore(plugin.getPlaytimeManager().replacePlaceholders(guiItem.getLore(), player, pp))
                            .setGlow(guiItem.isGlow())
                            .build();
                } else if (key.equals("playtimetop")) {
                    item = new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullOwner(player.getName()) // Set player's head for the item
                            .setName(plugin.getPlaytimeConfig().color(guiItem.getName()))
                            .setLore(plugin.getPlaytimeConfig().color(guiItem.getLore()))
                            .setGlow(guiItem.isGlow())
                            .build();
                } else {
                    item = new ItemBuilder(guiItem.getMaterial())
                            .setName(plugin.getPlaytimeConfig().color(guiItem.getName()))
                            .setLore(plugin.getPlaytimeConfig().color(guiItem.getLore()))
                            .setGlow(guiItem.isGlow())
                            .build();
                }
                inv.setItem(guiItem.getSlot(), item);
            }
        }
        player.openInventory(inv);
    }

    public void openLeaderboardGUI(Player player, int page) {
        String title = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getLeaderboardGUITitle());
        int size = plugin.getPlaytimeConfig().getLeaderboardGUISize();
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with filler item
        GUIItem filler = plugin.getPlaytimeConfig().getLeaderboardGUIFiller();
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        List<PlaytimePlayer> topPlayers = plugin.getPlaytimeManager().getTopPlaytimes(Integer.MAX_VALUE); // Get all for pagination
        int itemsPerPage = plugin.getPlaytimeConfig().getLeaderboardItemsPerPage();
        int totalPages = (int) Math.ceil((double) topPlayers.size() / itemsPerPage);

        // Calculate start and end index for current page
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, topPlayers.size());

        // Add player heads
        int currentSlot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PlaytimePlayer pp = topPlayers.get(i);
            Player targetPlayer = Bukkit.getPlayer(pp.getUuid());
            String playerName = (targetPlayer != null) ? targetPlayer.getName() : Bukkit.getOfflinePlayer(pp.getUuid()).getName();

            if (playerName == null) { // Fallback if offline player name is null (e.g., deleted player)
                playerName = "Unknown Player";
            }

            String finalPlayerName = playerName;
            List<String> lore = plugin.getPlaytimeConfig().getPlayerHeadLore().stream()
                    .map(line -> line.replace("%player%", finalPlayerName)
                            .replace("%active%", plugin.getPlaytimeManager().formatTime(pp.getActiveTime()))
                            .replace("%afk%", plugin.getPlaytimeManager().formatTime(pp.getAfkTime()))
                            .replace("%total%", plugin.getPlaytimeManager().formatTime(pp.getTotalTime())))
                    .collect(Collectors.toList());

            ItemStack playerHead = SkullCreator.itemFromUuid(pp.getUuid());
            ItemBuilder itemBuilder = new ItemBuilder(playerHead)
                    .setName(plugin.getPlaytimeConfig().color("&e" + playerName)) // Display player name as item name
                    .setLore(plugin.getPlaytimeConfig().color(lore));

            // Find an empty slot, skipping navigation button slots if they are at fixed positions
            while (inv.getItem(currentSlot) != null && currentSlot < size - 9) { // Avoid overwriting filler or nav buttons
                currentSlot++;
            }
            if (currentSlot < size - 9) { // Ensure we don't go into the bottom row for nav buttons
                inv.setItem(currentSlot, itemBuilder.build());
                currentSlot++;
            }
        }

        // Add navigation buttons (assuming bottom row)
        if (page > 1) {
            GUIItem prevItem = plugin.getPlaytimeConfig().getLeaderboardPrevPageItem();
            if (prevItem != null) {
                ItemStack prevPage = new ItemBuilder(prevItem.getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(prevItem.getName()))
                        .setLore(plugin.getPlaytimeConfig().color(prevItem.getLore()))
                        .setGlow(prevItem.isGlow())
                        .build();
                inv.setItem(size - 9, prevPage); // Bottom-left corner
            }
        }
        if (page < totalPages) {
            GUIItem nextItem = plugin.getPlaytimeConfig().getLeaderboardNextPageItem();
            if (nextItem != null) {
                ItemStack nextPage = new ItemBuilder(nextItem.getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(nextItem.getName()))
                        .setLore(plugin.getPlaytimeConfig().color(nextItem.getLore()))
                        .setGlow(nextItem.isGlow())
                        .build();
                inv.setItem(size - 1, nextPage); // Bottom-right corner
            }
        }
        player.openInventory(inv);
    }

    public void openRewardsGUI(Player player) {
        String title = plugin.getPlaytimeConfig().color("&aPlaytime Rewards");
        int size = 54; // Fixed size for rewards GUI
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with filler item (if configured)
        GUIItem filler = plugin.getPlaytimeConfig().getMainGUIFiller(); // Re-using main GUI filler
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        PlaytimePlayer pp = plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId());
        long totalPlaytime = pp.getTotalTime();

        // Sort rewards by required playtime
        List<Reward> sortedRewards = plugin.getPlaytimeConfig().getRewards().values().stream()
                .sorted(Comparator.comparingLong(Reward::getRequiredPlaytime))
                .collect(Collectors.toList());

        for (Reward reward : sortedRewards) {
            ItemStack item;
            List<String> lore;

            boolean unlocked = totalPlaytime >= reward.getRequiredPlaytime();
            boolean claimed = pp.getClaimedRewards().contains(reward.getRequiredPlaytime());

            if (unlocked && claimed) {
                // Claimed state (use unlocked item, modify lore)
                lore = reward.getUnlockedItem().getLore().stream()
                        .map(line -> line + plugin.getPlaytimeConfig().color(" &8(Claimed)"))
                        .collect(Collectors.toList());
                item = new ItemBuilder(reward.getUnlockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getUnlockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getUnlockedItem().isGlow())
                        .build();
            } else if (unlocked) {
                // Unlocked but not claimed
                lore = reward.getUnlockedItem().getLore();
                item = new ItemBuilder(reward.getUnlockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getUnlockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getUnlockedItem().isGlow())
                        .build();
            } else {
                // Locked
                lore = reward.getLockedItem().getLore().stream()
                        .map(line -> line.replace("%playtime_required%", plugin.getPlaytimeManager().formatTime(reward.getRequiredPlaytime())))
                        .collect(Collectors.toList());
                item = new ItemBuilder(reward.getLockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getLockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getLockedItem().isGlow())
                        .build();
            }
            inv.setItem(reward.getUnlockedItem().getSlot(), item); // Use unlocked item's slot
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Check if it's one of our GUIs
        String mainTitle = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUITitle());
        String leaderboardTitle = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getLeaderboardGUITitle());
        String rewardsTitle = plugin.getPlaytimeConfig().color("&aPlaytime Rewards"); // Hardcoded for consistency

        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(mainTitle)) {
            event.setCancelled(true);
            String itemName = clickedItem.hasItemMeta() ? clickedItem.getItemMeta().getDisplayName() : "";

            if (itemName.equals(plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUIItems().get("your_playtime").getName()))) {
                // This item shows your playtime, no action needed other than displaying it
                // The lore already contains the info, so just keep the GUI open.
            } else if (itemName.equals(plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUIItems().get("playtimetop").getName()))) {
                openLeaderboardGUI(player, 1);
            } else if (itemName.equals(plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUIItems().get("playtime_rewards").getName()))) {
                openRewardsGUI(player);
            }
        } else if (inventoryTitle.equals(leaderboardTitle)) {
            event.setCancelled(true);
            GUIItem nextItem = plugin.getPlaytimeConfig().getLeaderboardNextPageItem();
            GUIItem prevItem = plugin.getPlaytimeConfig().getLeaderboardPrevPageItem();

            if (nextItem != null && clickedItem.getType() == nextItem.getMaterial() &&
                    clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals(plugin.getPlaytimeConfig().color(nextItem.getName()))) {
                // Next page button
                int currentPage = getCurrentPageFromTitle(inventoryTitle);
                openLeaderboardGUI(player, currentPage + 1);
            } else if (prevItem != null && clickedItem.getType() == prevItem.getMaterial() &&
                    clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals(plugin.getPlaytimeConfig().color(prevItem.getName()))) {
                // Previous page button
                int currentPage = getCurrentPageFromTitle(inventoryTitle);
                openLeaderboardGUI(player, currentPage - 1);
            }
            // Player heads are just for display, no action on click
        } else if (inventoryTitle.equals(rewardsTitle)) {
            event.setCancelled(true);
            PlaytimePlayer pp = plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId());
            long totalPlaytime = pp.getTotalTime();

            for (Map.Entry<Long, Reward> entry : plugin.getPlaytimeConfig().getRewards().entrySet()) {
                Reward reward = entry.getValue();
                // Check if the clicked item matches an unlocked but unclaimed reward
                if (clickedItem.getType() == reward.getUnlockedItem().getMaterial() &&
                        clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName() &&
                        clickedItem.getItemMeta().getDisplayName().equals(plugin.getPlaytimeConfig().color(reward.getUnlockedItem().getName()))) {

                    boolean unlocked = totalPlaytime >= reward.getRequiredPlaytime();
                    boolean claimed = pp.getClaimedRewards().contains(reward.getRequiredPlaytime());

                    if (unlocked && !claimed) {
                        // Claim the reward
                        pp.getClaimedRewards().add(reward.getRequiredPlaytime());
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        player.sendMessage(plugin.getPlaytimeConfig().color("&aYou have claimed the " + reward.getUnlockedItem().getName() + " &areward!"));

                        // Send title
                        if (reward.getTitle() != null) {
                            player.sendTitle(
                                    plugin.getPlaytimeConfig().color(reward.getTitle().getMain()),
                                    plugin.getPlaytimeConfig().color(reward.getTitle().getSubtitle()),
                                    reward.getTitle().getFadeIn(), reward.getTitle().getStay(), reward.getTitle().getFadeOut()
                            );
                        }

                        // Run commands
                        for (String cmd : reward.getCommands()) {
                            String formattedCmd = cmd.replace("%player%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                        }
                        // Re-open GUI to update status
                        openRewardsGUI(player);
                        break;
                    } else if (claimed) {
                        player.sendMessage(plugin.getPlaytimeConfig().color("&cYou have already claimed this reward."));
                    } else {
                        player.sendMessage(plugin.getPlaytimeConfig().color("&cYou have not yet unlocked this reward."));
                    }
                }
            }
        }
    }

    private int getCurrentPageFromTitle(String title) {
        // This is a simple placeholder. If you add page numbers to the title,
        // you'd parse them here. For now, assume it's always page 1 unless navigated.
        return 1;
    }
}
