
package io.mewb.playtimePlugin.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
// Explicitly import Paper's PlayerProfile and ProfileProperty
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Utility class to create player skulls
public class SkullCreator {

    private static LoadingCache<UUID, ItemStack> skullCache;

    static {
        skullCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES) // Cache skulls for 30 minutes
                .build(new CacheLoader<UUID, ItemStack>() {
                    @Override
                    public ItemStack load(UUID uuid) throws Exception {
                        // This blocks until the profile is fetched and skull is created.
                        // For a cache loader, this is generally expected, but be mindful of performance.
                        return createSkullFromUUIDInternal(uuid).join();
                    }
                });
    }

    public static ItemStack itemFromUuid(UUID uuid) {
        try {
            return skullCache.get(uuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to get skull from cache or create new for UUID " + uuid + ": " + e.getMessage());
            return new ItemStack(Material.PLAYER_HEAD); // Fallback to a default player head
        }
    }

    private static CompletableFuture<ItemStack> createSkullFromUUIDInternal(UUID uuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return CompletableFuture.completedFuture(head);
        }

        // Bukkit.createProfile() will return Paper's PlayerProfile if running on Paper
        PlayerProfile profile = Bukkit.createProfile(uuid);

        // Asynchronously fetch the player profile, including textures.
        return profile.update().thenApply(updatedProfile -> {
            meta.setOwnerProfile(updatedProfile);
            head.setItemMeta(meta);
            return head;
        }).exceptionally(e -> {
            Bukkit.getLogger().warning("Failed to update player profile for UUID " + uuid + ": " + e.getMessage());
            // If profile update fails, return the head with just the UUID,
            // which might result in a default skin if the texture isn't found.
            return head;
        });
    }

    // Method to create a skull from a base64 texture string (for custom heads)
    public static ItemStack itemFromBase64(String base64Texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        // Bukkit.createProfile() will return Paper's PlayerProfile if running on Paper
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID()); // Random UUID for custom textures
        // Use Paper's ProfileProperty directly
        profile.getProperties().add(new ProfileProperty("textures", base64Texture));
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}