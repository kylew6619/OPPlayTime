package io.mewb.playtimePlugin.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Utility class to create player skulls
public class SkullCreator {

    private static Field profileField;
    private static LoadingCache<UUID, ItemStack> skullCache;

    static {
        try {
            profileField = Class.forName("org.bukkit.craftbukkit." + getVersion() + ".inventory.CraftMetaSkull").getDeclaredField("profile");
            profileField.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            Bukkit.getLogger().warning("Could not find CraftMetaSkull profile field. Skull creation might be limited.");
        }

        skullCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES) // Cache skulls for 10 minutes
                .build(new CacheLoader<UUID, ItemStack>() {
                    @Override
                    public ItemStack load(UUID uuid) throws Exception {
                        return createSkullFromUUIDInternal(uuid);
                    }
                });
    }

    private static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static ItemStack itemFromUuid(UUID uuid) {
        try {
            return skullCache.get(uuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to get skull from cache or create new for UUID " + uuid + ": " + e.getMessage());
            return new ItemStack(Material.PLAYER_HEAD); // Fallback
        }
    }

    private static ItemStack createSkullFromUUIDInternal(UUID uuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        if (profileField != null) {
            try {
                // For newer versions where setOwningPlayer is preferred
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            } catch (Exception e) {
                // Fallback for older versions or if setOwningPlayer fails
                GameProfile profile = new GameProfile(uuid, null);
                // This part would typically fetch the texture from Mojang API,
                // but for a simple plugin, we'll just set the UUID.
                // If you need actual textures for offline players, you'd need an async call here.
                try {
                    profileField.set(meta, profile);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Bukkit.getLogger().warning("Failed to set GameProfile for skull: " + ex.getMessage());
                }
            }
        }
        head.setItemMeta(meta);
        return head;
    }
}