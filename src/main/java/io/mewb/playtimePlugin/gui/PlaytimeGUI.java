package io.mewb.playtimePlugin.gui;


import dev.triumphteam.gui.guis.Gui; // This is for the simple GUI
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.mewb.playtimePlugin.PlaytimePlugin;
import io.mewb.playtimePlugin.data.GUIItem;
import io.mewb.playtimePlugin.data.PlaytimePlayer;
import io.mewb.playtimePlugin.data.Reward;
import io.mewb.playtimePlugin.util.ItemBuilder;
import io.mewb.playtimePlugin.util.SkullCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// Imports for Adventure API Components
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

// PlaytimeGUI no longer needs to implement Listener or have @EventHandler methods for clicks,
// as TriumphGUI handles this internally.
public class PlaytimeGUI {

    private final PlaytimePlugin plugin;
    // Pre-create the serializer for efficiency
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public PlaytimeGUI(PlaytimePlugin plugin) {
        this.plugin = plugin;
        // No need to register as a listener here anymore
    }

    public void openMainMenu(Player player) {
        String titleString = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getMainGUITitle());
        Component titleComponent = legacySerializer.deserialize(titleString); // Convert String to Component
        int size = plugin.getPlaytimeConfig().getMainGUISize();

        // Corrected: Use Gui.gui() to get the builder for simple GUIs, and .create() to build
        Gui gui = Gui.gui()
                .title(titleComponent) // Use Component for title
                .rows(size / 9) // Convert total slots to rows
                .disableAllInteractions() // Prevent players from taking items
                .create(); // Corrected: Use .create()

        // Fill with filler item
        GUIItem filler = plugin.getPlaytimeConfig().getMainGUIFiller();
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            gui.getFiller().fill(new GuiItem(fillerItem));
        }

        // Add main menu items
        PlaytimePlayer pp = plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId());
        for (Map.Entry<String, GUIItem> entry : plugin.getPlaytimeConfig().getMainGUIItems().entrySet()) {
            String key = entry.getKey();
            GUIItem guiConfigItem = entry.getValue(); // Renamed to avoid conflict with TriumphGUI's GuiItem
            if (guiConfigItem != null) {
                ItemStack itemStack;
                if (key.equals("your_playtime")) {
                    itemStack = new ItemBuilder(guiConfigItem.getMaterial())
                            .setName(plugin.getPlaytimeConfig().color(guiConfigItem.getName()))
                            .setLore(plugin.getPlaytimeManager().replacePlaceholders(guiConfigItem.getLore(), player, pp))
                            .setGlow(guiConfigItem.isGlow())
                            .build();
                } else if (key.equals("playtimetop")) {
                    itemStack = new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullOwner(player.getName()) // Set player's head for the item
                            .setName(plugin.getPlaytimeConfig().color(guiConfigItem.getName()))
                            .setLore(plugin.getPlaytimeConfig().color(guiConfigItem.getLore()))
                            .setGlow(guiConfigItem.isGlow())
                            .build();
                } else {
                    itemStack = new ItemBuilder(guiConfigItem.getMaterial())
                            .setName(plugin.getPlaytimeConfig().color(guiConfigItem.getName()))
                            .setLore(plugin.getPlaytimeConfig().color(guiConfigItem.getLore()))
                            .setGlow(guiConfigItem.isGlow())
                            .build();
                }

                GuiItem guiItem = new GuiItem(itemStack, event -> {
                    event.setCancelled(true); // Always cancel interaction
                    if (key.equals("playtimetop")) {
                        openLeaderboardGUI(player); // Open leaderboard from here
                    } else if (key.equals("playtime_rewards")) {
                        openRewardsGUI(player); // Open rewards from here
                    }
                });
                gui.setItem(guiConfigItem.getSlot(), guiItem);
            }
        }
        gui.open(player);
    }

    public void openLeaderboardGUI(Player player) {
        String titleString = plugin.getPlaytimeConfig().color(plugin.getPlaytimeConfig().getLeaderboardGUITitle());
        Component titleComponent = legacySerializer.deserialize(titleString); // Convert String to Component
        int size = plugin.getPlaytimeConfig().getLeaderboardGUISize();
        int itemsPerPage = plugin.getPlaytimeConfig().getLeaderboardItemsPerPage();

        GUIItem filler = plugin.getPlaytimeConfig().getLeaderboardGUIFiller();
        GuiItem borderFillerItem = null;
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            borderFillerItem = new GuiItem(fillerItem);
        }

        // --- Start of critical correction for PaginatedGui builder ---
        // 1. Start with the builder
        dev.triumphteam.gui.builder.gui.PaginatedBuilder builder = Gui.paginated()
                .title(titleComponent) // Called on builder
                .rows(size / 9) // Called on builder
                .pageSize(itemsPerPage) // Called on builder
                .disableAllInteractions(); // Called on builder

        // 2. Apply border filler if it exists (called on the builder)


        // 3. Create the GUI instance from the builder
        PaginatedGui gui = builder.create(); // Now 'gui' is correctly typed as PaginatedGui
        // --- End of critical correction ---

        List<PlaytimePlayer> topPlayers = plugin.getPlaytimeManager().getTopPlaytimes(Integer.MAX_VALUE);

        // Add player heads
        for (PlaytimePlayer pp : topPlayers) {
            CompletableFuture<ItemStack> headFuture = CompletableFuture.supplyAsync(() -> SkullCreator.itemFromUuid(pp.getUuid()));

            headFuture.thenAccept(playerHead -> {
                String playerName = playerHead.hasItemMeta() && playerHead.getItemMeta() instanceof SkullMeta && ((SkullMeta)playerHead.getItemMeta()).hasOwner()
                        ? ((SkullMeta)playerHead.getItemMeta()).getOwner() : "Unknown Player";

                // Fallback if Bukkit.getOfflinePlayer(pp.getUuid()).getName() is null for some reason
                if (playerName.equalsIgnoreCase("Unknown Player")) {
                    playerName = plugin.getServer().getOfflinePlayer(pp.getUuid()).getName();
                    if (playerName == null) playerName = "Unknown Player";
                }

                String finalPlayerName = playerName;
                List<String> lore = plugin.getPlaytimeConfig().getPlayerHeadLore().stream()
                        .map(line -> line.replace("%player%", finalPlayerName)
                                .replace("%active%", plugin.getPlaytimeManager().formatTime(pp.getActiveTime()))
                                .replace("%afk%", plugin.getPlaytimeManager().formatTime(pp.getAfkTime()))
                                .replace("%total%", plugin.getPlaytimeManager().formatTime(pp.getTotalTime())))
                        .collect(Collectors.toList());

                ItemStack finalHead = new ItemBuilder(playerHead)
                        .setName(plugin.getPlaytimeConfig().color("&e" + playerName)) // Display player name as item name
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .build();

                // Add to Gui, TriumphGUI handles placement in paginated section
                gui.addItem(new GuiItem(finalHead)); // Called on PaginatedGui instance
                gui.update(); // Called on PaginatedGui instance
            }).exceptionally(e -> {
                plugin.getLogger().warning("Failed to create skull for " + pp.getUuid() + ": " + e.getMessage());
                // Add a fallback item if skull creation fails
                gui.addItem(new GuiItem(new ItemBuilder(Material.BARRIER).setName(plugin.getPlaytimeConfig().color("&cError loading head")).build())); // Called on PaginatedGui instance
                gui.update(); // Called on PaginatedGui instance
                return null;
            });
        }

        // Add navigation buttons
        GUIItem prevConfigItem = plugin.getPlaytimeConfig().getLeaderboardPrevPageItem();
        if (prevConfigItem != null && prevConfigItem.getSlot() != -1) {
            ItemStack prevPage = new ItemBuilder(prevConfigItem.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(prevConfigItem.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(prevConfigItem.getLore()))
                    .setGlow(prevConfigItem.isGlow())
                    .build();
            gui.setItem(prevConfigItem.getSlot(), new GuiItem(prevPage, event -> {
                event.setCancelled(true);
                gui.previous(); // Called on PaginatedGui instance
            }));
        }

        GUIItem nextConfigItem = plugin.getPlaytimeConfig().getLeaderboardNextPageItem();
        if (nextConfigItem != null && nextConfigItem.getSlot() != -1) {
            ItemStack nextPage = new ItemBuilder(nextConfigItem.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(nextConfigItem.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(nextConfigItem.getLore()))
                    .setGlow(nextConfigItem.isGlow())
                    .build();
            gui.setItem(nextConfigItem.getSlot(), new GuiItem(nextPage, event -> {
                event.setCancelled(true);
                gui.next(); // Called on PaginatedGui instance
            }));
        }
        gui.open(player); // Called on PaginatedGui instance
    }


    public void openRewardsGUI(Player player) {
        String titleString = plugin.getPlaytimeConfig().color("&aPlaytime Rewards");
        Component titleComponent = legacySerializer.deserialize(titleString); // Convert String to Component
        int size = 54; // Fixed size for rewards GUI

        // Corrected: Use Gui.gui() to get the builder for simple GUIs, and .create() to build
        Gui gui = Gui.gui()
                .title(titleComponent) // Use Component for title
                .rows(size / 9)
                .disableAllInteractions()
                .create(); // Corrected: Use .create()

        // Fill with filler item (if configured)
        GUIItem filler = plugin.getPlaytimeConfig().getMainGUIFiller(); // Re-using main GUI filler
        if (filler != null) {
            ItemStack fillerItem = new ItemBuilder(filler.getMaterial())
                    .setName(plugin.getPlaytimeConfig().color(filler.getName()))
                    .setLore(plugin.getPlaytimeConfig().color(filler.getLore()))
                    .setGlow(filler.isGlow())
                    .build();
            gui.getFiller().fill(new GuiItem(fillerItem));
        }

        PlaytimePlayer pp = plugin.getPlaytimeManager().getPlaytimePlayer(player.getUniqueId());
        long totalPlaytime = pp.getTotalTime();

        // Sort rewards by required playtime
        List<Reward> sortedRewards = plugin.getPlaytimeConfig().getRewards().values().stream()
                .sorted(Comparator.comparingLong(Reward::getRequiredPlaytime))
                .collect(Collectors.toList());

        for (Reward reward : sortedRewards) {
            ItemStack itemStack;
            List<String> lore;

            boolean unlocked = totalPlaytime >= reward.getRequiredPlaytime();
            boolean claimed = pp.getClaimedRewards().contains(reward.getRequiredPlaytime());

            if (unlocked && claimed) {
                // Claimed state (use unlocked item, modify lore)
                lore = reward.getUnlockedItem().getLore().stream()
                        .map(line -> line + plugin.getPlaytimeConfig().color(" &8(Claimed)"))
                        .collect(Collectors.toList());
                itemStack = new ItemBuilder(reward.getUnlockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getUnlockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getUnlockedItem().isGlow())
                        .build();
            } else if (unlocked) {
                // Unlocked but not claimed
                lore = reward.getUnlockedItem().getLore();
                itemStack = new ItemBuilder(reward.getUnlockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getUnlockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getUnlockedItem().isGlow())
                        .build();
            } else {
                // Locked
                lore = reward.getLockedItem().getLore().stream()
                        .map(line -> line.replace("%playtime_required%", plugin.getPlaytimeManager().formatTime(reward.getRequiredPlaytime())))
                        .collect(Collectors.toList());
                itemStack = new ItemBuilder(reward.getLockedItem().getMaterial())
                        .setName(plugin.getPlaytimeConfig().color(reward.getLockedItem().getName()))
                        .setLore(plugin.getPlaytimeConfig().color(lore))
                        .setGlow(reward.getLockedItem().isGlow())
                        .build();
            }

            // Fix: Check for valid slot before setting item
            int rewardSlot = reward.getUnlockedItem().getSlot();
            if (rewardSlot >= 0 && rewardSlot < size) { // Ensure slot is valid
                // Create GuiItem with click action
                GuiItem guiItem = new GuiItem(itemStack, event -> {
                    event.setCancelled(true); // Always cancel interaction
                    if (unlocked && !claimed) {
                        pp.getClaimedRewards().add(reward.getRequiredPlaytime());
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        player.sendMessage(plugin.getPlaytimeConfig().color("&aYou have claimed the " + reward.getUnlockedItem().getName() + " &areward!"));

                        if (reward.getTitle() != null) {
                            player.sendTitle(
                                    plugin.getPlaytimeConfig().color(reward.getTitle().getMain()),
                                    plugin.getPlaytimeConfig().color(reward.getTitle().getSubtitle()),
                                    reward.getTitle().getFadeIn(), reward.getTitle().getStay(), reward.getTitle().getFadeOut()
                            );
                        }

                        for (String cmd : reward.getCommands()) {
                            String formattedCmd = cmd.replace("%player%", player.getName());
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), formattedCmd);
                        }
                        // Update the current GUI to reflect the claimed reward
                        gui.updateItem(rewardSlot, itemStack); // Use the validated slot
                    } else if (claimed) {
                        player.sendMessage(plugin.getPlaytimeConfig().color("&cYou have already claimed this reward."));
                    } else {
                        player.sendMessage(plugin.getPlaytimeConfig().color("&cYou have not yet unlocked this reward."));
                    }
                });
                gui.setItem(rewardSlot, guiItem); // Use the validated slot
            } else {
                plugin.getLogger().warning("Reward item '" + reward.getUnlockedItem().getName() + "' has an invalid slot configured: " + rewardSlot);
            }
        }
        gui.open(player);
    }
}