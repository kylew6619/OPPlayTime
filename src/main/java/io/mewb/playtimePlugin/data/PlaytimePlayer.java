package io.mewb.playtimePlugin.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaytimePlayer {
    private UUID uuid;
    private long activeTime; // in seconds
    private long afkTime;    // in seconds
    private long lastActivityTime; // Unix timestamp in milliseconds
    private boolean isAFK;
    private Set<Long> claimedRewards; // Set of reward milestones (in seconds) already claimed

    public PlaytimePlayer(UUID uuid) {
        this.uuid = uuid;
        this.activeTime = 0;
        this.afkTime = 0;
        this.lastActivityTime = System.currentTimeMillis();
        this.isAFK = false;
        this.claimedRewards = new HashSet<>();
    }

    public void addActiveTime(long seconds) {
        this.activeTime += seconds;
    }

    public void addAfkTime(long seconds) {
        this.afkTime += seconds;
    }

    public long getTotalTime() {
        return activeTime + afkTime;
    }

    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }
}
