package io.mewb.playtimePlugin.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

// Helper class to build ItemStacks more easily
public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = new ItemStack(itemStack);
    }

    public ItemBuilder setName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setGlow(boolean glow) {
        if (glow) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(meta);
            }
        } else {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.removeEnchant(Enchantment.DURABILITY);
                itemStack.setItemMeta(meta);
            }
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String ownerName) {
        if (itemStack.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
            meta.setOwner(ownerName);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }
}
