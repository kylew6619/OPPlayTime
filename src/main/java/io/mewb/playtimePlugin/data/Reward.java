// com/yourname/playtime/data/Reward.java
package io.mewb.playtimePlugin.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    private long requiredPlaytime; // in seconds
    private GUIItem unlockedItem;
    private GUIItem lockedItem;
    private TitleData title;
    private List<String> commands;
}