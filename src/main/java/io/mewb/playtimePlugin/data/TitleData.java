package io.mewb.playtimePlugin.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TitleData {
    private String main;
    private String subtitle;
    private int fadeIn;
    private int stay;
    private int fadeOut;
}