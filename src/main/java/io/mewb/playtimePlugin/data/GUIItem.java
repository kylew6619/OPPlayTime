package io.mewb.playtimePlugin.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Material;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GUIItem {
    private Material material;
    private String name;
    private List<String> lore;
    private boolean glow;
    private int slot; // Used for fixed slots in main GUI
}